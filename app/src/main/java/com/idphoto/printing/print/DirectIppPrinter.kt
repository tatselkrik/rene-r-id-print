package com.idphoto.printing.print

import android.annotation.SuppressLint
import android.content.Context
import com.hp.jipp.encoding.Attribute
import com.hp.jipp.encoding.IppInputStream
import com.hp.jipp.encoding.IppOutputStream
import com.hp.jipp.encoding.IppPacket
import com.hp.jipp.encoding.KeywordOrName
import com.hp.jipp.encoding.Tag
import com.hp.jipp.model.MediaCol
import com.hp.jipp.model.Orientation
import com.hp.jipp.model.PrintQuality
import com.hp.jipp.model.Types
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class DirectPrinterProfile(
    val host: String,
    val port: Int = DEFAULT_IPP_PORT,
    val resourcePath: String = DEFAULT_IPP_PATH,
    val displayName: String = "Epson printer",
    val mediaKeyword: String = DEFAULT_5X7_MEDIA,
    val mediaSource: String = DEFAULT_CASSETTE_1_SOURCE,
    val mediaType: String = DEFAULT_MATTE_MEDIA_TYPE,
    val useTls: Boolean = true,
    val certificateSha256: String? = null,
    val supportsJpeg: Boolean = true,
    val reportedFiveBySeven: Boolean = true,
) {
    val addressLabel: String
        get() = if (port == DEFAULT_IPP_PORT) host else "$host:$port"
    val readyForDirectPrint: Boolean
        get() = useTls && !certificateSha256.isNullOrBlank() &&
            supportsJpeg &&
            reportedFiveBySeven

    companion object {
        const val DEFAULT_IPP_PORT = 631
        const val DEFAULT_IPP_PATH = "/ipp/print"
        const val DEFAULT_5X7_MEDIA = "na_5x7_5x7in"
        const val DEFAULT_CASSETTE_1_SOURCE = "main"
        const val DEFAULT_MATTE_MEDIA_TYPE = "photographic-matte"
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}

object DirectPrinterSettings {
    private const val PREFERENCES = "direct_printer"
    private const val KEY_MEDIA_PROFILE_VERSION = "media_profile_version"
    private const val CURRENT_MEDIA_PROFILE_VERSION = 1
    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"
    private const val KEY_PATH = "path"
    private const val KEY_NAME = "name"
    private const val KEY_MEDIA = "media"
    private const val KEY_MEDIA_SOURCE = "media_source"
    private const val KEY_MEDIA_TYPE = "media_type"
    private const val KEY_USE_TLS = "use_tls"
    private const val KEY_CERTIFICATE_SHA256 = "certificate_sha256"
    private const val KEY_JPEG = "supports_jpeg"
    private const val KEY_5X7 = "reports_5x7"

    fun load(context: Context): DirectPrinterProfile? {
        val values = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val host = values.getString(KEY_HOST, null)?.takeIf(String::isNotBlank) ?: return null
        val hasCurrentMediaProfile = values.getInt(KEY_MEDIA_PROFILE_VERSION, 0) ==
            CURRENT_MEDIA_PROFILE_VERSION
        return DirectPrinterProfile(
            host = host,
            port = values.getInt(KEY_PORT, DirectPrinterProfile.DEFAULT_IPP_PORT),
            resourcePath = values.getString(KEY_PATH, DirectPrinterProfile.DEFAULT_IPP_PATH)
                ?: DirectPrinterProfile.DEFAULT_IPP_PATH,
            displayName = values.getString(KEY_NAME, "Epson printer") ?: "Epson printer",
            mediaKeyword = values.getString(KEY_MEDIA, DirectPrinterProfile.DEFAULT_5X7_MEDIA)
                ?: DirectPrinterProfile.DEFAULT_5X7_MEDIA,
            mediaSource = if (hasCurrentMediaProfile) {
                values.getString(KEY_MEDIA_SOURCE, DirectPrinterProfile.DEFAULT_CASSETTE_1_SOURCE)
                    ?: DirectPrinterProfile.DEFAULT_CASSETTE_1_SOURCE
            } else {
                DirectPrinterProfile.DEFAULT_CASSETTE_1_SOURCE
            },
            mediaType = if (hasCurrentMediaProfile) {
                values.getString(KEY_MEDIA_TYPE, DirectPrinterProfile.DEFAULT_MATTE_MEDIA_TYPE)
                    ?: DirectPrinterProfile.DEFAULT_MATTE_MEDIA_TYPE
            } else {
                DirectPrinterProfile.DEFAULT_MATTE_MEDIA_TYPE
            },
            useTls = values.getBoolean(KEY_USE_TLS, true),
            certificateSha256 = values.getString(KEY_CERTIFICATE_SHA256, null),
            // Profiles saved by older builds do not have this key. JPEG is the
            // L15150's advertised direct document format, so migrate them on load.
            supportsJpeg = values.getBoolean(KEY_JPEG, true),
            reportedFiveBySeven = values.getBoolean(KEY_5X7, true),
        )
    }

    fun save(context: Context, profile: DirectPrinterProfile) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOST, profile.host)
            .putInt(KEY_PORT, profile.port)
            .putString(KEY_PATH, profile.resourcePath)
            .putString(KEY_NAME, profile.displayName)
            .putString(KEY_MEDIA, profile.mediaKeyword)
            .putString(KEY_MEDIA_SOURCE, profile.mediaSource)
            .putString(KEY_MEDIA_TYPE, profile.mediaType)
            .putBoolean(KEY_USE_TLS, profile.useTls)
            .putString(KEY_CERTIFICATE_SHA256, profile.certificateSha256)
            .putInt(KEY_MEDIA_PROFILE_VERSION, CURRENT_MEDIA_PROFILE_VERSION)
            .putBoolean(KEY_JPEG, profile.supportsJpeg)
            .putBoolean(KEY_5X7, profile.reportedFiveBySeven)
            .apply()
    }
}

