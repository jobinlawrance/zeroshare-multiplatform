package live.jkbx.zeroshare

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Configuration
import androidx.work.WorkManager
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import live.jkbx.zeroshare.di.initKoin
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.nebula.NebulaAndroidImpl
import live.jkbx.zeroshare.socket.FileTransfer
import live.jkbx.zeroshare.socket.JavaSocketFileTransfer
import live.jkbx.zeroshare.socket.KmpHashing
import live.jkbx.zeroshare.socket.KmpHashingAndroidImpl
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.internal.applyConnectionSpec
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import io.ktor.client.plugins.api.*

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // In order to use the WorkManager from the nebulaVpnBg process (i.e. NebulaVpnService)
        // we must explicitly initialize this rather than using the default initializer.
        val myConfig = Configuration.Builder().build()
        WorkManager.initialize(this, myConfig)

        initKoin(appModule = module {
            single<Context> { this@MainApplication }
            single<SharedPreferences> {
                get<Context>().getSharedPreferences(
                    "ZeroShare_SETTINGS",
                    Context.MODE_PRIVATE
                )
            }
            single<Settings> {
                SharedPreferencesSettings(get<SharedPreferences>())
            }
            single<HttpClientEngine> {
                OkHttp.create {
                    OkHttpConfig().config {
                        readTimeout(0, TimeUnit.MINUTES)
                        writeTimeout(0, TimeUnit.MINUTES)
                        connectTimeout(0, TimeUnit.MINUTES)
                        pingInterval(20, TimeUnit.SECONDS)
                    }
                }
//                CIO.create()
            }
            single<String>(named("serverId")) { getString(live.jkbx.zeroshare.shared.R.string.serverId) }
            factoryOf(::ZeroTierPeerImpl) { bind<ZeroTierPeer>() }
            factoryOf(::NebulaAndroidImpl) { bind<Nebula>() }
            single<KmpHashing> { KmpHashingAndroidImpl() }
        })

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = true,
            )
        )
    }
}