package live.jkbx.zeroshare.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.koin.java.KoinJavaComponent.inject

actual fun openUrlInBrowser(url: String) {
    val context by inject<Context>(Context::class.java)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for application context
    }
    context.startActivity(intent)
}

actual suspend fun getMachineName(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}