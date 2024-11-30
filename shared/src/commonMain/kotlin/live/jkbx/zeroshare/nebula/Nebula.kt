package live.jkbx.zeroshare.nebula

import androidx.compose.runtime.Composable
import live.jkbx.zeroshare.models.SignedKeyResponse

interface Nebula {
    suspend fun generateKeyPair(messages: (String) -> Unit = {}, downloadProgress: (Int) -> Unit = {}): Key
    fun parseCert(cert: String): Result<String>
    fun verifyCertAndKey(cert: String, key: String): Result<Boolean>
    @Composable fun saveIncomingSite(incomingSite: IncomingSite, messages: (String) -> Unit): Any
    suspend fun signCertificate(publicKey: String): SignedKeyResponse
}

