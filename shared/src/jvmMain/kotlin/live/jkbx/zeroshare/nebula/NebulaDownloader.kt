package live.jkbx.zeroshare.nebula

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

abstract class NebulaDownloader(baseUrl: String) : KoinComponent {

    val log by injectLogger("Nebula")
    private val client by inject<HttpClient>()
    private val json by inject<Json>()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .addNetworkInterceptor(HttpLoggingInterceptor())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(baseUrl)
        .addConverterFactory(
            json.asConverterFactory("application/json; charset=UTF8".toMediaType())
        )
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build()

    private val api = retrofit.create<RetrofitBackendApi>()

    val os = detectOS()
    val arch = detectArchitecture()

    companion object {
        fun getDownloader(baseUrl: String): NebulaDownloader {
            val osName: String = detectOS()
            println("OS is $osName")
            return when {
                osName.contains("mac") || osName.contains("darwin") -> NebulaDownloaderDarwin(baseUrl)
                osName.contains("win") -> NebulaDownloaderWindows(baseUrl)
                osName.contains("nix") || osName.contains("nux") -> NebulaDownloaderLinux(baseUrl)
                else -> throw UnsupportedOperationException("Unsupported OS: $osName")
            }
        }
    }

    abstract val configDirPath: String

    abstract suspend fun checkNebulaExists(): Boolean
    abstract suspend fun downloadAndInstallNebula(
        messages: (String) -> Unit,
        downloadProgress: (Int) -> Unit
    )

    abstract fun runWithAdminPrivileges(command: String): String
    abstract fun getNebulaInstallPath(): String
    abstract fun startNebulaAsBackgroundTask(tempDir: Path): String
    abstract fun runCommand(command: String): String

    fun extractAsset(file: File, destinationDir: String) {
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

    suspend fun downloadAssetForPlatform(
        messages: (String) -> Unit,
        downloadProgress: (Int) -> Unit
    ): File? {

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
            return null
        }

        // If only one asset matches the OS, use it; otherwise, filter by architecture
        val asset = if (osAssets.size == 1) {
            osAssets.first()
        } else {
            osAssets.find { it.name.contains(arch, ignoreCase = true) }
                ?: run {
                    log.d { "No matching asset found for OS: $os and Architecture: $arch" }
                    return null
                }
        }

        log.d { "Downloading ${asset.name}" }
        messages("Downloading ${asset.name}")

        return downloadFile(asset.browserDownloadUrl, asset.name, downloadProgress)
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

sealed class DownloadState {
    data class Downloading(val progress: Int) : DownloadState()
    object Finished : DownloadState()
    data class Failed(val error: Throwable) : DownloadState()
}

fun ResponseBody.saveFile(destinationFile: File): Flow<DownloadState> {
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