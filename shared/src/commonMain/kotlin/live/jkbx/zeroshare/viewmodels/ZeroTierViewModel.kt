package live.jkbx.zeroshare.viewmodels

import live.jkbx.zeroshare.ViewModel
import live.jkbx.zeroshare.network.BackendApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ZeroTierViewModel : ViewModel(), KoinComponent {
    private val backendApi by inject<BackendApi>()

    fun creteNetworkURL(sessionToken: String) = backendApi.creteNetworkURL(sessionToken)

    suspend fun listenToLogin(token: String, onReceived: (networkId: String) -> Unit) {
        backendApi.listenToLogin(token, onReceived)
    }

    suspend fun setNodeId(nodeId: String, machineName: String, networkId: String) =
        backendApi.setNodeId(nodeId, machineName, networkId)


    suspend fun loginWithGoogle() {

    }

}