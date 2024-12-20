package live.jkbx.zeroshare.rpc
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.ktor.v3_0.server.KtorServerTracing
import org.slf4j.event.*
import utils.getOpenTelemetry
import java.time.Instant

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}



fun Application.configureTelemetry(): OpenTelemetry {
    val openTelemetry = getOpenTelemetry("krpc-server")

    install(KtorServerTracing) {
        setOpenTelemetry(openTelemetry)

        knownMethods(HttpMethod.DefaultMethods)
        capturedRequestHeaders(HttpHeaders.UserAgent)
        capturedResponseHeaders(HttpHeaders.ContentType)

        spanStatusExtractor {
            val path = response?.call?.request?.path() ?: ""
            if (path.contains("/span-status-extractor") || error != null) {
                spanStatusBuilder.setStatus(StatusCode.ERROR)
            }
        }

        spanKindExtractor {
            if (httpMethod == HttpMethod.Post) {
                SpanKind.PRODUCER
            } else {
                SpanKind.CLIENT
            }
        }

        attributeExtractor {
            onStart {
                attributes.put("start-time", System.currentTimeMillis())
            }
            onEnd {
                attributes.put("end-time", Instant.now().toEpochMilli())
            }
        }
    }
    return openTelemetry

}