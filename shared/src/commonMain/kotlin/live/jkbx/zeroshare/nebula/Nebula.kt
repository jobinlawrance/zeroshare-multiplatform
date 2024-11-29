package live.jkbx.zeroshare.nebula

import androidx.compose.runtime.Composable

interface Nebula {
    suspend fun generateKeyPair(): Key
    fun parseCert(cert: String): Result<String>
    fun verifyCertAndKey(cert: String, key: String): Result<Boolean>
    @Composable fun saveIncomingSite(incomingSite: IncomingSite): Any
}

