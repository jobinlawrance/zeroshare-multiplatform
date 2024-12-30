package live.jkbx.zeroshare.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import io.github.vinceglb.filekit.core.PlatformFile
import live.jkbx.zeroshare.socket.FileTransferMetadata
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig
import org.koin.compose.getKoin
import org.koin.core.context.GlobalContext
import java.io.File
import java.io.FileOutputStream

actual fun PlatformFile.toFileMetaData(): FileTransferMetadata {
    val file = this.uri.toCustomFile()!!
    val tika = Tika()
    val mimeType = tika.detect(file)
    val mimeTypes = TikaConfig.getDefaultConfig().mimeRepository
    val mimeTypeObj = mimeTypes.forName(mimeType)

    val fileMetadata = FileTransferMetadata(
        fileName = file.name,
        fileSize = file.length(),
        mimeType = mimeType,
        extension = mimeTypeObj.extensions.firstOrNull(),

    )
    return fileMetadata
}

fun Uri.toCustomFile(): File? {
    val context = getKoinContext() as Context
    if (scheme == "file") {
        return File(path!!)
    } else if (scheme == "content") {
        val contentResolver = context.contentResolver
        val fileName = contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: return null

        val tempFile = File(context.cacheDir, fileName)
        contentResolver.openInputStream(this)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }
    return null
}

actual fun getKoinContext(): Any {
    return GlobalContext.get().get<Context>()
}