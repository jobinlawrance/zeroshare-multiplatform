package live.jkbx.zeroshare

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import live.jkbx.zeroshare.di.initKoin
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.nebula.NebulaAndroidImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
                OkHttp.create()
            }
            single<String>(named("serverId")) { getString(live.jkbx.zeroshare.shared.R.string.serverId) }
            factoryOf(::ZeroTierPeerImpl) { bind<ZeroTierPeer>() }
            factoryOf(::NebulaAndroidImpl) { bind<Nebula>() }
        })

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = true,
            )
        )
    }
}