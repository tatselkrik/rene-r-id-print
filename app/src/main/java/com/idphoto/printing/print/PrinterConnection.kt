package com.idphoto.printing.print

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PrinterConnectionState(
    val profile: DirectPrinterProfile? = null,
    val checking: Boolean = false,
    val message: String = "Checking printer…",
)

/** Shared by capture, setup and preview so they never hold separate saved profiles. */
class PrinterConnection(
    private val load: () -> DirectPrinterProfile?,
    private val save: (DirectPrinterProfile) -> Unit,
    private val discover: suspend () -> String,
    private val probe: suspend (String, String?) -> DirectPrinterProfile,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(PrinterConnectionState())
    val state = mutableState.asStateFlow()

    suspend fun refresh() = mutex.withLock {
        mutableState.value = PrinterConnectionState(checking = true)
        try {
            val saved = load()
            // A legacy plain-IPP profile must be explicitly paired securely once.
            if (saved != null && (!saved.useTls || saved.certificateSha256.isNullOrBlank())) {
                throw IOException("Open Printer Setup once to connect securely.")
            }
            val connected = if (saved == null) {
                probe(discover(), null)
            } else {
                try {
                    probe(saved.probeAddress(), saved.certificateSha256)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Address changes are fine; identity changes require manual setup.
                    probe(discover(), saved.certificateSha256)
                }
            }
            if (saved != null && !connected.certificateSha256.equals(saved.certificateSha256, true)) {
                throw IOException("The printer certificate changed. Reconnect explicitly in Printer Setup.")
            }
            accept(connected)
        } catch (cancelled: CancellationException) {
            mutableState.value = PrinterConnectionState(message = "Printer check paused")
            throw cancelled
        } catch (error: Exception) {
            mutableState.value = PrinterConnectionState(
                message = error.message ?: "Printer unavailable. Check Wi-Fi and power.",
            )
        }
    }

    /** Explicit setup may establish a new trust-on-first-use certificate. */
    suspend fun connect(address: String): DirectPrinterProfile = mutex.withLock {
        mutableState.value = PrinterConnectionState(checking = true)
        try {
            probe(address, null).also(::accept)
        } catch (cancelled: CancellationException) {
            mutableState.value = PrinterConnectionState(message = "Printer check paused")
            throw cancelled
        } catch (error: Exception) {
            mutableState.value = PrinterConnectionState(
                message = error.message ?: "Could not connect securely.",
            )
            throw error
        }
    }

    private fun accept(profile: DirectPrinterProfile) {
        if (!profile.readyForDirectPrint) {
            throw IOException("The printer must support encrypted JPEG printing on 5 × 7 paper.")
        }
        save(profile)
        mutableState.value = PrinterConnectionState(profile = profile, message = "Printer ready")
    }

    companion object {
        fun create(context: Context): PrinterConnection {
            val app = context.applicationContext
            return PrinterConnection(
                load = { DirectPrinterSettings.load(app) },
                save = { DirectPrinterSettings.save(app, it) },
                discover = { EpsonPrinterDiscovery.find(app).probeAddress },
                probe = { address, certificate ->
                    withContext(Dispatchers.IO) {
                        DirectIppPrinter.probe(address, certificate, tryStandardPaths = false)
                    }
                },
            )
        }
    }
}

internal fun DirectPrinterProfile.probeAddress(): String {
    val urlHost = if (':' in host && !host.startsWith('[')) "[$host]" else host
    return "$urlHost:$port$resourcePath"
}

/** Observe local Wi-Fi while visible; internet validation is deliberately unnecessary. */
suspend fun PrinterConnection.watchWifi(context: Context) {
    val connectivity = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(Unit) }
            override fun onLost(network: Network) { trySend(Unit) }
            override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) { trySend(Unit) }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivity.registerNetworkCallback(request, callback)
        trySend(Unit)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.conflate().collectLatest {
        // Coalesce initial availability and address callbacks into one check.
        delay(500)
        refresh()
    }
}
