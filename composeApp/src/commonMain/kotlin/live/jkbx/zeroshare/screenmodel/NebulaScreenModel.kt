package live.jkbx.zeroshare.screenmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import live.jkbx.zeroshare.nebula.IncomingSite
import live.jkbx.zeroshare.nebula.Key
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.utils.uniqueDeviceId


class NebulaScreenModel(
    private val log: Logger,
    private val backendApi: BackendApi,
    private val nebula: Nebula,

    ) : ScreenModel {

    private var _incomingSiteState = mutableStateOf<IncomingSite?>(null)
    val incomingSite: State<IncomingSite?> = _incomingSiteState

    val messages = MutableStateFlow<List<String>>(emptyList())

    val downloadMessage = mutableStateOf("")

    val showSignAlert = mutableStateOf(false)

    val globalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var kp: Key

    init {
        log.d { "Init Model" }
        globalScope.launch {
            if (!isActive) return@launch
            kp = nebula.generateKeyPair(messages = { messages.update { msg -> msg + it } },
                downloadProgress = {
                    downloadMessage.value = "Download Progress - $it %"
                    if (it == 100) {
                        globalScope.launch {
                            delay(500)
                            downloadMessage.value = ""
                        }
                    }
                })
            signAndInstall()
        }
    }

    fun signAndInstall() {
        globalScope.launch {
            messages.update { msg -> msg + "Signing the public key" }
            val cert = nebula.signCertificate(kp.publicKey)
            messages.update { msg -> msg + "Signed the keys successfully" }
            nebula.parseCert(cert.caCert)
            nebula.verifyCertAndKey(cert.signedKey, kp.privateKey)
            val incomingSite = cert.incomingSite
            incomingSite.ca = cert.caCert
            incomingSite.cert = cert.signedKey
            incomingSite.key = kp.privateKey
            _incomingSiteState.value = incomingSite
        }
    }

    override fun onDispose() {
        super.onDispose()
        log.d { "Disposing Model" }
    }
}