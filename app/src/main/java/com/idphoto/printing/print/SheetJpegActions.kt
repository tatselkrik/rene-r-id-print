package com.idphoto.printing.print

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object SheetJpegActions {
    fun save(context: Context, jpeg: File, destination: Uri) {
        val output = context.contentResolver.openOutputStream(destination, "w")
            ?: throw IOException("The selected save location could not be opened.")
        output.use { stream -> jpeg.inputStream().use { it.copyTo(stream) } }
    }

    fun share(context: Context, jpeg: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            jpeg,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = DirectPrinterProfile.JPEG_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(jpeg.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share 5 x 7 ID photo sheet"))
    }
}
