package live.jkbx.zeroshare.nebula

import android.content.Context
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject



class InvalidCredentialsException: Exception("Invalid credentials")

class APIClient(context: Context) {
    private val json by inject<Json>(Json::class.java)

    private val packageInfo = PackageInfo(context)
    private val client = mobileNebula.MobileNebula.newAPIClient(
        "MobileNebula/%s (Android %s)".format(
                packageInfo.getVersion(),
                packageInfo.getSystemVersion(),
        ))

    fun enroll(code: String): IncomingSite {
        val res = client.enroll(code)
        return decodeIncomingSite(res.site)
    }

    fun tryUpdate(siteName: String, hostID: String, privateKey: String, counter: Long, trustedKeys: String): IncomingSite? {
        val res: mobileNebula.TryUpdateResult
        try {
            res = client.tryUpdate(siteName, hostID, privateKey, counter, trustedKeys)
        } catch (e: Exception) {
            // type information from Go is not available, use string matching instead
            if (e.message == "invalid credentials") {
                throw InvalidCredentialsException()
            }

            throw e
        }

        if (res.fetchedUpdate) {
            return decodeIncomingSite(res.site)
        }

        return null
    }

    private fun decodeIncomingSite(jsonSite: String): IncomingSite {
        return json.decodeFromString(IncomingSite.serializer(), jsonSite)
    }
}