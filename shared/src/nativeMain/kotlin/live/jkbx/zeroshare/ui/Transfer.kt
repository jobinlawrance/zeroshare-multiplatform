package live.jkbx.zeroshare.ui

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.window.ComposeUIViewController
import live.jkbx.zeroshare.socket.FileTransferMetadata
import platform.UIKit.UIViewController

fun DefaultFilePickerController(onClick: () -> Unit): UIViewController = ComposeUIViewController {
    DefaultFilePicker(
        onClick = onClick
    )
}

@OptIn(ExperimentalComposeApi::class)
fun FileDetailsController(meta: FileTransferMetadata, onClick: () -> Unit): UIViewController {
    return ComposeUIViewController(configure = {
        opaque = false
    }) {
        FileDetails(
            onClick = onClick,
            meta = meta
        )
    }
}

fun SelectedDeviceController(
    item: DropdownItem, isSelected: Boolean, onClick: () -> Unit, showTick: Boolean
): UIViewController = ComposeUIViewController {
    DialogListItem(
        item = item,
        isSelected = isSelected,
        onClick = onClick,
        showTick = showTick,
    )
}

@OptIn(ExperimentalComposeApi::class)
fun DeviceSelectorController(
    selectedOption: DropdownItem,
    dialogItems: List<DropdownItem>,
    onSelected: (DropdownItem) -> Unit,
    onDismiss: () -> Unit,
): UIViewController = ComposeUIViewController(configure = {
    opaque = false
}) {
    DeviceListDialog(
        selectedOption = selectedOption,
        dialogItems = dialogItems,
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalComposeApi::class)
fun IncomingFileDialogController(
    meta: FileTransferMetadata,
    onDismiss: () -> Unit,
    onClick: (Boolean) -> Unit,
): UIViewController = ComposeUIViewController(configure = {
    opaque = false
}) {
    IncomingFileDialog(
        meta = meta,
        onDismiss = onDismiss,
        onClick = onClick,
    )
}