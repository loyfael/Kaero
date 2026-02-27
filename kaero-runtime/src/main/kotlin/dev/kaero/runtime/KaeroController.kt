package dev.kaero.runtime

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonElement

/**
 * Base controller (style Symfony/Laravel).
 *
 * - Controller methods do not need to accept [Ctx] as a parameter.
 * - Kaero creates one controller instance per request and injects the context.
 */
abstract class KaeroController {
    @PublishedApi
    internal lateinit var ctx: Ctx

    @PublishedApi
    internal fun bind(ctx: Ctx) {
        this.ctx = ctx
    }

    protected val params: Map<String, String> get() = ctx.params
    protected val query: Map<String, List<String>> get() = ctx.query

    protected fun param(name: String): String? = params[name]

    protected fun paramInt(name: String): Int? = params[name]?.toIntOrNull()

    protected suspend inline fun <reified T : Any> body(): T = ctx.body<T>()

    protected inline fun <reified T> ok(data: T): KaeroOut = ctx.ok(data)

    protected inline fun <reified T> created(data: T): KaeroOut = ctx.created(data)

    protected fun noContent(): KaeroOut = ctx.noContent()

    protected fun fail(
        code: String,
        message: String,
        details: JsonElement? = null,
        status: HttpStatusCode = HttpStatusCode.BadRequest,
    ): KaeroOut = ctx.fail(code = code, message = message, details = details, status = status)
}
