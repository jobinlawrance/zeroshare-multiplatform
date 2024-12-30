package live.jkbx.zeroshare.utils

actual class FileSaver actual constructor(fileName: String) {
    private val userHome = System.getProperty("user.home")
    private val file = java.io.File("$userHome/Documents/$fileName")

    actual fun append(bytes: ByteArray) {
        file.appendBytes(bytes)
    }
}