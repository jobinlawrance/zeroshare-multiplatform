package live.jkbx.zeroshare.controllers

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import live.jkbx.zeroshare.di.injectLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class ZeroController : KoinComponent {
    val settings by inject<Settings>()

    val logger: Logger by injectLogger("ZeroController")

    fun saveNetworkId(networkId: String) {
        settings.putString("networkId", networkId)
        logger.i { "Network ID saved: $networkId" }
    }

    fun getNetworkId(): String {
        val networkId = settings.getStringOrNull("networkId") ?: ""
        logger.i { "Network ID Retrieved: $networkId" }
        return networkId
    }
}