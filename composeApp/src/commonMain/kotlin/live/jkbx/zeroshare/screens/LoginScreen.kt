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
import androidx.compose.runtime.LaunchedEffect
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.nebulaSetupKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.utils.getMachineName
import live.jkbx.zeroshare.utils.getPlatform
import live.jkbx.zeroshare.utils.loginWithGoogle
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zeroshare.composeapp.generated.resources.Res
import zeroshare.composeapp.generated.resources.nebula
import zeroshare.composeapp.generated.resources.neural
import zeroshare.composeapp.generated.resources.search


class LoginScreen : Screen, KoinComponent {

    @Composable
    override fun Content() {
        val log: Logger by injectLogger("LoginScreen")
        val backendApi by inject<BackendApi>()
        val buttonEnabled = remember { mutableStateOf(true) }
        val descriptionText = remember { mutableStateOf("") }
        val descriptionVisible = remember { mutableStateOf(false) }
        val login = remember { mutableStateOf<Boolean?>(null) }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val navigator = LocalNavigator.currentOrThrow
        val settings by inject<Settings>()

        LaunchedEffect(Unit) {
            if (settings.getString(tokenKey, "").isNotEmpty()) {
                if (settings.getBoolean(nebulaSetupKey, false)) {
 //                    navigator.replace(TransferScreen())
                    navigator.replace(NebulaScreen())
                } else {
                    navigator.replace(NebulaScreen())
                }
            }
        }

        // Invoke the composable function when the state is ready
        login.value?.let {
            loginWithGoogle(
                onLoginSuccess = { sseEvent ->
                    log.d { "$sseEvent" }

                    scope.launch {
                        backendApi.setDeviceDetails(
                            machineName = getMachineName(),
                            platformName = getPlatform().name,
                            deviceId = uniqueDeviceId()
                        )
                        if (settings.getBoolean(nebulaSetupKey, false)) {
                            navigator.replace(TransferScreen())
                        } else {
                            navigator.replace(NebulaScreen())
                        }
                    }
                },
                onLoginError = { log.e(it) { "Error" } }
            )
        }

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
                        buttonEnabled.value = false
                        login.value = true
                    }
                ) {
                    Image(
                        painterResource(Res.drawable.search),
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Google Login"
                    )
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
                    painter = painterResource(Res.drawable.nebula), // Replace with your ZeroTier image resource
                    contentDescription = "Nebula Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Powered by Nebula",
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}