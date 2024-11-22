package live.jkbx.zeroshare.controllers

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import live.jkbx.zeroshare.models.GoogleAuthCredentials

class GoogleAuthProviderImpl(
    private val credentials: GoogleAuthCredentials,
    private val credentialManager: CredentialManager,
) : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        val context = LocalContext.current
        return GoogleAuthUiProviderImpl(
            activityContext = context,
            credentialManager = credentialManager,
            credentials = credentials,
        )
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}