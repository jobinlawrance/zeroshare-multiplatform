package live.jkbx.zeroshare.viewmodels

import live.jkbx.zeroshare.ViewModel
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.network.BackendApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginViewModel : ViewModel(), KoinComponent {
    private val backendApi by inject<BackendApi>()

    fun creteNetworkURL(sessionToken: String) = backendApi.creteNetworkURL(sessionToken)

    suspend fun listenToLogin(token: String, onReceived: (sseEvent: SSEEvent) -> Unit, onError: (Throwable) -> Unit) {
        backendApi.listenToLogin(token, onReceived, onError)
    }

    suspend fun verifyGoogleToken(token: String): SSEEvent {
        return backendApi.verifyGoogleToken(token)
    }
}