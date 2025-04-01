package live.jkbx.zeroshare.nebula

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class NebulaDownloaderLinux(baseUrl: String) : NebulaDownloader(baseUrl) {
    override val configDirPath: String = "/etc/nebula/"

    override suspend fun checkNebulaExists(): Boolean {
        return runCommand("which nebula").isNotEmpty()
    }

    override suspend fun downloadAndInstallNebula(messages: (String) -> Unit, downloadProgress: (Int) -> Unit) {
        val downloadedFile = downloadAssetForPlatform(messages, downloadProgress)
        if (downloadedFile != null) {
            messages("Downloaded the file")

            val tempFileDir = withContext(Dispatchers.IO) {
                Files.createTempDirectory("tmpDir")
            }
            extractAsset(downloadedFile, tempFileDir.toString())
            withContext(Dispatchers.IO) {
                Files.walk(tempFileDir)
            }.forEach {
                runCommand("chmod +x ${it.absolutePathString()}")
            }
            val mvResult = runWithAdminPrivileges("cp -r $tempFileDir/* /usr/local/bin/")
            log.d { "Copy Result $mvResult" }
            runCommand("rm -rf ${tempFileDir.absolutePathString()}")
            runCommand("rm ${downloadedFile.absolutePath}")
        }
    }

    override fun runWithAdminPrivileges(command: String): String {
        val process = ProcessBuilder("pkexec", "sh", "-c", command)
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

    override fun getNebulaInstallPath(): String {
        return runCommand("which nebula").trim()
    }

    override fun startNebulaAsBackgroundTask(tempDir: Path): String {
        val serviceText = """
            [Unit]
            Description=Nebula Service
            Wants=basic.target network-online.target nss-lookup.target time-sync.target
            After=network.target

            [Service]
            SyslogIdentifier=nebula
            ExecStart=/usr/local/bin/nebula -config ${configDirPath}config.yml
            Restart=always
            User=root

            [Install]
            WantedBy=multi-user.target
        """.trimIndent()

        val serviceFile = tempDir.resolve("nebula.service")
        Files.writeString(serviceFile, serviceText)

        return runWithAdminPrivileges(
            "mkdir -p $configDirPath && " +
            "cp ${tempDir}/ca.crt $configDirPath && " +
            "cp ${tempDir}/host.crt $configDirPath && " +
            "cp ${tempDir}/host.key $configDirPath && " +
            "cp ${tempDir}/config.yml $configDirPath && " +
            "cp ${tempDir}/nebula.service /etc/systemd/system/ && " +
            "systemctl daemon-reload && " +
            "systemctl enable nebula.service && " +
            "systemctl start nebula.service && " +
            "journalctl -u nebula.service"
        )
    }

    override fun runCommand(command: String): String {
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
}