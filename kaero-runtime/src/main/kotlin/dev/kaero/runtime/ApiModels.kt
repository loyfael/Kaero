/**
 * Kaero runtime response models.
 *
 * Kaero standardizes HTTP responses as either:
 * - success: `{ "data": ... }`
 * - error: `{ "error": { code, message, details? } }`
 *
 * This file also contains the typed wrappers used by Ktor so generic payloads
 * serialize correctly.
 */
package dev.kaero.runtime

import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import io.ktor.util.reflect.typeInfo

@Serializable
data class ApiSuccess<T>(val data: T)

@Serializable
data class ApiError(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: JsonElement? = null,
)

sealed interface KaeroOut {
    val status: HttpStatusCode
}

data class KaeroTyped(
    override val status: HttpStatusCode,
    val body: Any,
    val typeInfo: TypeInfo,
) : KaeroOut

data class KaeroFailure(
    override val status: HttpStatusCode,
    val body: ApiError,
) : KaeroOut

data object KaeroNoContent : KaeroOut {
    override val status: HttpStatusCode = HttpStatusCode.NoContent
}

@PublishedApi
internal inline fun <reified T> kaeroSuccessTypeInfo(): TypeInfo = typeInfo<ApiSuccess<T>>()

@PublishedApi
internal fun kaeroErrorTypeInfo(): TypeInfo = typeInfo<ApiError>()