data class DirectPrintResult(
    val message: String,
    val jobId: Int? = null,
)

object DirectIppPrinter {
    private val standardPaths = listOf(
        DirectPrinterProfile.DEFAULT_IPP_PATH,
        "/ipp/printer",
        "/ipp/port1",
    )

    fun probe(
        address: String,
        expectedCertificate: String? = null,
        tryStandardPaths: Boolean = true,
    ): DirectPrinterProfile {
        val parsed = PrinterAddress.parse(address)
        val paths = if (tryStandardPaths) {
            listOfNotNull(parsed.path).plus(standardPaths).distinct()
        } else {
            listOf(parsed.path ?: DirectPrinterProfile.DEFAULT_IPP_PATH)
        }
        var lastError: Exception? = null
        val candidates = paths.map { path ->
            PrinterEndpoint(parsed.host, parsed.port, path, useTls = true)
        }
        candidates.forEach { candidate ->
            try {
                // Background reconnection must never replace a saved certificate.
                val endpoint = candidate.copy(
                    certificateSha256 = expectedCertificate ?: fetchCertificateFingerprint(candidate),
                )
                val request = IppPacket.getPrinterAttributes(
                    endpoint.ippUri,
                    Types.printerName,
                    Types.printerMakeAndModel,
                    Types.documentFormatSupported,
                    Types.mediaSupported,
                    Types.mediaSourceSupported,
                    Types.mediaTypeSupported,
                    Types.printScalingSupported,
                ).build()
                val response = post(endpoint, request, null, 12_000)
                ensureSuccessful(response)

                val name = response.getString(Tag.printerAttributes, Types.printerName)
                    ?: response.getString(Tag.printerAttributes, Types.printerMakeAndModel)
                    ?: "Epson printer"
                val model = response.getString(Tag.printerAttributes, Types.printerMakeAndModel).orEmpty()
                if (!isL15150Printer(model, name)) {
                    throw IOException("This app requires an Epson L15150 printer.")
                }
                val formats = response.getStrings(Tag.printerAttributes, Types.documentFormatSupported)
                val media = response.getStrings(Tag.printerAttributes, Types.mediaSupported)
                val mediaSources = response.getStrings(Tag.printerAttributes, Types.mediaSourceSupported)
                val mediaTypes = response.getStrings(Tag.printerAttributes, Types.mediaTypeSupported)
                val fiveBySeven = selectFiveBySeven(media)

                return DirectPrinterProfile(
                    host = endpoint.host,
                    port = endpoint.port,
                    resourcePath = endpoint.path,
                    displayName = name,
                    mediaKeyword = fiveBySeven ?: DirectPrinterProfile.DEFAULT_5X7_MEDIA,
                    mediaSource = selectCassetteOne(mediaSources),
                    mediaType = selectMattePhotoMediaType(mediaTypes),
                    useTls = endpoint.useTls,
                    certificateSha256 = endpoint.certificateSha256,
                    supportsJpeg = formats.any {
                        it.equals(DirectPrinterProfile.JPEG_MIME_TYPE, ignoreCase = true)
                    },
                    reportedFiveBySeven = fiveBySeven != null,
                )
            } catch (error: Exception) {
                lastError = error
            }
        }
        val detail = lastError?.message?.takeIf(String::isNotBlank)
        throw IOException(
            buildString {
                append("The printer's encrypted print service could not be opened.")
                if (detail != null) append(" $detail")
            },
            lastError,
        )
    }

