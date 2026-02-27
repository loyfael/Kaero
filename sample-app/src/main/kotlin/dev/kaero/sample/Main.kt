/**
 * Sample Kaero application entrypoint.
 *
 * Starts a Ktor server on port 8080, registers routes, and (if available)
 * serves the built frontend from `frontend/dist`.
 */
package dev.kaero.sample

import dev.kaero.runtime.KaeroConfig
import dev.kaero.runtime.KaeroRouter
import dev.kaero.runtime.installKaero
import dev.kaero.sample.kaero.registerRoutes
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

fun main() {
    val router = KaeroRouter().apply {
        registerRoutes()
    }

    val dist = File("frontend/dist")

    embeddedServer(Netty, port = 8080) {
        installKaero(
            router,
            KaeroConfig(
                corsAnyHost = true,
                serveFrontendDist = dist.takeIf { it.exists() },
                spaFallback = true,
            ),
        )
    }.start(wait = true)
}
