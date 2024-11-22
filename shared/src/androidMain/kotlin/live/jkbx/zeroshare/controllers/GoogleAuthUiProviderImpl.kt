package live.jkbx.zeroshare.controllers

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import co.touchlab.kermit.Logger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import live.jkbx.zeroshare.models.GoogleAuthCredentials
import live.jkbx.zeroshare.models.GoogleUser

internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentials: GoogleAuthCredentials,
    private val credentialManager: CredentialManager = CredentialManager.create(activityContext),
) :
    GoogleAuthUiProvider {
    private val logger = Logger.withTag("GoogleAuthUiProvider")

    override suspend fun signIn(): GoogleUser? {
        return try {
            val credential = credentialManager.getCredential(
                context = activityContext,
                request = getCredentialRequest()
            ).credential
            getGoogleUserFromCredential(credential)
        } catch (e: GetCredentialException) {
            logger.e(e) { "GoogleAuthUiProvider error: ${e.message}" }
            null
        } catch (e: NullPointerException) {
            logger.e(e) { "NPE" }
            null
        }
    }

    private fun getGoogleUserFromCredential(credential: Credential): GoogleUser? {
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        displayName = googleIdTokenCredential.displayName ?: "",
                        profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                } catch (e: GoogleIdTokenParsingException) {
                  logger.e("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    null
                }
            }

            else -> null
        }
    }

    private fun getCredentialRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(getGoogleIdOption(serverClientId = credentials.serverId))
            .build()
    }

    private fun getGoogleIdOption(serverClientId: String): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    }
}