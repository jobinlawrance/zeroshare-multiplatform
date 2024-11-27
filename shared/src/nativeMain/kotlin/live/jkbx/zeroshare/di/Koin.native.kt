package live.jkbx.zeroshare.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.XcodeSeverityWriter
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.utils.SettingsUtil
import live.jkbx.zeroshare.viewmodels.ZeroTierViewModel
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.module.Module
import org.koin.core.module.single
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

fun initKoinIos(
    userDefaults: NSUserDefaults,
    doOnStartup: () -> Unit
): KoinApplication = initKoin(
    module {
        single<Settings> { NSUserDefaultsSettings(userDefaults) }
        single { doOnStartup }
        single<HttpClientEngine> { Darwin.create() }
    }
)

// Access from Swift to create a logger
@Suppress("unused")
fun Koin.loggerWithTag(tag: String) = get<Logger>(qualifier = null) { parametersOf(tag) }

@Suppress("unused") // Called from Swift
object KotlinDependencies : KoinComponent {
    fun getZeroTierViewModel() = getKoin().get<ZeroTierViewModel>()
    fun getBackendApi() = getKoin().get<BackendApi>()
    fun getSettingsUtil() = getKoin().get<SettingsUtil>()
}

actual val platformModule: Module = module {

}