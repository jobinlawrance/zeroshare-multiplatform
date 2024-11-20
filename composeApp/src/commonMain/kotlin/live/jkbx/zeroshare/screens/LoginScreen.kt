package live.jkbx.zeroshare.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.connectToNetwork
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.utils.openUrlInBrowser
import live.jkbx.zeroshare.utils.getMachineName
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import zeroshare.composeapp.generated.resources.Res
import zeroshare.composeapp.generated.resources.neural
import zeroshare.composeapp.generated.resources.search
import zeroshare.composeapp.generated.resources.zerotier
import java.util.UUID

class LoginScreen : Screen, KoinComponent {

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val backendApi by inject<BackendApi>(BackendApi::class.java)
    val log by injectLogger("LoginScreen")

    @Composable
    override fun Content() {

        val buttonEnabled = remember { mutableStateOf(true) }
        val descriptionText = remember { mutableStateOf("") }
        val descriptionVisible = remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Replace with your image resource
                Image(
                    painter = painterResource(Res.drawable.neural), // Replace with your image resource
                    contentDescription = "ZeroShare Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ZeroShare",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    enabled = buttonEnabled.value,
                    onClick = {
                        val sessionToken = UUID.randomUUID().toString()
                        val url = backendApi.creteNetworkURL(sessionToken)
                        log.d { "URL: $url" }
                        openUrlInBrowser(url)
                        scope.launch {
                            buttonEnabled.value = false
                            backendApi.listenToLogin(sessionToken, {networkId ->
                                descriptionVisible.value = true
                                descriptionText.value = "Connecting to ZeroTier network $networkId ..."
                                scope.launch {
                                    val machineName = getMachineName()
                                    val nodeId = connectToNetwork(networkId)
                                    backendApi.setNodeId(nodeId, machineName, networkId)
                                }
                            })
                        }
                    }
                ) {
                    Image(painterResource(Res.drawable.search), contentDescription = "Google Login")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Login with Google")
                }

                if (descriptionVisible.value) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = descriptionText.value,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.zerotier), // Replace with your ZeroTier image resource
                    contentDescription = "ZeroTier Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Powered by ZeroTier",
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
fun preview() {
    LoginScreen().Content()
}