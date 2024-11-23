package live.jkbx.zeroshare.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.russhwolf.settings.Settings
import live.jkbx.zeroshare.controllers.GoogleAuthProvider
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.viewmodels.ZeroTierViewModel
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject

actual fun openUrlInBrowser(url: String) {
    TODO("Call it with activity context")
}

actual suspend fun getMachineName(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}


@Composable
actual fun loginWithGoogle(
    onLoginSuccess: (SSEEvent) -> Unit,
    onLoginError: (Throwable) -> Unit
) {
    val googleAuthProvider: GoogleAuthProvider by inject(GoogleAuthProvider::class.java)
    val zeroTierViewModel by inject<ZeroTierViewModel>(ZeroTierViewModel::class.java)
    val uiProvider = googleAuthProvider.getUiProvider()

    LaunchedEffect(Unit) {
        try {
            val googleUser = uiProvider.signIn()
            val sseEvent = zeroTierViewModel.verifyGoogleToken(googleUser!!.idToken)

            onLoginSuccess(sseEvent)
        }  catch (e: Exception) {
            onLoginError(e)
        }
    }
}

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()