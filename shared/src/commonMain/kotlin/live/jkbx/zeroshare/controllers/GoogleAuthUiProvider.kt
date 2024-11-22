package live.jkbx.zeroshare.controllers

import live.jkbx.zeroshare.models.GoogleUser

interface GoogleAuthUiProvider {

    /**
     * Opens Sign In with Google UI,
     * @return returns GoogleUser
     */
    suspend fun signIn(): GoogleUser?
}