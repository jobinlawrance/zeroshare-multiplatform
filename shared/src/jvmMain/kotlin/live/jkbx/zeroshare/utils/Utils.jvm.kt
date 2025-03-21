package live.jkbx.zeroshare.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import io.ktor.client.*
import io.ktor.http.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.client.KtorClientTracing
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ResourceAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.jkbx.zeroshare.di.refreshTokenKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.viewmodels.LoginViewModel
import org.koin.java.KoinJavaComponent.inject
import java.awt.Desktop
import java.net.InetAddress
import java.net.URI
import java.util.UUID

actual fun openUrlInBrowser(url: String) {
    Desktop.getDesktop().browse(URI(url))
}

actual suspend fun getMachineName(): String {
    return try {
        withContext(Dispatchers.IO) {
            InetAddress.getLocalHost()
        }.hostName
    } catch (e: Exception) {
        throw IllegalStateException("Failed to get machine name", e)
    }
}

@Composable
actual fun loginWithGoogle(
    onLoginSuccess: (SSEEvent) -> Unit,
    onLoginError: (Throwable) -> Unit
) {
    val loginViewModel by inject<LoginViewModel>(LoginViewModel::class.java)
    val sessionToken = UUID.randomUUID().toString()
    val url = loginViewModel.creteNetworkURL(sessionToken)
    val settings by inject<Settings>(Settings::class.java)

    openUrlInBrowser(url)
    LaunchedEffect(Unit) {
        loginViewModel.listenToLogin(sessionToken, { sseEvent ->
            settings.putString(tokenKey, sseEvent.token)
            settings.putString(refreshTokenKey, sseEvent.refreshToken)
            onLoginSuccess(sseEvent)
        })
    }
}

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun uniqueDeviceId(): String {
    val settings by inject<Settings>(Settings::class.java)
    val deviceId = settings.get("device_id", "")
    if (deviceId.isEmpty()) {
        val uuid = UUID.randomUUID().toString()
        settings.putString("device_id", uuid)
        return uuid
    }
    return deviceId
}

actual fun HttpClientConfig<*>.installOpenTelemetry() {
    val openTelemetry = getOpenTelemetry(serviceName = "ktor-client-jvm")

    install(KtorClientTracing) {
        setOpenTelemetry(openTelemetry)

        emitExperimentalHttpClientMetrics()

        knownMethods(HttpMethod.DefaultMethods)
        capturedRequestHeaders(HttpHeaders.Accept)
        capturedResponseHeaders(HttpHeaders.ContentType)

        attributeExtractor {
            onStart {
                attributes.put("start-time", System.currentTimeMillis())
            }
            onEnd {
                attributes.put("end-time", System.currentTimeMillis())
            }
        }
    }
}

fun getOpenTelemetry(serviceName: String): OpenTelemetry {
    return AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer { oldResource, _ ->
        oldResource.toBuilder()
            .putAll(oldResource.attributes)
            .put(ResourceAttributes.SERVICE_NAME, serviceName)
            .build()
    }.build().openTelemetrySdk
}