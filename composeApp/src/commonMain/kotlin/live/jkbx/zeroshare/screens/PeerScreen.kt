package live.jkbx.zeroshare.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.russhwolf.settings.Settings
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.ZeroTierPeer
import live.jkbx.zeroshare.di.networkIdKey
import live.jkbx.zeroshare.models.Member
import live.jkbx.zeroshare.utils.getPlatform
import live.jkbx.zeroshare.viewmodels.ZeroTierViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PeerScreen : Screen, KoinComponent {

    val settings by inject<Settings>()
    val zeroTierViewModel by inject<ZeroTierViewModel>()
    val zeroTierPeer by inject<ZeroTierPeer>()

    @Composable
    override fun Content() {
        var members by remember { mutableStateOf<List<Member>?>(null) }
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val directory: PlatformDirectory? by remember { mutableStateOf(null) }
        var files: Set<PlatformFile> by remember { mutableStateOf(emptySet()) }
        var remoteAddress = ""

        val singleFilePicker = rememberFilePickerLauncher(
            type = PickerType.ImageAndVideo,
            title = "Single file picker",
            initialDirectory = directory?.path,
            onResult = { file ->
                scope.launch {
                    zeroTierPeer.sendFile(remoteAddress, 9999, file!!)
                }
            },
            platformSettings = null
        )

        DisposableEffect(Unit) {

            scope.launch {
                // Perform your API request
                members = null // Reset to show loading
                members = zeroTierViewModel.getZTPeers()
                zeroTierPeer.startServer(9999)
            }

            onDispose {
                job.cancel() // Ensure cleanup
            }
        }

        Column {
            Text(
                text = "Peers",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val data = members) {
                    null -> {
                        // Show loading
                        CircularProgressIndicator(color = Color.White)
                    }

                    else -> {
                        // Show node list
                        MemberList(
                            nodes = data,
                            myNodeId = settings.getString(networkIdKey, ""),
                            onClick = { ipAddress ->
                                scope.launch {
                                    remoteAddress = ipAddress
                                    zeroTierPeer.sendMessage(
                                        ipAddress,
                                        9999,
                                        "Hello from ${getPlatform().name}"
                                    )
                                }
                            })
                    }
                }
            }

            Button(
                onClick = {
                    singleFilePicker.launch()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Pick a file")
            }
        }

    }
}

@Composable
fun MemberList(nodes: List<Member>, myNodeId: String, onClick: (ipAddress: String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(nodes.size) { index ->
            MemberItem(node = nodes[index], isHighlighted = nodes[index].id == myNodeId, onClick)
        }
    }
}

@Composable
fun MemberItem(node: Member, isHighlighted: Boolean, onClick: (ipAddress: String) -> Unit) {
    val textColor = if (isHighlighted) Color.Yellow else Color.White
    val dateFormatter = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(node.creationTime))
//    val image = when {
//        node.platform.contains("Android") -> Res.drawable.android
//        node.platform.contains("iOS") -> Res.drawable.apple
//        else -> Res.drawable.laptop
//    }

    Card(
        shape = RoundedCornerShape(16.dp),
        onClick = {
            onClick(node.ipAssignments.first())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading avatar
//            Image(
//                painter = painterResource(image), // Placeholder avatar
//                contentDescription = null,
//                modifier = Modifier
//                    .size(48.dp)
//                    .padding(8.dp)
//            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = node.name,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = node.ipAssignments.first(),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Timestamp
            Text(
                text = formattedDate,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
