package live.jkbx.zeroshare.utils

import androidx.compose.runtime.Composable
import live.jkbx.zeroshare.models.SSEEvent
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice


actual fun openUrlInBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url)

    UIApplication.sharedApplication.openURL(nsUrl!!, options = emptyMap<Any?, Any>()) { success ->
        if (success) {
            println("URL opened successfully")
        } else {
            println("Failed to open URL")
        }
    }
}

actual suspend fun getMachineName(): String {
    return UIDevice.currentDevice.name
}

@Composable
actual fun loginWithGoogle(
    onLoginSuccess: (SSEEvent) -> Unit,
    onLoginError: (Throwable) -> Unit
) {
    TODO("Not yet implemented")
}

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun uniqueDeviceId(): String {
    return UIDevice().identifierForVendor?.UUIDString!!
}