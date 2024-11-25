package live.jkbx.zeroshare

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.vinceglb.filekit.core.FileKit
import live.jkbx.zeroshare.nebula.NebulaActivityCallback
import live.jkbx.zeroshare.nebula.Site
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit

const val UPDATE_WORKER = "dnUpdater"
const val VPN_START_CODE = 0x10

class MainActivity : ComponentActivity(), KoinComponent, NebulaActivityCallback {

    companion object {
        const val ACTION_REFRESH_SITES = "net.defined.mobileNebula.REFRESH_SITES"
    }

    private lateinit var workManager: WorkManager
    private var outMessenger: Messenger? = null
    // When starting a site we may need to request VPN permissions. These variables help us
    // maintain state while waiting for a permission result.
    private var inMessenger: Messenger? = Messenger(IncomingHandler())

    lateinit var site: Site

    override fun onCreate(savedInstanceState: Bundle?) {
        workManager = WorkManager.getInstance(application)
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()
        FileKit.init(this)

        enqueueDNUpdater()
    }

    override fun startSite(site: Site) {
        this.site = site
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_START_CODE)
        } else {
            onActivityResult(VPN_START_CODE, Activity.RESULT_OK, null)
        }
    }

    private fun enqueueDNUpdater() {
        val workRequest = PeriodicWorkRequestBuilder<DNUpdateWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            UPDATE_WORKER,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // This is where activity results come back to us (startActivityForResult)
        if (requestCode == VPN_START_CODE) {
            // If we are processing a result for VPN permissions and don't get them, let the UI know

            if (resultCode != Activity.RESULT_OK) {
                // The user did not grant permissions

//                return result.error("permissions", "Please grant VPN permissions to the app when requested. (If another VPN is running, please disable it now.)", null)
            }

            // Start the VPN service
            val intent = Intent(this, NebulaVpnService::class.java).apply {
                putExtra("path", site.path)
                putExtra("id", site.id)
            }
            startService(intent)
            if (outMessenger == null) {
                bindService(intent, connection, 0)
            }

//            return result.success(null)
        }

        // The file picker needs us to super
        super.onActivityResult(requestCode, resultCode, data)
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            outMessenger = Messenger(service)

            // We want to monitor the service for as long as we are connected to it.
            try {
                val msg = Message.obtain(null, NebulaVpnService.MSG_REGISTER_CLIENT)
                msg.replyTo = inMessenger
                outMessenger!!.send(msg)

            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
                //TODO:
            }

            val msg = Message.obtain(null, NebulaVpnService.MSG_IS_RUNNING)
            outMessenger!!.send(msg)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            outMessenger = null
            if (site.id != null) {
                //TODO: this indicates the service died, notify that it is disconnected
            }
//            activeSiteId = null
        }
    }

    inner class IncomingHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val id = msg.data.getString("id")

            //TODO: If the elvis hits then we had a deleted site running, which shouldn't happen
            if (!::site.isInitialized) return

            when (msg.what) {
                NebulaVpnService.MSG_IS_RUNNING -> isRunning(site, msg)
                NebulaVpnService.MSG_EXIT -> serviceExited(site, msg)
                else -> super.handleMessage(msg)
            }
        }

        private fun isRunning(site: Site, msg: Message) {
            var status = "Disconnected"
            var connected = false

            if (msg.arg1 == 1) {
                status = "Connected"
                connected = true
            }


        }

        private fun serviceExited(site: Site, msg: Message) {
//            activeSiteId = null
//            site.updater.setState(false, "Disconnected", msg.data.getString("error"))
            unbindVpnService()
        }
    }

    private fun unbindVpnService() {
        if (outMessenger != null) {
            // Unregister ourselves
            val msg = Message.obtain(null, NebulaVpnService.MSG_UNREGISTER_CLIENT)
            msg.replyTo = inMessenger
            outMessenger!!.send(msg)
            // Unbind
            unbindService(connection)
        }
        outMessenger = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
//    App(log)
}