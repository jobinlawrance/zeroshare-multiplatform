package live.jkbx.zeroshare.utils

import androidx.compose.runtime.Composable
import live.jkbx.zeroshare.models.SSEEvent

expect fun openUrlInBrowser(url: String)

expect suspend fun getMachineName(): String

@Composable
expect fun loginWithGoogle(onLoginSuccess: (SSEEvent) -> Unit,
                           onLoginError: (Throwable) -> Unit)

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform