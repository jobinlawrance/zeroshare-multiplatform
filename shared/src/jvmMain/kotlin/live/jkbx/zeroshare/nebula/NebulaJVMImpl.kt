package live.jkbx.zeroshare.nebula

import androidx.compose.runtime.Composable
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.nebulaSetupKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SignedKeyResponse
import live.jkbx.zeroshare.utils.uniqueDeviceId
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url
import rx.Observable
import rx.schedulers.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class NebulaJVMImpl : Nebula, KoinComponent {

    private val log by injectLogger("Nebula")
    private val client by inject<HttpClient>()
    private val settings by inject<Settings>()
    private val json by inject<Json>()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .addNetworkInterceptor(HttpLoggingInterceptor())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl("http://localhost:4000")
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()

    private val api = retrofit.create<RetrofitBackendApi>()

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
        val nebulaExists = runCommand("which nebula")
        log.d { nebulaExists }
        if (nebulaExists.contains("/usr/local/bin/nebula")) {
            // we already have nebula
            log.d { "Nebula already exists" }
            messages("Nebula is installed at $nebulaExists")
        } else {
            downloadNebula(messages, downloadProgress)
        }
    }

    override fun parseCert(cert: String): Result<String> {

        return Result.success("NO_OP in JVM")
    }

    override fun verifyCertAndKey(cert: String, key: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun signCertificate(publicKey: String): SignedKeyResponse {

        val token = settings.get<String>(tokenKey)
        return api.signPublicKey(
            SignPublicKeyRequest(publicKey, uniqueDeviceId()), authToken = "Bearer $token"
        )
            .subscribeOn(Schedulers.io())
            .toBlocking()
            .first()
    }

    @Composable
    override fun saveIncomingSite(incomingSite: IncomingSite, messages: (String) -> Unit): Any {

        log.d { "Saving Incoming Site" }
        messages("Saving incoming site")


        val lightHouseIp = incomingSite.staticHostmap.keys.first()
        val staticHostMap = incomingSite.staticHostmap.get(lightHouseIp)!!.destinations.first()

        messages("Nebula Lighthouse \uD83D\uDCA1 - $lightHouseIp")

        val configContent = """
        # PKI paths
        pki:
          ca: /etc/zeroshare/ca.crt
          cert: /etc/zeroshare/host.crt
          key: /etc/zeroshare/host.key

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

        val launchText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.nebula</string>
                <key>ProgramArguments</key>
                <array>
                    <string>/usr/local/bin/nebula</string>
                    <string>-config</string>
                    <string>/etc/zeroshare/config.yml</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>KeepAlive</key>
                <true/>
                <key>StandardErrorPath</key>
                <string>/var/log/nebula.err</string>
                <key>StandardOutPath</key>
                <string>/var/log/nebula.log</string>
            </dict>
            </plist>
        """.trimIndent()

        val tempDir = Files.createTempDirectory("nebula-config-")

        val configFile = tempDir.resolve("config.yml")
        val caFile = tempDir.resolve("ca.crt")
        val hostFile = tempDir.resolve("host.crt")
        val hostKey = tempDir.resolve("host.key")
//        val serviceFile = tempDir.resolve("nebula.service")
        val launchFile = tempDir.resolve("com.nebula.plist")

        // Write the configuration content to the file
        Files.writeString(configFile, configContent)
        Files.writeString(caFile, incomingSite.ca)
        Files.writeString(hostFile, incomingSite.cert)
        Files.writeString(hostKey, incomingSite.key)
//        Files.writeString(serviceFile, serviceText)
        Files.writeString(launchFile, launchText)

        log.d { "Files are created at ${tempDir.absolutePathString()}" }

        val result = runWithAdminPrivilegesMac(
            "mkdir -p /etc/zeroshare " +
                    "&& cp $tempDir/ca.crt /etc/zeroshare/ " +
                    "&& cp $tempDir/host.crt /etc/zeroshare/ " +
                    "&& cp $tempDir/host.key /etc/zeroshare/ " +
                    "&& cp $tempDir/config.yml /etc/zeroshare/ " +
//                    "&& cp $tempDir/nebula.service /etc/systemd/system/" +
                    "&& sudo cp $tempDir/com.nebula.plist /Library/LaunchDaemons/" +
                    "&& sudo route delete 69.69.0.0/8" +
//                    "sudo systemctl daemon-reload" +
//                    "sudo systemctl enable nebula.service" +
//                    "sudo systemctl start nebula.service" +
                    "&& sudo launchctl unload /Library/LaunchDaemons/com.nebula.plist" +
                    "&& sudo launchctl load /Library/LaunchDaemons/com.nebula.plist" +
                    "&& sudo launchctl start com.nebula"
                   +  "&& sudo cat /var/log/nebula.log"
        )

        messages(result)
        settings.set(nebulaSetupKey, true)
        return Result.success("")
    }

    private suspend fun downloadNebula(
        messages: (String) -> Unit,
        downloadProgress: (Int) -> Unit
    ) {

        val os = detectOS()
        val arch = detectArchitecture()

        messages("Fetching latest release from Github")
        // Get Latest Release from Github API
        val githubApi = "https://api.github.com/repos/slackhq/nebula/releases/latest"
        val request = client.get(githubApi) {
            header(HttpHeaders.ContentType, "application/json")
        }
        val release = request.body<GithubReleaseResponse>()
        log.d { "Latest Release is ${release.name}" }
        messages("Latest Release is ${release.name}")

        // Filter assets by OS name first
        val osAssets = release.assets.filter { it.name.contains(os, ignoreCase = true) }

        if (osAssets.isEmpty()) {
            log.d { "No matching asset found for OS: $os" }
            return
        }

        // If only one asset matches the OS, use it; otherwise, filter by architecture
        val asset = if (osAssets.size == 1) {
            osAssets.first()
        } else {
            osAssets.find { it.name.contains(arch, ignoreCase = true) }
                ?: run {
                    log.d { "No matching asset found for OS: $os and Architecture: $arch" }
                    return
                }
        }

        log.d { "Downloading ${asset.name}" }
        messages("Downloading ${asset.name}")

        val file = downloadFile(asset.browserDownloadUrl, asset.name, downloadProgress)

        messages("Downloaded the file")

        val tempFileDir = withContext(Dispatchers.IO) {
            Files.createTempDirectory("tmpDir")
        }

        extractAndPlaceBinary(file, tempFileDir.absolutePathString())

        withContext(Dispatchers.IO) {
            Files.walk(tempFileDir)
        }.forEach {
            runCommand("chmod +x ${it.absolutePathString()}")
        }

        val mvResult = runWithAdminPrivilegesMac("cp -r $tempFileDir/* /usr/local/bin/")
        log.d { "Copy Result $mvResult" }

        runCommand("rm -rf ${tempFileDir.absolutePathString()}")
        runCommand("rm ${file.absolutePath}")
    }

    private suspend fun downloadFile(
        url: String,
        fileName: String,
        downloadProgress: (Int) -> Unit
    ): File {
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("nebula", fileName)
        }

        val responseBody = withContext(Dispatchers.IO) {
            api.downloadAsset(url)
        }

        // Use CompletableDeferred to signal completion
        val fileDeferred = CompletableDeferred<File>()

        responseBody.saveFile(tempFile).collect { downloadState ->
            when (downloadState) {
                is DownloadState.Downloading -> {
                    downloadProgress(downloadState.progress)
                }

                is DownloadState.Failed -> {
                    fileDeferred.completeExceptionally(downloadState.error)
                }

                DownloadState.Finished -> {
                    fileDeferred.complete(tempFile)
                }
            }
        }
        return fileDeferred.await()
    }

    private fun extractAndPlaceBinary(file: File, destinationDir: String) {
        if (file.name.endsWith(".tar.gz", ignoreCase = true)) {
            log.d { "Extracting tar.gz file: ${file.name}" }
            extractTarGz(file, destinationDir)
        } else if (file.name.endsWith(".zip", ignoreCase = true)) {
            log.d { "Extracting zip file: ${file.name}" }
            extractZip(file, destinationDir)
        } else {
            throw IOException("Unsupported file format: ${file.name}")
        }
    }

    private fun extractTarGz(file: File, destinationDir: String) {
        FileInputStream(file).use { fis ->
            GZIPInputStream(fis).use { gis ->
                TarArchiveInputStream(gis).use { tar ->
                    var entry: TarArchiveEntry?
                    while (tar.nextEntry.also { entry = it } != null) {
                        entry?.let {
                            val outputFile = File(destinationDir, it.name)
                            if (it.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.parentFile.mkdirs()
                                FileOutputStream(outputFile).use { fos ->
                                    tar.copyTo(fos)
                                }
                            }
                            log.d { "Extracted: ${outputFile.absolutePath}" }
                        }
                    }
                }
            }
        }
    }

    private fun extractZip(file: File, destinationDir: String) {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("The file ${file.name} does not exist or is empty.")
        }

        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outputFile = File(destinationDir, entry.name)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                log.d { "Extracted: ${outputFile.absolutePath}" }
            }
        }
    }

    private fun runWithAdminPrivilegesMac(command: String): String {
        val script = """
        do shell script "$command" with administrator privileges
    """.trimIndent()

        val process = ProcessBuilder("osascript", "-e", script)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
        }

        process.waitFor()
        return output.toString()
    }

    private fun runCommand(command: String): String {
        val process = ProcessBuilder("/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
        }

        process.waitFor()
        return output.toString()
    }

    private fun detectOS(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            osName.contains("win") -> "windows"
            osName.contains("nix") || osName.contains("nux") -> "linux"
            else -> throw UnsupportedOperationException("Unsupported OS: $osName")
        }
    }

    private fun detectArchitecture(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            arch.contains("amd64") || arch.contains("x86_64") -> "amd64"
            arch.contains("arm64") || arch.contains("aarch64") -> "arm64"
            arch.contains("arm") -> "arm" // Generic ARM, may need refinement for variants like arm7
            arch.contains("386") || arch.contains("x86") -> "386"
            arch.contains("ppc64le") -> "ppc64le"
            arch.contains("mips64") -> "mips64"
            arch.contains("riscv64") -> "riscv64"
            else -> throw UnsupportedOperationException("Unsupported Architecture: $arch")
        }
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

@Serializable
data class SignPublicKeyRequest(
    val public_key: String,
    val device_id: String
)

interface RetrofitBackendApi {

    @POST("nebula/sign-public-key")
    fun signPublicKey(
        @Body request: SignPublicKeyRequest,
        @Header("Authorization") authToken: String
    ): Observable<SignedKeyResponse>

    @GET
    @Streaming
    suspend fun downloadAsset(@Url url: String): ResponseBody
}

private fun ResponseBody.saveFile(destinationFile: File): Flow<DownloadState> {
    return flow {
        emit(DownloadState.Downloading(0))
        try {
            byteStream().use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    val totalBytes = contentLength()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var progressBytes = 0L
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        progressBytes += bytes
                        bytes = inputStream.read(buffer)
                        emit(DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt()))
                    }
                }
            }
            emit(DownloadState.Finished)
        } catch (e: Exception) {
            emit(DownloadState.Failed(e))
        }
    }
        .flowOn(Dispatchers.IO).distinctUntilChanged()
}


private sealed class DownloadState {
    data class Downloading(val progress: Int) : DownloadState()
    object Finished : DownloadState()
    data class Failed(val error: Throwable) : DownloadState()
}