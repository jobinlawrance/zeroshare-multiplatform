package live.jkbx.zeroshare.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.http.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.client.KtorClientTracing
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ResourceAttributes
import live.jkbx.zeroshare.controllers.GoogleAuthProvider
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.viewmodels.ZeroTierViewModel
import android.provider.Settings as AndroidSettings
import org.koin.java.KoinJavaComponent.inject

actual fun openUrlInBrowser(url: String) {
    TODO("Call it with activity context")
}

actual suspend fun getMachineName(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}


@Composable
actual fun loginWithGoogle(
    onLoginSuccess: (SSEEvent) -> Unit,
    onLoginError: (Throwable) -> Unit
) {
    val googleAuthProvider: GoogleAuthProvider by inject(GoogleAuthProvider::class.java)
    val zeroTierViewModel by inject<ZeroTierViewModel>(ZeroTierViewModel::class.java)
    val uiProvider = googleAuthProvider.getUiProvider()

    LaunchedEffect(Unit) {
        try {
            val googleUser = uiProvider.signIn()
            val sseEvent = zeroTierViewModel.verifyGoogleToken(googleUser!!.idToken)

            onLoginSuccess(sseEvent)
        }  catch (e: Exception) {
            onLoginError(e)
        }
    }
}

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun uniqueDeviceId(): String {
    val context by inject<Context>(Context::class.java)
    return AndroidSettings.Secure.getString(context.contentResolver, AndroidSettings.Secure.ANDROID_ID)
}

actual fun HttpClientConfig<*>.installOpenTelemetry() {
    val openTelemetry = getOpenTelemetry(serviceName = "ktor-client-android")

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