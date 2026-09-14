package com.idphoto.printing.print

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

data class DiscoveredPrinter(
    val serviceName: String,
    val host: String,
    val port: Int,
    val resourcePath: String,
) {
    val probeAddress: String
        get() {
            val urlHost = if (':' in host && !host.startsWith('[')) "[$host]" else host
            return "$urlHost:$port$resourcePath"
        }
}

object EpsonPrinterDiscovery {
    private const val SERVICE_TYPE = "_ipp._tcp."
    private const val SEARCH_TIMEOUT_MS = 8_000L

    @Suppress("DEPRECATION")
    suspend fun find(context: Context): DiscoveredPrinter = withTimeout(SEARCH_TIMEOUT_MS + 2_000L) {
        suspendCancellableCoroutine { continuation ->
            val appContext = context.applicationContext
            val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val multicastLock = wifiManager?.createMulticastLock("id-photo-printer-discovery")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            val executor = ContextCompat.getMainExecutor(appContext)
            val handler = android.os.Handler(appContext.mainLooper)
            val candidates = mutableMapOf<String, NsdServiceInfo>()
            var discoveryActive = false
            var resolving = false

            lateinit var listener: NsdManager.DiscoveryListener
            var pendingTimeout: Runnable? = null

            fun releaseResources() {
                pendingTimeout?.let(handler::removeCallbacks)
                if (discoveryActive) {
                    runCatching { nsdManager.stopServiceDiscovery(listener) }
                    discoveryActive = false
                }
                if (multicastLock?.isHeld == true) multicastLock.release()
            }

            fun fail(message: String, cause: Throwable? = null) {
                if (!continuation.isActive) return
                releaseResources()
                continuation.resumeWithException(IOException(message, cause))
            }

            fun resolve(service: NsdServiceInfo) {
                if (resolving || !continuation.isActive) return
                resolving = true
                if (discoveryActive) {
                    runCatching { nsdManager.stopServiceDiscovery(listener) }
                    discoveryActive = false
                }
                try {
                    nsdManager.resolveService(
                        service,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                executor.execute {
                                    fail("The printer was found, but its Wi-Fi address could not be read (code $errorCode).")
                                }
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                executor.execute resolved@{
                                    if (!continuation.isActive) return@resolved
                                    val host = serviceInfo.host?.hostAddress
                                    if (host.isNullOrBlank()) {
                                        fail("The printer was found, but it did not provide a Wi-Fi address.")
                                        return@resolved
                                    }
                                    val path = serviceInfo.attributes["rp"]
                                        ?.toString(Charsets.UTF_8)
                                        ?.trim()
                                        ?.trimStart('/')
                                        ?.takeIf(String::isNotBlank)
                                        ?.let { "/$it" }
                                        ?: DirectPrinterProfile.DEFAULT_IPP_PATH
                                    releaseResources()
                                    continuation.resume(
                                        DiscoveredPrinter(
                                            serviceName = serviceInfo.serviceName,
                                            host = host,
                                            port = serviceInfo.port.takeIf { it > 0 }
                                                ?: DirectPrinterProfile.DEFAULT_IPP_PORT,
                                            resourcePath = path,
                                        ),
                                    )
                                }
                            }
                        },
                    )
                } catch (error: Exception) {
                    fail("The printer address could not be resolved.", error)
                }
            }

            val timeout = Runnable {
                when (candidates.size) {
                    0 -> fail("Printer unavailable. Check that the L15150 is on and your phone is on the same Wi-Fi.")
                    1 -> resolve(candidates.values.single())
                    else -> fail("Several L15150 printers were found. Choose yours by address in Printer Setup.")
                }
            }
            pendingTimeout = timeout

            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    executor.execute {
                        discoveryActive = true
                        if (!continuation.isActive) releaseResources()
                    }
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (!serviceInfo.serviceType.contains("_ipp._tcp", ignoreCase = true)) return
                    executor.execute {
                        if (continuation.isActive && isL15150Printer("", serviceInfo.serviceName)) {
                            candidates[serviceInfo.serviceName] = serviceInfo
                        }
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    executor.execute { candidates.remove(serviceInfo.serviceName) }
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    executor.execute { discoveryActive = false }
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    executor.execute { fail("Printer search could not start (code $errorCode).") }
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    executor.execute { discoveryActive = false }
                }
            }

            continuation.invokeOnCancellation {
                executor.execute { releaseResources() }
            }
            executor.execute {
                if (!continuation.isActive) return@execute
                try {
                    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
                    handler.postDelayed(timeout, SEARCH_TIMEOUT_MS)
                } catch (error: Exception) {
                    fail("Printer search could not start.", error)
                }
            }
        }
    }
}
