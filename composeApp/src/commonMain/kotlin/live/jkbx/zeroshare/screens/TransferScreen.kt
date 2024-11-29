package live.jkbx.zeroshare.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.socket.FileTransferListener
import live.jkbx.zeroshare.socket.JavaSocketFileTransfer
import org.koin.core.component.KoinComponent
import live.jkbx.zeroshare.socket.KtorServer
import live.jkbx.zeroshare.socket.SimpleFTPClient
import live.jkbx.zeroshare.socket.SimpleFTPServer
import live.jkbx.zeroshare.socket.fromPlatformFile
import live.jkbx.zeroshare.socket.FileSaver

class TransferScreen : KoinComponent, Screen {

    @Composable
    override fun Content() {
        val log by injectLogger("Transfer Screen")
        val fileTransfer = JavaSocketFileTransfer(log)
        val ftpServer = SimpleFTPServer(log)
        val ktorServer = KtorServer(log)
        val ftpClient = SimpleFTPClient(log)
        val directory: PlatformDirectory? by remember { mutableStateOf(null) }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val singleFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(),
            title = "Single file picker",
            mode = PickerMode.Single,
            initialDirectory = directory?.path,
            onResult = { file ->
                log.d { "file: $file" }
                scope.launch {
//                    ktorServer.sendFile(ipAddress = "69.69.0.4", port = 6969, file = file!!)
                    val fileWrapper = fromPlatformFile(file!!)
                    fileTransfer.sendFile(
                        "69.69.0.5",
                        fileWrapper,
                        object : FileTransferListener {
                            override fun onSpeedUpdate(speedString: String) {
                                log.d {"Transfer Speed: $speedString"}
                            }

                            override fun onTransferProgress(progress: Float) {
                                log.d {"Transfer Progress: $progress%" }
                            }

                            override fun onTransferComplete() {
                                log.d {"Transfer Complete"}
                            }

                            override fun onError(throwable: Throwable) {
                                log.e(throwable) {"Transfer Error: ${throwable.message}"}
                            }
                        })
                }
            },
            platformSettings = null
        )

        LaunchedEffect(Unit) {
            fileTransfer.startServer { metadata, receivedBytes ->
                // Handle received file
                log.d {"Received file: ${metadata.fileName}"}

                // Save or process received bytes
                FileSaver.saveFile(metadata.fileName, receivedBytes)
            }
//            ftpServer.start()
//            ktorServer.startServer()
        }

        Column {
            Text(
                text = "File Sharing",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    singleFilePicker.launch()
                }) {
                    Text("Pick a file")
                }
            }
        }
    }
}