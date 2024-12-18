package live.jkbx.zeroshare.utils

import io.github.vinceglb.filekit.core.PlatformFile
import live.jkbx.zeroshare.socket.FileTransferMetadata
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig

actual fun PlatformFile.toFileMetaData(): FileTransferMetadata {
    val file = file
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

actual fun getKoinContext(): Any {
    TODO("Not yet implemented")
}