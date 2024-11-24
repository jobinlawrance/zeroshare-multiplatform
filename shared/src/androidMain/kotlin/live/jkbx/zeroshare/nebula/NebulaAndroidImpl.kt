package live.jkbx.zeroshare.nebula

import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class NebulaAndroidImpl: Nebula, KoinComponent {
    private val json by inject<Json>()

    override fun generateKeyPair(): Key {
        val kp = mobileNebula.MobileNebula.generateKeyPair()
        val key = json.decodeFromString<Key>(kp)
        return key
    }

    override fun parseCert(cert: String): Result<String> {
        try {
            val json = mobileNebula.MobileNebula.parseCerts(cert)
            return Result.success(json)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun verifyCertAndKey(cert: String, key: String): Result<Boolean> {
        try {
            val json = mobileNebula.MobileNebula.verifyCertAndKey(cert, key)
            return Result.success(json)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}