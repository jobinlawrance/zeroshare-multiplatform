package live.jkbx.zeroshare

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import live.jkbx.zeroshare.di.initKoin
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.nebula.NebulaJVMImpl
import live.jkbx.zeroshare.socket.KmpHashing
import live.jkbx.zeroshare.socket.KmpHashingJVMImpl
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences

fun main() = application {
    initKoin(module {
        single<Settings> { PreferencesSettings(Preferences.userRoot()) }
        single<HttpClientEngine> {
            OkHttp.create {
                config {
                    connectTimeout(0, TimeUnit.MINUTES)
                    readTimeout(0, TimeUnit.MINUTES)
                    writeTimeout(0, TimeUnit.MINUTES)
                    pingInterval(20, TimeUnit.SECONDS)
                }
            }
        }
        single<String>(named("backendUrl")) { System.getenv("BACKEND_URL") }
        single<KmpHashing> { KmpHashingJVMImpl() }
        single<Nebula> { NebulaJVMImpl(get(qualifier("backendUrl"))) }
    })

    NotifierManager.initialize(
        NotificationPlatformConfiguration.Desktop(
            showPushNotification = true,
            notificationIconPath = composeDesktopResourcesPath() + File.separator + "laptop.png"
        )
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "ZeroShare",
    ) {
        App()
    }
}