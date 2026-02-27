package dev.kaero.runtime

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.json.JsonElement

/**
 * Request context passed to handlers.
 *
 * This wraps Ktor's [ApplicationCall] and exposes a minimal, beginner-friendly API
 * (params, query, body parsing, and standard response helpers).
 */
class Ctx(
    val call: ApplicationCall,
) {
    val params: Map<String, String>
        get() = call.parameters.names().associateWith { name -> call.parameters[name].orEmpty() }

    val query: Map<String, List<String>>
        get() = call.request.queryParameters.names().associateWith { name -> call.request.queryParameters.getAll(name).orEmpty() }

    suspend inline fun <reified T : Any> body(): T = call.receive<T>()

    inline fun <reified T> ok(data: T): KaeroOut =
        KaeroTyped(HttpStatusCode.OK, ApiSuccess(data), kaeroSuccessTypeInfo<T>())

    inline fun <reified T> created(data: T): KaeroOut =
        KaeroTyped(HttpStatusCode.Created, ApiSuccess(data), kaeroSuccessTypeInfo<T>())

    fun noContent(): KaeroOut = KaeroNoContent

    fun fail(
        code: String,
        message: String,
        details: JsonElement? = null,
        status: HttpStatusCode = HttpStatusCode.BadRequest,
    ): KaeroOut = KaeroTyped(
        status,
        ApiError(ErrorBody(code = code, message = message, details = details)),
        kaeroErrorTypeInfo(),
    )
}
