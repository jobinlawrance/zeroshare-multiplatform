package live.jkbx.zeroshare.nebula

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import live.jkbx.zeroshare.di.injectLogger
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import kotlin.io.path.absolutePathString

class NebulaJVMImpl : Nebula, KoinComponent {

    private val log by injectLogger("Nebula")
    private val client by inject<HttpClient>()

    override suspend fun generateKeyPair(): Key {
        checkAndInstallNebula()

        return Key("", "")
    }

    private suspend fun checkAndInstallNebula() {
        val nebulaExists = runCommand("which nebula")
        log.d { nebulaExists }
        if (nebulaExists.contains("/usr/local/bin/nebula")) {
            // we already have nebula
            log.d { "Nebula already exists" }
        } else {
            downloadNebula()
        }
    }

    override fun parseCert(cert: String): Result<String> {

        return Result.success("")
    }

    override fun verifyCertAndKey(cert: String, key: String): Result<Boolean> {
        return Result.success(true)
    }

    @Composable
    override fun saveIncomingSite(incomingSite: IncomingSite): Any {
        return Result.success("")
    }

    private suspend fun downloadNebula() {
        val os = detectOS()
        val arch = detectArchitecture()

        // Get Latest Release from Github API
        val githubApi = "https://api.github.com/repos/slackhq/nebula/releases/latest"
        val request = client.get(githubApi) {
            header(HttpHeaders.ContentType, "application/json")
        }
        val release = request.body<GithubReleaseResponse>()
        log.d { "Latest Release is ${release.name}" }

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

        val file = downloadFile(client, asset.browserDownloadUrl, asset.name)

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

    private suspend fun downloadFile(client: HttpClient, url: String, fileName: String): File {
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("nebula", fileName)
        }

        try {
            val httpResponse: HttpResponse = client.get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    log.d { "Progress - $bytesSentTotal and $contentLength" }
                }
            }
            val responseBody: ByteArray = httpResponse.body()
            tempFile.writeBytes(responseBody)
            log.d { ("Downloaded file to: ${tempFile.absolutePath} (Size: ${tempFile.length()} bytes)") }

            if (tempFile.length() == 0L) {
                throw IOException("Downloaded file is empty: ${tempFile.absolutePath}")
            }

            return tempFile
        } catch (e: Exception) {
            tempFile.delete() // Clean up on failure
            throw IOException("Error downloading file from $url", e)
        }
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
