package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.impl.DefaultFtpServerContext
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission

class SimpleFTPServer(private val log: Logger) {
    fun start() = CoroutineScope(Dispatchers.IO).launch {
        try {
            log.d { "Starting FTP server" }
            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()
            listenerFactory.port = 2221;  // Set port for FTP server

            serverFactory.addListener("default", listenerFactory.createListener())


            val userManager = serverFactory.userManager
            val user = BaseUser().apply {
                name = "ftpuser"
                password = "password"
                homeDirectory = getPublicDirectory()
                authorities = listOf(WritePermission())
            }
            // Save user into user manager
            userManager.save(user);

            // Set the user manager to the FTP server context
            serverFactory.userManager = userManager

            // Set the file system for the FTP server
            serverFactory.fileSystem = NativeFileSystemFactory()

            // Start the server
            val server = serverFactory.createServer()
            server.start()

            log.d { "FTP server started at ${getPublicDirectory()}" }
        } catch (e: Exception) {
            log.e(e) { "FTP server error: ${e.message}" }
        }
    }
}