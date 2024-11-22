package live.jkbx.zeroshare.utils

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