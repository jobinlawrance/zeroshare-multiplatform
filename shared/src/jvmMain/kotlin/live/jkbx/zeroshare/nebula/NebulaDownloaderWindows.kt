package live.jkbx.zeroshare.nebula

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class NebulaDownloaderWindows(baseUrl: String) : NebulaDownloader(baseUrl) {
    override val configDirPath: String = "C:\\ProgramData\\nebula\\"

    override suspend fun checkNebulaExists(): Boolean {
        return runCommand("where nebula.exe").isNotEmpty()
    }

    override suspend fun downloadAndInstallNebula(messages: (String) -> Unit, downloadProgress: (Int) -> Unit) {
        val downloadedFile = downloadAssetForPlatform(messages, downloadProgress)
        if (downloadedFile != null) {
            messages("Downloaded the file")

            val tempFileDir = withContext(Dispatchers.IO) {
                Files.createTempDirectory("tmpDir")
            }
            extractAsset(downloadedFile, tempFileDir.toString())
            val mvResult = runWithAdminPrivileges(
                "xcopy /E /I \"${tempFileDir.absolutePathString()}\\*\" \"C:\\Program Files\\Nebula\\\""
            )
            log.d { "Copy Result $mvResult" }
            runCommand("rmdir /S /Q \"${tempFileDir.absolutePathString()}\"")
            runCommand("del \"${downloadedFile.absolutePath}\"")
        }
    }

    override fun runWithAdminPrivileges(command: String): String {
        val script = """
            Set objShell = CreateObject("Shell.Application")
            objShell.ShellExecute "cmd.exe", "/c $command", "", "runas", 1
        """.trimIndent()

        val vbsFile = File.createTempFile("elevate", ".vbs")
        vbsFile.writeText(script)
        
        val process = ProcessBuilder("cscript", "//Nologo", vbsFile.absolutePath)
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
        vbsFile.delete()
        return output.toString()
    }

    override fun getNebulaInstallPath(): String {
        return "C:\\Program Files\\Nebula\\nebula.exe"
    }

    override fun startNebulaAsBackgroundTask(tempDir: Path): String {
        val serviceScript = """
            @echo off
            sc create "Nebula" binPath= "${getNebulaInstallPath()} -config ${configDirPath}config.yml"
            sc description "Nebula" "Nebula VPN Service"
            sc config "Nebula" start= auto
            sc start "Nebula"
        """.trimIndent()

        val scriptFile = tempDir.resolve("install-service.bat")
        Files.writeString(scriptFile, serviceScript)

        return runWithAdminPrivileges(
            "mkdir \"$configDirPath\" && " +
            "copy \"${tempDir}\\ca.crt\" \"$configDirPath\" && " +
            "copy \"${tempDir}\\host.crt\" \"$configDirPath\" && " +
            "copy \"${tempDir}\\host.key\" \"$configDirPath\" && " +
            "copy \"${tempDir}\\config.yml\" \"$configDirPath\" && " +
            "\"${scriptFile.absolutePathString()}\""
        )
    }

    override fun runCommand(command: String): String {
        val process = ProcessBuilder("cmd.exe", "/c", command)
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
