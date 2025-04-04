package live.jkbx.zeroshare.di

import co.touchlab.kermit.Logger
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.socket.FileSaver
import live.jkbx.zeroshare.socket.KmpHashing
import live.jkbx.zeroshare.utils.SettingsUtil
import live.jkbx.zeroshare.viewmodels.LoginViewModel
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

fun initKoinIos(
    userDefaults: NSUserDefaults,
    doOnStartup: () -> Unit,
    kmpHashing: KmpHashing,
): KoinApplication = initKoin(
    module {
        single<Settings> { NSUserDefaultsSettings(userDefaults) }
        single { doOnStartup }
        single<HttpClientEngine> { Darwin.create() }
        single { FileSaver }
        single { kmpHashing }
    }
)

// Access from Swift to create a logger
@Suppress("unused")
fun Koin.loggerWithTag(tag: String) = get<Logger>(qualifier = null) { parametersOf(tag) }

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getLoginViewModel() = getKoin().get<LoginViewModel>()
    fun getBackendApi() = getKoin().get<BackendApi>()
    fun getSettingsUtil() = getKoin().get<SettingsUtil>()
    fun getFileSaver() = getKoin().get<FileSaver>()
    fun getJson() = getKoin().get<Json>()
}

actual val platformModule: Module = module {

}