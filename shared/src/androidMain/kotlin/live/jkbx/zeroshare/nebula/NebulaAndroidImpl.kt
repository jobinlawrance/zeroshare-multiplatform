package live.jkbx.zeroshare.nebula

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File


class NebulaAndroidImpl : Nebula, KoinComponent {
    private val json by inject<Json>()

    override suspend fun generateKeyPair(): Key {
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

    @Composable
    override fun saveIncomingSite(incomingSite: IncomingSite): Result<File> {
        val baseDir = LocalContext.current.filesDir
        val siteDir = baseDir.resolve("sites").resolve(incomingSite.id)
        if (!siteDir.exists()) {
            siteDir.mkdirs()
        }

        if (incomingSite.key != null) {
            val keyFile = siteDir.resolve("key")
            keyFile.delete()
            val encFile = EncFile(LocalContext.current).openWrite(keyFile)
            encFile.use { it.write(incomingSite.key) }
            encFile.close()
        }
        incomingSite.key = null

        val confFile = siteDir.resolve("config.json")
        confFile.writeText(json.encodeToString(IncomingSite.serializer(), incomingSite))
        if (!validateOrDeleteSite(siteDir, LocalContext.current))
            return Result.failure(Exception("Invalid site"))

        if (LocalContext.current is NebulaActivityCallback) {
            val callback = LocalContext.current as NebulaActivityCallback
            callback.startSite(Site(LocalContext.current, siteDir))
        }

        return Result.success(siteDir)
    }

    private fun validateOrDeleteSite(siteDir: File, context: Context): Boolean {
        try {
            // Try to render a full site, if this fails the config was bad somehow
            Site(context, siteDir)
        } catch(err: java.io.FileNotFoundException) {
            Log.e("###", "Site not found at $siteDir")
            return false
        } catch(err: Exception) {
            Log.e("###", "Deleting site at $siteDir due to error: $err")
            siteDir.deleteRecursively()
            return false
        }
        return true
    }
}