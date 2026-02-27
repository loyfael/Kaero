/**
 * Kaero routing.
 *
 * Provides a small DSL and an annotation-based controller registration helper.
 * Paths use `:param` syntax which is normalized to Ktor's `{param}` format.
 */
package dev.kaero.runtime

import dev.kaero.runtime.annotations.Delete
import dev.kaero.runtime.annotations.Get
import dev.kaero.runtime.annotations.Patch
import dev.kaero.runtime.annotations.Post
import dev.kaero.runtime.annotations.Put
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE,
}

data class RouteDef(
    val method: HttpMethod,
    val path: String,
    val handler: suspend (Ctx) -> Any?,
)

class KaeroRouter {
    @PublishedApi
    internal val routes: MutableList<RouteDef> = mutableListOf()

    fun get(path: String, handler: suspend (Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.GET, path, handler)
    }

    inline fun <reified C : Any> get(path: String, noinline action: suspend C.(Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.GET, path, controllerAction(action))
    }

    fun post(path: String, handler: suspend (Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.POST, path, handler)
    }

    inline fun <reified C : Any> post(path: String, noinline action: suspend C.(Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.POST, path, controllerAction(action))
    }

    fun put(path: String, handler: suspend (Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.PUT, path, handler)
    }

    inline fun <reified C : Any> put(path: String, noinline action: suspend C.(Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.PUT, path, controllerAction(action))
    }

    fun patch(path: String, handler: suspend (Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.PATCH, path, handler)
    }

    inline fun <reified C : Any> patch(path: String, noinline action: suspend C.(Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.PATCH, path, controllerAction(action))
    }

    fun delete(path: String, handler: suspend (Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.DELETE, path, handler)
    }

    inline fun <reified C : Any> delete(path: String, noinline action: suspend C.(Ctx) -> Any?) {
        routes += RouteDef(HttpMethod.DELETE, path, controllerAction(action))
    }

    /**
     * Symfony/Laravel-inspired: register all routes from annotations on a controller.
     *
     * Controller must extend [KaeroController], methods must be `suspend fun foo(): Any?`.
     */
    inline fun <reified C> controller() where C : KaeroController {
        val kClass = C::class

        for (fn in kClass.memberFunctions) {
            val get = fn.findAnnotation<Get>()
            val post = fn.findAnnotation<Post>()
            val put = fn.findAnnotation<Put>()
            val patch = fn.findAnnotation<Patch>()
            val delete = fn.findAnnotation<Delete>()

            val (method, path) = when {
                get != null -> HttpMethod.GET to get.path
                post != null -> HttpMethod.POST to post.path
                put != null -> HttpMethod.PUT to put.path
                patch != null -> HttpMethod.PATCH to patch.path
                delete != null -> HttpMethod.DELETE to delete.path
                else -> continue
            }

            if (fn.parameters.size != 1) {
                throw IllegalStateException(
                    "@Route method ${kClass.qualifiedName}.${fn.name} must not declare parameters",
                )
            }

            routes += RouteDef(method, path) { ctx ->
                val controller = runCatching { kClass.createInstance() }
                    .getOrElse { e ->
                        throw IllegalStateException(
                            "Controller ${kClass.qualifiedName} must have a public no-arg constructor",
                            e,
                        )
                    }

                controller.bind(ctx)
                fn.callSuspend(controller)
            }
        }
    }

    @PublishedApi
    internal inline fun <reified C : Any> controllerAction(noinline action: suspend C.(Ctx) -> Any?): suspend (Ctx) -> Any? {
        return { ctx ->
            val controller = runCatching { C::class.createInstance() }
                .getOrElse { e ->
                    throw IllegalStateException(
                        "Controller ${C::class.qualifiedName} must have a public no-arg constructor",
                        e,
                    )
                }
            controller.action(ctx)
        }
    }
}

internal fun normalizePath(path: String): String {
    val withSlash = if (path.startsWith('/')) path else "/$path"
    return withSlash.replace(Regex(":([A-Za-z0-9_]+)")) { m -> "{${m.groupValues[1]}}" }
}
