package live.jkbx.zeroshare.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.screenmodel.TransferScreenModel
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.ui.DefaultFilePicker
import live.jkbx.zeroshare.ui.DeviceListDialog
import live.jkbx.zeroshare.ui.DialogListItem
import live.jkbx.zeroshare.ui.DropdownItem
import live.jkbx.zeroshare.ui.FileDetails
import live.jkbx.zeroshare.ui.IncomingFileDialog
import live.jkbx.zeroshare.utils.toFileMetaData
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.jetbrains.compose.resources.imageResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zeroshare.composeapp.generated.resources.Res
import zeroshare.composeapp.generated.resources.paper_plane


class TransferScreen : KoinComponent, Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val log by injectLogger("Transfer Screen")
        val directory: PlatformDirectory? by remember { mutableStateOf(null) }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val transferVM = rememberScreenModel { TransferScreenModel() }
        val json by inject<Json>()
        var showDialog by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf<DropdownItem?>(null) }
        var selectedFileMeta by remember { mutableStateOf<FileTransferMetadata?>(null) }
        var selectedFile by remember { mutableStateOf<PlatformFile?>(null) }

        val showIncomingDialog by remember { transferVM.incomingFileDialog }
        val incomingFile by remember { transferVM.incomingFile }
        var incomingDevice by remember { transferVM.incomingDevice }

        val singleFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(),
            title = "Single file picker",
            mode = PickerMode.Single,
            initialDirectory = directory?.path,
            onResult = { platformFile ->
                val fileMetadata = platformFile!!.toFileMetaData()
                selectedFileMeta = fileMetadata
                selectedFile = platformFile
            },
            platformSettings = null
        )

        val dialogItems = transferVM.devices.value.map {
            DropdownItem(
                id = it.iD,
                name = it.machineName,
                disabled = uniqueDeviceId() == it.deviceId,
                platform = it.platform,
                ipAddress = it.ipAddress
            )
        }

        val defaultDevice = transferVM.defaultDevice.value

        LaunchedEffect(defaultDevice) {
            if (defaultDevice != null) {
                selectedOption = DropdownItem(
                    name = defaultDevice.machineName,
                    ipAddress = defaultDevice.ipAddress,
                    platform = defaultDevice.platform,
                    disabled = false,
                    id = defaultDevice.iD
                )
            }
        }

        Column {
            Text(
                text = "File Sharing",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column {

                    if (selectedFileMeta == null) {
                        DefaultFilePicker(
                            modifier = Modifier.align(Alignment.CenterHorizontally).width(560.dp),
                            onClick = {
                                singleFilePicker.launch()
                            })
                    } else {
                        FileDetails(
                            modifier = Modifier.align(Alignment.CenterHorizontally).width(560.dp), {
                                singleFilePicker.launch()
                            }, meta = selectedFileMeta!!
                        )
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    Text(
                        text = "Select device to Send",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    // Main UI
                    selectedOption?.let {
                        Box(
                            modifier = Modifier
                                .width(560.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            DialogListItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = it,
                                isSelected = true,
                                onClick = { showDialog = true },
                                showTick = false,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                        enabled = selectedFileMeta != null && selectedOption != null,
                        onClick = {
                            scope.launch {
                                transferVM.sendDownloadRequest(selectedOption!!.id, selectedFileMeta!!, selectedFile!!)
                            }
                        }) {
                        Text("Send")
                        Spacer(modifier = Modifier.size(8.dp))
                        Image(
                            modifier = Modifier.size(20.dp),
                            bitmap = imageResource(Res.drawable.paper_plane),
                            contentDescription = "send"
                        )
                    }
                }
            }

            // Dialog
            if (showDialog) {
                DeviceListDialog(
                    selectedOption = selectedOption!!,
                    onDismiss = { showDialog = false },
                    dialogItems = dialogItems,
                    onSelected = { selectedOption = it }
                )
            }

            if (showIncomingDialog) {
                IncomingFileDialog(
                    meta = incomingFile!!,
                    onDismiss = { transferVM.incomingFileDialog.value = false },
                    onClick = { accept ->
                        transferVM.sendAcknowledgement(accept = accept)
                    }
                )
            }

        }
    }

}

suspend fun PointerInputScope.detectHover(
    onEnter: () -> Unit, onExit: () -> Unit
) {
    while (true) {
        val event = awaitPointerEventScope {
            awaitPointerEvent(PointerEventPass.Initial)
        }
        when (event.type) {
            PointerEventType.Enter -> onEnter()
            PointerEventType.Exit -> onExit()
        }
    }
}
