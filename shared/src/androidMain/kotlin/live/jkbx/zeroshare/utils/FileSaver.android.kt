package live.jkbx.zeroshare.utils

import java.io.File

actual class FileSaver actual constructor(fileName: String) {
    private val downloadsDir =
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    private val file = File(downloadsDir, fileName)

    actual fun append(bytes: ByteArray) {
        file.appendBytes(bytes)
    }
}