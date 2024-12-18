package live.jkbx.zeroshare.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.update
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.screenmodel.NebulaScreenModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NebulaScreen : Screen, KoinComponent {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val log: Logger by injectLogger("LoginScreen")
        val nebula by inject<Nebula>()
        val backendApi by inject<BackendApi>()
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { NebulaScreenModel(log, backendApi, nebula) }
        val messages by viewModel.messages.collectAsState(initial = emptyList())
        val showButton = mutableStateOf(false)

//         Invoke the composable function when the state is ready
        viewModel.incomingSite.value?.let { incomingSite ->
            val dir = nebula.saveIncomingSite(incomingSite) {
                viewModel.messages.update { msg -> msg + it }
            }
            log.d { "Site saved to $dir" }
            showButton.value = true
        }

        if (viewModel.showSignAlert.value) {
            BasicAlertDialog(
                onDismissRequest = {
                    viewModel.showSignAlert.value = false
                }
            ) {
                Surface(
                    modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = Color(0xFFF5F5F5), // Light background color
                    contentColor = Color(0xFF212121) // Dark text color
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "To install the certificates, your system password will be required.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.showSignAlert.value = false

                            },
                            modifier = Modifier.align(Alignment.End)
                        )
                        { Text("Confirm") }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        )
        {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Nebula",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                Box(
                    modifier = Modifier
                        .padding(16.dp)
//                        .fillMaxSize()
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF2F2F2F), shape = RoundedCornerShape(16.dp))
                ) {
                    Column {
                        val listState = rememberLazyListState()
                        Box {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                items(messages.size) { index ->
                                    Text(
                                        text = messages[index],
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (viewModel.downloadMessage.value.isNotEmpty()) {
                        Text(
                            text = viewModel.downloadMessage.value,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        )
                    }
                }

                if (showButton.value) {
                    Button(
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                        onClick = {
                            navigator.replace(TransferScreen())
                        }
                    ) {
                        Text(text = "Continue")
                    }
                }
            }
        }
    }
}