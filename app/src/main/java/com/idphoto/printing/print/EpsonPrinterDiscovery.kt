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
            var fallback: NsdServiceInfo? = null
            var discoveryActive = false
            var resolving = false

            lateinit var listener: NsdManager.DiscoveryListener

            fun releaseResources() {
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
                nsdManager.resolveService(
                    service,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            fail("The printer was found, but its Wi-Fi address could not be read (code $errorCode).")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress
                            if (host.isNullOrBlank()) {
                                fail("The printer was found, but it did not provide a Wi-Fi address.")
                                return
                            }
                            val path = serviceInfo.attributes["rp"]
                                ?.toString(Charsets.UTF_8)
                                ?.trim()
                                ?.trimStart('/')
                                ?.takeIf(String::isNotBlank)
                                ?.let { "/$it" }
                                ?: DirectPrinterProfile.DEFAULT_IPP_PATH
                            releaseResources()
                            if (continuation.isActive) {
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
            }

            val timeout = Runnable {
                val candidate = fallback
                if (candidate == null) {
                    fail("No IPP printer was found. Check Wi-Fi, or enter the printer IP address manually.")
                } else {
                    resolve(candidate)
                }
            }

            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    discoveryActive = true
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (!serviceInfo.serviceType.contains("_ipp._tcp", ignoreCase = true)) return
                    if (fallback == null) fallback = serviceInfo
                    val name = serviceInfo.serviceName.lowercase()
                    if ("epson" in name || "l15150" in name) {
                        resolve(serviceInfo)
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                override fun onDiscoveryStopped(serviceType: String) {
                    discoveryActive = false
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    fail("Printer search could not start (code $errorCode).")
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    discoveryActive = false
                }
            }

            continuation.invokeOnCancellation {
                executor.execute { releaseResources() }
            }
            executor.execute {
                try {
                    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
                    executor.execute {
                        android.os.Handler(appContext.mainLooper).postDelayed(timeout, SEARCH_TIMEOUT_MS)
                    }
                } catch (error: Exception) {
                    fail("Printer search could not start.", error)
                }
            }
        }
    }
}
