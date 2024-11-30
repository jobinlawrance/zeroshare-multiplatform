package live.jkbx.zeroshare
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import live.jkbx.zeroshare.di.initKoin
import live.jkbx.zeroshare.nebula.Nebula
import live.jkbx.zeroshare.nebula.NebulaJVMImpl
import live.jkbx.zeroshare.socket.FileTransfer
import live.jkbx.zeroshare.socket.JavaSocketFileTransfer
import live.jkbx.zeroshare.socket.KmpHashing
import live.jkbx.zeroshare.socket.KmpHashingJVMImpl
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.io.File
import java.util.Properties
import java.util.prefs.Preferences

fun main() = application {
    initKoin(module {
        single<Settings> { PreferencesSettings(Preferences.userRoot()) }
        single<HttpClientEngine> { OkHttp.create() }
        factoryOf(::ZeroTierPeerImpl) { bind<ZeroTierPeer>() }
        single<KmpHashing> { KmpHashingJVMImpl() }
        single<Nebula> { NebulaJVMImpl() }
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