    fun printJpeg(
        profile: DirectPrinterProfile,
        jpeg: File,
        jobName: String,
    ): DirectPrintResult {
        require(jpeg.isFile && jpeg.length() > 0L) { "The print JPEG is missing or empty." }
        if (!profile.supportsJpeg) {
            throw IOException(
                "This printer did not report direct JPEG support.",
            )
        }
        if (!profile.reportedFiveBySeven) {
            throw IOException(
                "This printer did not report 5 x 7 paper through direct printing.",
            )
        }

        if (!profile.readyForDirectPrint) {
            throw IOException("Encrypted printing is required. Open Printer Setup and reconnect securely.")
        }
        val endpoint = PrinterEndpoint(
            host = profile.host,
            port = profile.port,
            path = profile.resourcePath,
            useTls = profile.useTls,
            certificateSha256 = profile.certificateSha256,
        )
        val mediaAttribute = Types.mediaCol.of(
            MediaCol().apply {
                mediaSizeName = KeywordOrName(profile.mediaKeyword)
                mediaSource = KeywordOrName(profile.mediaSource)
                mediaType = KeywordOrName(profile.mediaType)
            },
        )
        val jobAttributes = mutableListOf<Attribute<*>>(
            mediaAttribute,
            Types.orientationRequested.of(Orientation.portrait),
            Types.printQuality.of(PrintQuality.high),
            Types.printColorMode.of("color"),
            Types.sides.of("one-sided"),
            Types.printScaling.of("fill"),
            Types.copies.of(1),
        )
        val request = IppPacket.printJob(endpoint.ippUri)
            .putOperationAttributes(
                Types.documentFormat.of(DirectPrinterProfile.JPEG_MIME_TYPE),
                Types.requestingUserName.of("ID Photo Print"),
                Types.jobName.of(jobName),
                Types.ippAttributeFidelity.of(false),
            )
            .putJobAttributes(jobAttributes)
            .build()
        val response = post(endpoint, request, jpeg, 120_000)
        ensureSuccessful(response)
        val jobId = response.getValue(Tag.jobAttributes, Types.jobId)
        return DirectPrintResult(
            message = if (jobId == null) {
                "The print job was sent to ${profile.displayName}."
            } else {
                "Print job $jobId was sent to ${profile.displayName}."
            },
            jobId = jobId,
        )
    }

