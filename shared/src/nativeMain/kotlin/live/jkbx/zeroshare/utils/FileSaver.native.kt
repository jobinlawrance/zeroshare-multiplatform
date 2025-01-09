package live.jkbx.zeroshare.utils

import kotlinx.cinterop.ExperimentalForeignApi
import live.jkbx.zeroshare.socket.getPublicDirectory
import live.jkbx.zeroshare.socket.toNSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForUpdatingURL
import platform.Foundation.seekToEndOfFile
import platform.Foundation.writeToURL

actual class FileSaver actual constructor(fileName: String) {
    private val filePath: String = "${getPublicDirectory()}/$fileName"

    @OptIn(ExperimentalForeignApi::class)
    actual fun append(bytes: ByteArray) {
        val fileURL = NSURL.fileURLWithPath(filePath)
        val fileManager = NSFileManager.defaultManager

        println("File path: $filePath")

        if (!fileManager.fileExistsAtPath(filePath)) {
            // If the file doesn't exist, create it
            println("File doesn't exist, creating it")
            fileManager.createFileAtPath(filePath, null, null)
        }
            // Open the file for appending
        val fileHandle = NSFileHandle.fileHandleForUpdatingURL(fileURL, null)
        fileHandle?.seekToEndOfFile()
        fileHandle?.writeData(bytes.toNSData(), null)
        fileHandle?.closeFile()
    }
}