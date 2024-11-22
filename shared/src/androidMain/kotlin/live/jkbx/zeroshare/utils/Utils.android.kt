package live.jkbx.zeroshare.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.koin.java.KoinJavaComponent.inject

actual fun openUrlInBrowser(url: String) {
    TODO("Call it with activity context")
}

actual suspend fun getMachineName(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}