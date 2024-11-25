package live.jkbx.zeroshare.nebula

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class Site(context: Context, siteDir: File) : KoinComponent {
    val name: String
    val id: String
    val staticHostmap: HashMap<String, StaticHosts>
    val unsafeRoutes: List<UnsafeRoute>
    var cert: CertificateInfo? = null
    var ca: Array<CertificateInfo>
    val lhDuration: Int
    val port: Int
    val mtu: Int
    val cipher: String
    val sortKey: Int
    val logVerbosity: String
    var connected: Boolean?
    var status: String?
    val logFile: String?
    var errors: ArrayList<String> = ArrayList()
    val managed: Boolean

    // Path to this site on disk
    @Transient
    val path: String

    // Strong representation of the site config
    @Transient
    val config: String

    private val json by inject<Json>()

    init {

        config = siteDir.resolve("config.json").readText()
        val incomingSite = json.decodeFromString<IncomingSite>(config)

        path = siteDir.absolutePath
        name = incomingSite.name
        id = incomingSite.id
        staticHostmap = incomingSite.staticHostmap
        unsafeRoutes = incomingSite.unsafeRoutes ?: ArrayList()
        lhDuration = incomingSite.lhDuration
        port = incomingSite.port
        mtu = incomingSite.mtu ?: 1300
        cipher = incomingSite.cipher
        sortKey = incomingSite.sortKey ?: 0
        logFile = siteDir.resolve("log").absolutePath
        logVerbosity = incomingSite.logVerbosity ?: "info"
//        rawConfig = incomingSite.rawConfig
        managed = incomingSite.managed ?: false
//        lastManagedUpdate = incomingSite.lastManagedUpdate

        connected = false
        status = "Disconnected"

        try {
            val rawDetails = mobileNebula.MobileNebula.parseCerts(incomingSite.cert)
            val certs = json.decodeFromString<Array<CertificateInfo>>(rawDetails)
            if (certs.isEmpty()) {
                throw IllegalArgumentException("No certificate found")
            }
            cert = certs[0]
            if (!cert!!.validity.valid) {
                errors.add("Certificate is invalid: ${cert!!.validity.reason}")
            }

        } catch (err: Exception) {
            errors.add("Error while loading certificate: ${err.message}")
        }

        try {
            val rawCa = mobileNebula.MobileNebula.parseCerts(incomingSite.ca)
            ca = json.decodeFromString<Array<CertificateInfo>>(rawCa)
            var hasErrors = false
            ca.forEach {
                if (!it.validity.valid) {
                    hasErrors = true
                }
            }

            if (hasErrors && !managed) {
                errors.add("There are issues with 1 or more ca certificates")
            }

        } catch (err: Exception) {
            ca = arrayOf()
            errors.add("Error while loading certificate authorities: ${err.message}")
        }

        if (errors.isEmpty()) {
            try {
                mobileNebula.MobileNebula.testConfig(config, getKey(context))
            } catch (err: Exception) {
                errors.add("Config test error: ${err.message}")
            }
        }
    }

    fun getKey(context: Context): String {
        val f = EncFile(context).openRead(File(path).resolve("key"))
        val k = f.readText()
        f.close()
        return k
    }
}

class SiteList(context: Context) {
    private var sites: Map<String, Site>

    init {
        val nebulaSites = getSites(context, context.filesDir)
        val dnSites = getSites(context, context.noBackupFilesDir)

        // In case of a conflict, dnSites will take precedence.
        sites = nebulaSites + dnSites
    }

    fun getSites(): Map<String, Site>  {
        return sites
    }

    companion object {
        fun getSites(context: Context, directory: File): HashMap<String, Site> {
            val sites = HashMap<String, Site>()

            val sitesDir = directory.resolve("sites")

            if (!sitesDir.isDirectory) {
                sitesDir.delete()
                sitesDir.mkdir()
            }

            sitesDir.listFiles()?.forEach { siteDir ->
                try {
                    val site = Site(context, siteDir)

                    // Make sure we can load the private key
                    site.getKey(context)

                    sites[site.id] = site
                } catch (err: Exception) {
                    siteDir.deleteRecursively()
                    Log.e("###", "Deleting non conforming site ${siteDir.absolutePath}", err)
                }
            }

            return sites
        }
    }
}