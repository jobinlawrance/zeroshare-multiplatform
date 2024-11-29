package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Marker
import org.slf4j.event.Level
import java.io.ByteArrayInputStream
import java.io.File

class KtorServer(private val log: Logger) : KoinComponent {

    val client by inject<HttpClient>()

    fun startServer() = CoroutineScope(Dispatchers.IO).launch {
        log.d { "Starting Ktor Server" }
        embeddedServer(CIO, port = 6969) {
            install(CallLogging) {
                level = Level.INFO
                logger = getKermitLoggerForSLF4J(log)
            }
            routing {
                post("/upload") {
                    val multipart = call.receiveMultipart()
                    var fileName: String? = null
                    val chunkNumber = call.request.headers["chunk-number"]?.toIntOrNull()
                    val totalChunks = call.request.headers["total-chunks"]?.toIntOrNull()

                    log.d { "Chunk Number: $chunkNumber, Total Chunks: $totalChunks" }

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            fileName = part.originalFileName
                            if (chunkNumber != null && totalChunks != null) {
                                // Handle chunked file upload
                                log.d { "Received chunk $chunkNumber of $totalChunks" }

                                // Store the chunk in a temporary location
                                val chunkFile =
                                    File("${getPublicDirectory()}/$fileName.$chunkNumber")
                                chunkFile.writeBytes(
                                    part.provider().readRemaining().readByteArray()
                                )

                                // If all chunks have been received, reassemble the file
                                if (chunkNumber == totalChunks) {
                                    // Reassemble the file from the temporary chunks
                                    val finalFile = File("${getPublicDirectory()}/$fileName")
                                    val outputStream = finalFile.outputStream()

                                    for (i in 1..totalChunks) {
                                        val chunkedFile =
                                            File("${getPublicDirectory()}/$fileName.$i")
                                        chunkedFile.inputStream().use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                        chunkedFile.delete() // Delete the temporary chunk file
                                    }

                                    outputStream.close()
                                    log.d { "File $fileName reassembled successfully" }
                                }

                            } else {
                                // Handle single file upload as before
                                fileName = part.originalFileName
                                val fileBytes = part.provider().readRemaining().readByteArray()
                                File("${getPublicDirectory()}/$fileName").writeBytes(fileBytes)
                            }
                        }
                        part.dispose()
                    }

                    if (fileName != null) {
                        call.respondText("File $fileName uploaded successfully!")
                    } else {
                        call.respondText("File upload failed!")
                    }
                }
            }
        }.start(wait = true)
        log.d { "Ktor Server started" }
    }

    @OptIn(InternalAPI::class)
    suspend fun sendFile(ipAddress: String = "", port: Int = 6969, file: PlatformFile) {
        val chunkSize = 1024 * 1024 // 1 MB

        val byteArray = file.readBytes()
        val inputStream = ByteArrayInputStream(byteArray)

        var offset = 0
        val chunks = mutableListOf<ByteArray>()
        while (offset < byteArray.size) {
            val chunkedSize = minOf(chunkSize, byteArray.size - offset)
            val chunk = ByteArray(chunkedSize)
            inputStream.read(chunk, 0, chunkedSize)
            chunks.add(chunk)
            offset += chunkedSize
        }

        chunks.forEachIndexed { index, chunk ->
            client.submitFormWithBinaryData(
                url = "http://$ipAddress:$port/upload",
                formData = formData {
                    append("file", chunk, Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"file\"; filename=\"${file.name}\";"
                        )
                    })
                }) {
                header("chunk-number", index + 1)
                header("total-chunks", chunks.size)
            }
        }
    }
}