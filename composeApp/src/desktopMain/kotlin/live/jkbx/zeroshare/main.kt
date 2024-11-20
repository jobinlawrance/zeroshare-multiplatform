package live.jkbx.zeroshare
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import live.jkbx.zeroshare.di.initKoin
import org.koin.dsl.module
import java.util.Properties

fun main() = application {
    initKoin(module {
        single<Settings> { PropertiesSettings(Properties()) }
        single<HttpClientEngine> { CIO.create() }
    })

    Window(
        onCloseRequest = ::exitApplication,
        title = "ZeroShare",
    ) {
        App()
    }
}