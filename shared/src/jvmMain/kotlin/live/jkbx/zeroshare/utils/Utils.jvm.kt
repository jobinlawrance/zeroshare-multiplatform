package live.jkbx.zeroshare.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.viewmodels.ZeroTierViewModel
import org.koin.java.KoinJavaComponent.inject
import java.awt.Desktop
import java.net.InetAddress
import java.net.URI
import java.util.UUID

actual fun openUrlInBrowser(url: String) {
    Desktop.getDesktop().browse(URI(url))
}

actual suspend fun getMachineName(): String {
    return try {
        withContext(Dispatchers.IO) {
            InetAddress.getLocalHost()
        }.hostName
    } catch (e: Exception) {
        throw IllegalStateException("Failed to get machine name", e)
    }
}

@Composable
actual fun loginWithGoogle(
    onLoginSuccess: (SSEEvent) -> Unit,
    onLoginError: (Throwable) -> Unit
) {
    val zeroTierViewModel by inject<ZeroTierViewModel>(ZeroTierViewModel::class.java)
    val sessionToken = UUID.randomUUID().toString()
    val url = zeroTierViewModel.creteNetworkURL(sessionToken)
    val settings by inject<Settings>(Settings::class.java)

    openUrlInBrowser(url)
    LaunchedEffect(Unit) {
        zeroTierViewModel.listenToLogin(sessionToken, { sseEvent ->
            settings.putString(tokenKey, sseEvent.token)
            onLoginSuccess(sseEvent)
        })
    }
}

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun uniqueDeviceId(): String {
    val settings by inject<Settings>(Settings::class.java)
    val deviceId = settings.get("device_id", "")
    if (deviceId.isEmpty()) {
        val uuid = UUID.randomUUID().toString()
        settings.putString("device_id", uuid)
        return uuid
    }
    return deviceId
}