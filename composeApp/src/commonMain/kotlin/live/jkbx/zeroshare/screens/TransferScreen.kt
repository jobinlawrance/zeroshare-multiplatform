package live.jkbx.zeroshare.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.screenmodel.TransferScreenModel
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.utils.applicationMimeTypes
import live.jkbx.zeroshare.utils.codeRelatedMimeTypes
import live.jkbx.zeroshare.utils.compressedFileMimeTypes
import live.jkbx.zeroshare.utils.convertByteSize
import live.jkbx.zeroshare.utils.databaseMimeTypes
import live.jkbx.zeroshare.utils.documentMimeTypes
import live.jkbx.zeroshare.utils.excelMimeTypes
import live.jkbx.zeroshare.utils.uniqueDeviceId
import okio.Buffer
import okio.Source
import okio.Timeout
import org.apache.tika.Tika
import org.apache.tika.config.TikaConfig
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zeroshare.composeapp.generated.resources.Res
import zeroshare.composeapp.generated.resources.album
import zeroshare.composeapp.generated.resources.android
import zeroshare.composeapp.generated.resources.apple
import zeroshare.composeapp.generated.resources.application
import zeroshare.composeapp.generated.resources.casette
import zeroshare.composeapp.generated.resources.check_mark
import zeroshare.composeapp.generated.resources.code
import zeroshare.composeapp.generated.resources.crossed
import zeroshare.composeapp.generated.resources.database
import zeroshare.composeapp.generated.resources.docs
import zeroshare.composeapp.generated.resources.flames
import zeroshare.composeapp.generated.resources.laptop
import zeroshare.composeapp.generated.resources.paper_plane
import zeroshare.composeapp.generated.resources.rar
import zeroshare.composeapp.generated.resources.spreedsheet
import zeroshare.composeapp.generated.resources.video
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.Locale


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
        var selectedFile by remember { mutableStateOf<File?>(null) }

        val singleFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(),
            title = "Single file picker",
            mode = PickerMode.Single,
            initialDirectory = directory?.path,
            onResult = { platformFile ->
                log.d { "file: ${platformFile!!.file}" }
                var file = platformFile!!.file
                val tikka = Tika()
                val mimeType = tikka.detect(file)
                val mimeTypes = TikaConfig.getDefaultConfig().mimeRepository
                val mimeTypeObj = mimeTypes.forName(mimeType)

                val fileMetadata = FileTransferMetadata(
                    fileName = file.name,
                    fileSize = file.length(),
                    mimeType = mimeType,
                    extension = mimeTypeObj.extensions.firstOrNull(),
                )

                selectedFileMeta = fileMetadata
                selectedFile = file

                log.d {
                    "Mime type - ${
                        json.encodeToString(
                            FileTransferMetadata.serializer(),
                            fileMetadata
                        )
                    }"
                }
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

                    Spacer(modifier = Modifier.size(16.dp))

                    Text(
                        text = "Select device to Send",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Main UI
                    selectedOption?.let {
                        Box(
                            modifier = Modifier
                                .width(560.dp)
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            DialogListItem(
                                item = it,
                                isSelected = true,
                                onClick = { showDialog = true },
                                showTick = false,
                            )
                        }
                    }

                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                        enabled = selectedFileMeta != null && selectedOption != null,
                        onClick = {
                            scope.launch {
                                transferVM.sendToDevice(selectedOption!!.id, selectedFile!!, selectedFileMeta!!)
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
                BasicAlertDialog(onDismissRequest = { showDialog = false }) {
                    Surface(
                        modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        color = Color(25, 25, 25), // Light background color
                        contentColor = Color(0xFF000000) // Dark text color
                    ) {
                        // Dialog Content
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(dialogItems.size) { index ->
                                DialogListItem(
                                    item = dialogItems[index],
                                    isSelected = selectedOption?.id == dialogItems[index].id,
                                    onClick = {
                                        if (!dialogItems[index].disabled) {
                                            selectedOption = dialogItems[index]
                                            showDialog = false
                                        }
                                    },
                                    showTick = true
                                )
                            }
                        }
                    }
                }
            }

            transferVM.receiveRequest.value?.let { meta ->

                BasicAlertDialog(onDismissRequest = {
                    transferVM.receiveRequest.value = null
                }) {
                    Surface(
                        modifier = Modifier.wrapContentWidth().wrapContentHeight().padding(16.dp),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        color = Color(45, 45, 45), // Light background color
                        contentColor = Color(0xFFFFFFFF) // Dark text color
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Incoming File",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            FileDetails(onClick = {}, meta = meta)
                            Spacer(modifier = Modifier.size(16.dp))
                            Row {
                                Box(modifier = Modifier.weight(1f)) {
                                    CircularImageButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        imageRes = Res.drawable.crossed,
                                        onClick = {},
                                        contentDescription = "Decline"
                                    )

                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    CircularImageButton(
                                        imageRes = Res.drawable.check_mark,
                                        contentDescription = "Accept",
                                        onClick = {},
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
    }


}

// List Item UI
@Composable
fun DialogListItem(
    item: DropdownItem, isSelected: Boolean, onClick: () -> Unit, showTick: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = !item.disabled,
                onClick = onClick
            )
            .background(
                when {
                    isSelected -> Color(35, 35, 35)
                    else -> Color.Transparent
                }, shape = RoundedCornerShape(12.dp)
            ).padding(12.dp)
    ) {

        val image = when {
            item.platform.contains("OS") -> imageResource(Res.drawable.apple)
            item.platform.contains("Android") -> imageResource(Res.drawable.android)
            else -> imageResource(Res.drawable.laptop)
        }
        // Icon
        Image(
            bitmap = image,
            contentDescription = item.name,
            modifier = Modifier.size(24.dp).align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = if (item.disabled) Color.Gray else Color.White,
            )
            Text(
                text = item.ipAddress,
                color = Color(180, 180, 180),
                fontSize = 11.sp
            )
        }

        // Checkmark for selected item
        if (isSelected && showTick) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        if (item.disabled) {
            TextButton(onClick = {}, enabled = false) {
                Text(
                    text = "YOUR DEVICE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp, // Increased font size for better visibility
                    color = Color.White
                )
            }
        }
    }
}

// Data class for dialog items
data class DropdownItem(
    val name: String,
    val disabled: Boolean,
    val id: String,
    val platform: String,
    val ipAddress: String
)

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

internal class FileChannelSource(private val channel: FileChannel, timeout: Timeout) : Source {
    private val timeout: Timeout = timeout

    private var position = channel.position()

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
        check(channel.isOpen) { "closed" }
        if (position == channel.size()) return -1L

        val read = channel.transferTo(position, byteCount, sink)
        position += read
        return read
    }

    override fun timeout(): Timeout {
        return timeout
    }

    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }
}

