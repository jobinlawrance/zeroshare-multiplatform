package live.jkbx.zeroshare.nebula

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class NebulaDownloaderDarwin(baseUrl: String) : NebulaDownloader(baseUrl) {

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
        val script = """
        do shell script "$command" with administrator privileges
    """.trimIndent()

        val process =
            ProcessBuilder("osascript", "-e", script)
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
                    <string>${configDirPath}config.yml</string>
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
        val launchFile = tempDir.resolve("com.nebula.plist")
        Files.writeString(launchFile, launchText)
        return runWithAdminPrivileges(
            "mkdir -p $configDirPath" +
                    "&& cp $tempDir/ca.crt $configDirPath " +
                    "&& cp $tempDir/host.crt $configDirPath " +
                    "&& cp $tempDir/host.key $configDirPath " +
                    "&& cp $tempDir/config.yml $configDirPath " +
                    "&& sudo cp $tempDir/com.nebula.plist /Library/LaunchDaemons/" +
                    "&& sudo launchctl unload /Library/LaunchDaemons/com.nebula.plist" +
                    "&& sudo launchctl load /Library/LaunchDaemons/com.nebula.plist" +
                    "&& sudo launchctl start com.nebula"
                    + "&& sudo cat /var/log/nebula.log"
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