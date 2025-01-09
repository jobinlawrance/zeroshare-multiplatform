package live.jkbx.zeroshare.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.utils.applicationMimeTypes
import live.jkbx.zeroshare.utils.codeRelatedMimeTypes
import live.jkbx.zeroshare.utils.compressedFileMimeTypes
import live.jkbx.zeroshare.utils.convertByteSize
import live.jkbx.zeroshare.utils.databaseMimeTypes
import live.jkbx.zeroshare.utils.documentMimeTypes
import live.jkbx.zeroshare.utils.excelMimeTypes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import zeroshare.shared.generated.resources.Res
import zeroshare.shared.generated.resources.album
import zeroshare.shared.generated.resources.android
import zeroshare.shared.generated.resources.apple
import zeroshare.shared.generated.resources.application
import zeroshare.shared.generated.resources.casette
import zeroshare.shared.generated.resources.check_mark
import zeroshare.shared.generated.resources.code
import zeroshare.shared.generated.resources.crossed
import zeroshare.shared.generated.resources.database
import zeroshare.shared.generated.resources.docs
import zeroshare.shared.generated.resources.flames
import zeroshare.shared.generated.resources.laptop
import zeroshare.shared.generated.resources.rar
import zeroshare.shared.generated.resources.spreedsheet
import zeroshare.shared.generated.resources.video

@Composable
fun DefaultFilePicker(
    modifier: Modifier = Modifier.fillMaxSize(), onClick: () -> Unit
) {
    Box(
        modifier = modifier.background(Color.Black).clip(RoundedCornerShape(12.dp)).background(
            color = Color(35, 35, 35), shape = RoundedCornerShape(12.dp)
        ).clickable(onClick = {
            onClick()
        }).padding(16.dp),
    ) {
        Row {
            Image(
                bitmap = imageResource(Res.drawable.flames),
                modifier = Modifier.size(48.dp).align(Alignment.CenterVertically),
                contentDescription = "folder",
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Click to select a file", color = Color.White, modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun FileDetails(modifier: Modifier = Modifier.fillMaxSize(), onClick: () -> Unit, meta: FileTransferMetadata) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = Color(35, 35, 35), shape = RoundedCornerShape(12.dp)
            ).clickable(onClick = {
                onClick()
            }).padding(16.dp)
    ) {
        RowOrColumn {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(
                    Color(25, 25, 25),
                ).border(
                    border = BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(12.dp)
                ).padding(16.dp).fillMaxWidthWithPredicate {
                    maxWidth < maxHeight
                }) {
                Column(modifier = Modifier.align(Alignment.Center)) {
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
                        modifier = Modifier.size(84.dp).align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    meta.extension?.let {
                        Text(
                            it.replace(".", "").uppercase(),
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column {
                Text(fontFamily = FontFamily.Monospace, color = Color.White, text = "Name: ${meta.fileName}")
                Text(
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    text = "Size: ${meta.fileSize.convertByteSize()}"
                )
                Text(fontFamily = FontFamily.Monospace, color = Color.White, text = "Type: ${meta.mimeType}")
            }
        }
    }
}

fun Modifier.fillMaxWidthWithPredicate(predicate: () -> Boolean): Modifier {
    return if (predicate()) {
        this.fillMaxWidth()
    } else this
}

@Composable
fun RowOrColumn(content: @Composable () -> Unit) {
    BoxWithConstraints {
        println("Max Width: ${maxWidth}")
        println("Max Height: ${maxHeight}")
        if (maxWidth > maxHeight) {
            Row {
                content()
            }
        } else {
            Column {
                content()
            }
        }
    }
}

@Composable
fun DialogListItem(
    modifier: Modifier = Modifier.fillMaxSize().background(Color.Black),
    item: DropdownItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    showTick: Boolean
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(
            enabled = !item.disabled, onClick = onClick
        ).background(
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
            modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
            Text(
                text = item.name, color = if (item.disabled) Color.Gray else Color.White, fontSize = 16.sp
            )
            Text(
                text = item.ipAddress, color = Color(180, 180, 180), fontSize = 13.sp
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
    val name: String, val disabled: Boolean, val id: String, val platform: String, val ipAddress: String
)

@Composable
fun CircularImageButton(
    imageRes: DrawableResource, contentDescription: String, onClick: () -> Unit, modifier: Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListDialog(
    selectedOption: DropdownItem,
    dialogItems: List<DropdownItem>,
    onSelected: (DropdownItem) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {

        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = Color(25, 25, 25), // Light background color
            contentColor = Color(0xFF000000) // Dark text color
        ) {
            // Dialog Content
            LazyColumn(
                modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(dialogItems.size) { index ->
                    DialogListItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = dialogItems[index],
                        isSelected = selectedOption.id == dialogItems[index].id,
                        onClick = {
                            if (!dialogItems[index].disabled) {
                                onSelected(dialogItems[index])
                                onDismiss()
                            }
                        },
                        showTick = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingFileDialog(meta: FileTransferMetadata, onDismiss: () -> Unit, onClick: (Boolean) -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
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
                FileDetails(onClick = {}, meta = meta, modifier = Modifier.width(560.dp))
                Spacer(modifier = Modifier.size(16.dp))
                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        CircularImageButton(
                            modifier = Modifier.align(Alignment.Center),
                            imageRes = Res.drawable.crossed,
                            onClick = {
                                onDismiss()
                                onClick(false)
                            },
                            contentDescription = "Decline"
                        )

                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CircularImageButton(
                            imageRes = Res.drawable.check_mark,
                            contentDescription = "Accept",
                            onClick = {
                                onDismiss()
                                onClick(true)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}