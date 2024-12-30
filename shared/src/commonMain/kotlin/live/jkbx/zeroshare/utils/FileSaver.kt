package live.jkbx.zeroshare.utils

expect class FileSaver(fileName: String) {

    fun append(bytes: ByteArray)
}