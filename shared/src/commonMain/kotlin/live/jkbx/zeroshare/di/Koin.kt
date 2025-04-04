package live.jkbx.zeroshare.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.koin.KermitKoinLogger
import co.touchlab.kermit.platformLogWriter
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.models.GoogleAuthCredentials
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.network.getHttpClient
import live.jkbx.zeroshare.socket.KtorFileTransfer
import live.jkbx.zeroshare.utils.SettingsUtil
import live.jkbx.zeroshare.viewmodels.LoginViewModel
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module

const val tokenKey = "token"
const val refreshTokenKey = "refreshToken"
const val networkIdKey = "networkId"
const val nodeIdKey = "nodeId"
const val nebulaSetupKey = "nebulaSetup"

fun initKoin(appModule: Module): KoinApplication {
    val koinApplication = startKoin {
        logger(
            KermitKoinLogger(Logger.withTag("koin"))
        )
        modules(
            appModule,
            coreModule,
            platformModule
        )
    }

    // Dummy initialization logic, making use of appModule declarations for demonstration purposes.
    val koin = koinApplication.koin

    val kermit = koin.get<Logger> { parametersOf(null) }


    return koinApplication
}

private val coreModule = module {


    // platformLogWriter() is a relatively simple config option, useful for local debugging. For production
    // uses you *may* want to have a more robust configuration from the native platform. In KaMP Kit,
    // that would likely go into platformModule expect/actual.
    // See https://github.com/touchlab/Kermit
    val baseLogger =
        Logger(config = StaticConfig(logWriterList = listOf(platformLogWriter())), "ZeroShare")
    factory { (tag: String?) -> if (tag != null) baseLogger.withTag(tag) else baseLogger }

    single<Json> { Json {
        isLenient = true
        prettyPrint = false
        ignoreUnknownKeys = true
    } }

    single { BackendApi(get(qualifier("backendUrl"))) }
    single { LoginViewModel() }
    factory { GoogleAuthCredentials(get(qualifier("serverId"))) }
    single { SettingsUtil() }
    single { KtorFileTransfer() }
    single { getHttpClient(get(), get(), baseLogger.withTag("Http-Client"), get(), get(qualifier("backendUrl"))) }

}

internal inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}

// Simple function to clean up the syntax a bit
fun KoinComponent.injectLogger(tag: String): Lazy<Logger> = inject { parametersOf(tag) }

expect val platformModule: Module