@Composable
fun FileDetails(modifier: Modifier = Modifier, onClick: () -> Unit, meta: FileTransferMetadata) {
    Box(
        modifier = modifier.padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = Color(35, 35, 35),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = {
                onClick()
            })
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Color(25, 25, 25),
                    ).border(
                        border = BorderStroke(2.dp, Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    val image = when {
                        documentMimeTypes.contains(meta.mimeType) -> Res.drawable.docs
                        compressedFileMimeTypes.contains(meta.mimeType) -> Res.drawable.rar
                        codeRelatedMimeTypes.contains(meta.mimeType) -> Res.drawable.code
                        databaseMimeTypes.contains(meta.mimeType) -> Res.drawable.database
                        applicationMimeTypes.contains(meta.mimeType) -> Res.drawable.application
                        excelMimeTypes.contains(meta.mimeType) -> Res.drawable.spreedsheet
                        meta.mimeType.contains("video") -> Res.drawable.video
                        meta.mimeType.contains("audio") -> Res.drawable.casette
                        meta.mimeType.contains("image") -> Res.drawable.album
                        else -> Res.drawable.docs
                    }

                    Image(
                        bitmap = imageResource(image),
                        contentDescription = "File",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    meta.extension?.let {
                        Text(
                            it.replace(".", "").uppercase(Locale.getDefault()),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(fontFamily = FontFamily.Monospace, text = "Name: ${meta.fileName}")
                Text(
                    fontFamily = FontFamily.Monospace,
                    text = "Size: ${meta.fileSize.convertByteSize()}"
                )
                Text(fontFamily = FontFamily.Monospace, text = "Type: ${meta.mimeType}")
            }
        }
    }
}

@Composable
fun DefaultFilePicker(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = Color(35, 35, 35),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = {
                onClick()
            })
            .padding(16.dp),
    ) {
        Row {
            Image(
                bitmap = imageResource(Res.drawable.flames),
                modifier = Modifier.size(48.dp)
                    .align(Alignment.CenterVertically),
                contentDescription = "folder",
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Click to select a file",
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun CircularImageButton(
    imageRes: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        shape = CircleShape,
        contentPadding = PaddingValues(8.dp), // Removes padding around content
        modifier = modifier.size(64.dp) // Adjust the size as needed
    ) {
        Image(
            bitmap = imageResource(imageRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(24.dp)
        )
    }
}
