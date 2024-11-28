package live.jkbx.zeroshare.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.nebula.IncomingSite
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NebulaScreen: Screen, KoinComponent {

    @Composable
    override fun Content() {
        val incomingSiteState = remember { mutableStateOf<IncomingSite?>(null) }
        val log: Logger by injectLogger("LoginScreen")
        val nebula by inject<Nebula>()
        val backendApi by inject<BackendApi>()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            val kp = nebula.generateKeyPair()
            scope.launch {
                val cert = backendApi.signPublicKey(kp.publicKey, uniqueDeviceId())
                val parseResult = nebula.parseCert(cert.caCert)
                val verifyResult = nebula.verifyCertAndKey(cert.signedKey, kp.privateKey)
                log.d { "ParseResult $parseResult" }
                log.d { "VerifyResult $verifyResult" }
                val incomingSite = cert.incomingSite
                incomingSite.ca = cert.caCert
                incomingSite.cert = cert.signedKey
                incomingSite.key = kp.privateKey
                incomingSiteState.value = incomingSite
            }
        }

        // Invoke the composable function when the state is ready
        incomingSiteState.value?.let { incomingSite ->
            val dir = nebula.saveIncomingSite(incomingSite)
            log.d { "Site saved to $dir" }
            navigator.replace(TransferScreen())
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
                Text(
                    text = "Nebula",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}