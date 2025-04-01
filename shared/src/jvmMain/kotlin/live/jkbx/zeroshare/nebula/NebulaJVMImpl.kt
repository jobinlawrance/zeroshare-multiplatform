package live.jkbx.zeroshare.nebula

import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.nebulaSetupKey
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class NebulaJVMImpl(baseUrl: String) : Nebula, KoinComponent {

    private val log by injectLogger("Nebula")
    private val settings by inject<Settings>()


    private val nebulaDownloader = NebulaDownloader.getDownloader(baseUrl)

    override suspend fun generateKeyPair(
        messages: (String) -> Unit,
        downloadProgress: (Int) -> Unit
    ): Key {
        checkAndInstallNebula(messages, downloadProgress)

        messages("Generating Key Pair with Nebula Cert")
        val tempDir = withContext(Dispatchers.IO) {
            Files.createTempDirectory("nebula-cert-")
        }
        var publicKey = ""
        var privateKey = ""
        try {
            // Run the nebula-cert keygen command
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder(
                    "nebula-cert",
                    "keygen",
                    "-out-key", tempDir.resolve("host.key").toString(),
                    "-out-pub", tempDir.resolve("host.pub").toString()
                ).directory(tempDir.toFile()) // Ensure command runs in the temp directory
                    .redirectErrorStream(true) // Combine error stream with output stream
                    .start()
            }

            // Wait for the process to finish
            val exitCode = withContext(Dispatchers.IO) {
                process.waitFor()
            }
            if (exitCode != 0) {
                val errorOutput = process.inputStream.bufferedReader().readText()
                throw RuntimeException("nebula-cert command failed with exit code $exitCode: $errorOutput")
            }

            // Read the generated files
            privateKey = tempDir.resolve("host.key").readText()
            publicKey = tempDir.resolve("host.pub").readText()
        } finally {
            // Clean up the temp directory and its contents
            withContext(Dispatchers.IO) {
                Files.walk(tempDir)
            }
                .sorted(Comparator.reverseOrder()) // Delete files before directories
                .forEach(Files::deleteIfExists)
        }

        messages("Generated Keys Successfully")
        return Key(publicKey, privateKey)
    }

    private suspend fun checkAndInstallNebula(
        messages: (String) -> Unit,
        downloadProgress: (Int) -> Unit
    ) {
        if (nebulaDownloader.checkNebulaExists()) {
            // we already have nebula
            log.d { "Nebula already exists" }
            messages("Nebula is installed at ${nebulaDownloader.getNebulaInstallPath()}")
        } else {
            nebulaDownloader.downloadAndInstallNebula(messages, downloadProgress)
        }
    }

    override fun parseCert(cert: String): Result<String> {

        return Result.success("NO_OP in JVM")
    }

    override fun verifyCertAndKey(cert: String, key: String): Result<Boolean> {
        return Result.success(true)
    }


    @Composable
    override fun saveIncomingSite(incomingSite: IncomingSite, messages: (String) -> Unit): Any {

        log.d { "Saving Incoming Site" }
        messages("Saving incoming site")


        val lightHouseIp = incomingSite.staticHostmap.keys.first()
        val staticHostMap = incomingSite.staticHostmap.get(lightHouseIp)!!.destinations.first()

        messages("Nebula Lighthouse \uD83D\uDCA1 - $lightHouseIp")

        val configDirPath = nebulaDownloader.configDirPath

        val configContent = """
        # PKI paths
        pki:
          ca: ${configDirPath}ca.crt
          cert: ${configDirPath}host.crt
          key: ${configDirPath}host.key

        # Need a static host map, using the DNS name of the lighthouse
        static_host_map:
          # Put all of your lighthouses here
          '$lightHouseIp': ['$staticHostMap']

        # This is completely undocumented
        # static_map is how to interpret static_host_map
        # It defaults to ip4, trying to connect to the lighthouse
        # using only ipv4. This sorta-kinda makes sense since the node
        # knows its own public v6 already but not its public v4 (Via NAT)
        # so connecting to the lighthouse via v4 lets it learn that
        # For ipv6-only hosts, change to `ip6` instead
        static_map:
          network: ip4

        # Lighthouse config for clients
        lighthouse:
          hosts:
            - '$lightHouseIp'

        # Listen
        listen:
          # Default for this key is 0.0.0.0 which is v4-only and stupid
          host: '[::]'
          # Port of 0 means randomly choose, usually good for clients
          # Want to set to 4242 for relays and lighthouses
          port: 0
        punchy:
          punch: true
          delay: 1s
          respond: true
          respond_delay: 5s

        # Firewall settings
        firewall:
          outbound:
            # Allow all outbound traffic from this node
            - port: any
              proto: any
              host: any

          inbound:
            # Allow icmp between any nebula hosts
            - port: any
              proto: any
              host: any
    """.trimIndent()

        val serviceText = """
            [Unit]
            Description=Nebula Service
            Wants=basic.target network-online.target nss-lookup.target time-sync.target
            After=network.target

            [Service]
            SyslogIdentifier=nebula
            ExecStart=/usr/local/bin/nebula -config /etc/zeroshare/config.yml
            Restart=on-always
            User=root

            [Install]
            WantedBy=multi-user.target
        """.trimIndent()



        val tempDir = Files.createTempDirectory("nebula-config-")

        val configFile = tempDir.resolve("config.yml")
        val caFile = tempDir.resolve("ca.crt")
        val hostFile = tempDir.resolve("host.crt")
        val hostKey = tempDir.resolve("host.key")

        // Write the configuration content to the file
        Files.writeString(configFile, configContent)
        Files.writeString(caFile, incomingSite.ca)
        Files.writeString(hostFile, incomingSite.cert)
        Files.writeString(hostKey, incomingSite.key)

        log.d { "Files are created at ${tempDir.absolutePathString()}" }

        val result = nebulaDownloader.startNebulaAsBackgroundTask(tempDir)

        messages(result)
        settings.set(nebulaSetupKey, true)
        return Result.success("")
    }
}

@Serializable
data class GithubReleaseResponse(
    val name: String,
    val assets: List<GithubAssets>
)

@Serializable
data class GithubAssets(
    val id: Long,
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)


interface RetrofitBackendApi {

    @GET
    @Streaming
    suspend fun downloadAsset(@Url url: String): ResponseBody
}

