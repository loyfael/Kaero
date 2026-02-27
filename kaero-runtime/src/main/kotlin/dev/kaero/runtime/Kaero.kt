/**
 * Kaero runtime Ktor integration.
 *
 * Installs Ktor plugins (JSON, CORS, compression, error mapping) and registers
 * routes defined in [KaeroRouter]. Optionally serves a built frontend (Vite dist)
 * with an SPA fallback.
 */
package dev.kaero.runtime

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File

data class KaeroConfig(
    val corsAnyHost: Boolean = true,
    val serveFrontendDist: File? = null,
    val spaFallback: Boolean = true,
)

private val logger = KtorSimpleLogger("Kaero")

fun Application.installKaero(router: KaeroRouter, config: KaeroConfig = KaeroConfig()) {
    install(CallLogging)
    install(Compression) {
        gzip()
        deflate()
    }

    install(ContentNegotiation) {
        @OptIn(ExperimentalSerializationApi::class)
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
            contentType = ContentType.Application.Json,
        )
    }

    install(CORS) {
        if (config.corsAnyHost) {
            anyHost()
        }
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception on ${call.request.path()}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(
                    ErrorBody(
                        code = "INTERNAL_ERROR",
                        message = "Internal error",
                    ),
                ),
            )
        }
    }

    routing {
        registerKaeroRoutes(router)
        serveFrontendIfConfigured(config)
    }
}

private fun Route.registerKaeroRoutes(router: KaeroRouter) {
    for (route in router.routes) {
        val ktorPath = normalizePath(route.path)
        when (route.method) {
            HttpMethod.GET -> get(ktorPath) { handleKaero(call, route.handler) }
            HttpMethod.POST -> post(ktorPath) { handleKaero(call, route.handler) }
            HttpMethod.PUT -> put(ktorPath) { handleKaero(call, route.handler) }
            HttpMethod.PATCH -> patch(ktorPath) { handleKaero(call, route.handler) }
            HttpMethod.DELETE -> delete(ktorPath) { handleKaero(call, route.handler) }
        }
    }
}

private suspend fun handleKaero(call: io.ktor.server.application.ApplicationCall, handler: suspend (Ctx) -> Any?) {
    val out = handler(Ctx(call))

    when (out) {
        is KaeroTyped -> call.respond(out.status, out.body, out.typeInfo)
        is KaeroNoContent -> call.respond(HttpStatusCode.NoContent)
        null -> call.respond(HttpStatusCode.NoContent)
        else -> call.respond(HttpStatusCode.OK, ApiSuccess(out))
    }
}

private fun Route.serveFrontendIfConfigured(config: KaeroConfig) {
    val distDir = config.serveFrontendDist ?: return
    if (!distDir.exists() || !distDir.isDirectory) return

    get("/") {
        val index = File(distDir, "index.html")
        if (index.exists()) {
            call.respondFile(index)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/{...}") {
        val path = call.parameters.getAll("...")?.joinToString("/") ?: ""
        val file = File(distDir, path)

        if (file.exists() && file.isFile) {
            call.respondFile(file)
            return@get
        }

        if (config.spaFallback) {
            val index = File(distDir, "index.html")
            if (index.exists()) {
                call.respondFile(index)
                return@get
            }
        }

        call.respond(HttpStatusCode.NotFound)
    }
}