    private fun post(
        endpoint: PrinterEndpoint,
        packet: IppPacket,
        document: File?,
        timeoutMs: Int,
    ): IppPacket {
        require(endpoint.useTls) { "Unencrypted printing is disabled." }
        val packetBytes = java.io.ByteArrayOutputStream().use { output ->
            IppOutputStream(output).write(packet)
            output.toByteArray()
        }
        val contentLength = packetBytes.size.toLong() + (document?.length() ?: 0L)
        val connection = endpoint.httpUrl.openConnection() as HttpURLConnection
        try {
            if (connection is HttpsURLConnection) configurePinnedTls(connection, endpoint)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 8_000
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/ipp")
            connection.setRequestProperty("Accept", "application/ipp")
            connection.setRequestProperty("User-Agent", "ID Photo Print/1.0")
            connection.setFixedLengthStreamingMode(contentLength)
            connection.outputStream.use { output ->
                output.write(packetBytes)
                document?.inputStream()?.use { input -> input.copyTo(output) }
            }
            val httpStatus = connection.responseCode
            val responseStream = if (httpStatus in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            if (httpStatus !in 200..299 || responseStream == null) {
                val message = if (httpStatus == 426) {
                    "The printer requires encrypted IPPS. Reconnect it from Printer Setup."
                } else {
                    "The printer returned network error $httpStatus at ${endpoint.httpUrl}."
                }
                throw IOException(message)
            }
            return responseStream.use { IppInputStream(it).readPacket() }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchCertificateFingerprint(endpoint: PrinterEndpoint): String {
        val connection = endpoint.httpUrl.openConnection() as? HttpsURLConnection
            ?: throw IOException("The secure printer address is invalid.")
        try {
            connection.sslSocketFactory = sslSocketFactory(trustAllManager)
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.requestMethod = "GET"
            connection.connect()
            val certificate = connection.serverCertificates.firstOrNull() as? X509Certificate
                ?: throw IOException("The printer did not provide a security certificate.")
            return sha256(certificate.encoded)
        } finally {
            connection.disconnect()
        }
    }

    @SuppressLint("CustomX509TrustManager")
    private fun configurePinnedTls(
        connection: HttpsURLConnection,
        endpoint: PrinterEndpoint,
    ) {
        val expected = endpoint.certificateSha256
            ?: throw IOException("Reconnect the printer once to save its security certificate.")
        val pinnedManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val certificate = chain?.firstOrNull()
                    ?: throw CertificateException("The printer did not provide a security certificate.")
                val actual = sha256(certificate.encoded)
                if (!actual.equals(expected, ignoreCase = true)) {
                    throw CertificateException(
                        "The printer security certificate changed. Reconnect it from Printer Setup.",
                    )
                }
            }
        }
        connection.sslSocketFactory = sslSocketFactory(pinnedManager)
        // The certificate fingerprint authenticates this exact printer even when its
        // self-signed certificate does not contain the numeric IP address as a host name.
        connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
    }

    private fun sslSocketFactory(manager: X509TrustManager) =
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(manager), null)
        }.socketFactory

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    // Used only to read the Epson printer's self-signed certificate on the first
    // local connection. Every IPP request after that validates the saved fingerprint.
    @SuppressLint("CustomX509TrustManager")
    private val trustAllManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    }

    private fun ensureSuccessful(response: IppPacket) {
        if (response.status.code >= 0x0100) {
            val detail = response.getString(Tag.operationAttributes, Types.statusMessage)
                ?.takeIf(String::isNotBlank)
            throw IOException(detail ?: "The printer rejected the job: ${response.status.name}.")
        }
    }

    private fun selectFiveBySeven(media: List<String>): String? = media.firstOrNull {
        val value = it.lowercase()
        "5x7" in value && "borderless" !in value
    } ?: media.firstOrNull { "5x7" in it.lowercase() }

    private fun selectCassetteOne(mediaSources: List<String>): String =
        mediaSources.firstOrNull {
            val value = it.lowercase()
            value == "tray-1" || value == "cassette-1" || value == "source-1"
        } ?: mediaSources.firstOrNull {
            val value = it.lowercase()
            ('1' in value) && ("tray" in value || "cassette" in value)
        } ?: mediaSources.firstOrNull { it.equals("main", ignoreCase = true) }
        ?: DirectPrinterProfile.DEFAULT_CASSETTE_1_SOURCE

    private fun selectMattePhotoMediaType(mediaTypes: List<String>): String =
        mediaTypes.firstOrNull {
            val value = it.lowercase()
            "matte" in value && ("photo" in value || "photographic" in value)
        } ?: mediaTypes.firstOrNull { "matte" in it.lowercase() }
        ?: DirectPrinterProfile.DEFAULT_MATTE_MEDIA_TYPE
}

internal fun isL15150Printer(model: String, name: String): Boolean =
    Regex("(?i)\\bL15150\\b").containsMatchIn("$model $name")

private data class PrinterEndpoint(
    val host: String,
    val port: Int,
    val path: String,
    val useTls: Boolean,
    val certificateSha256: String? = null,
) {
    val ippUri: URI = URI(if (useTls) "ipps" else "ipp", null, host, port, path, null, null)
    val httpUrl: URL = URL(if (useTls) "https" else "http", host, port, path)
}

private data class PrinterAddress(
    val host: String,
    val port: Int,
    val path: String?,
) {
    companion object {
        fun parse(input: String): PrinterAddress {
            val trimmed = input.trim()
            require(trimmed.isNotBlank()) { "Enter the printer IP address first." }
            val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
            val httpAddress = withScheme.replaceFirst("ipp://", "http://", ignoreCase = true)
            val uri = URI(httpAddress)
            val host = uri.host?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("Enter an address such as 192.168.1.25.")
            return PrinterAddress(
                host = host,
                port = uri.port.takeIf { it > 0 } ?: DirectPrinterProfile.DEFAULT_IPP_PORT,
                path = uri.path?.takeIf { it.isNotBlank() && it != "/" },
            )
        }
    }
}
