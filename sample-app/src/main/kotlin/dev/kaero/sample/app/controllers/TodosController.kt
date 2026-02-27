package dev.kaero.sample.app.controllers

import dev.kaero.runtime.KaeroController
import dev.kaero.runtime.annotations.Delete
import dev.kaero.runtime.annotations.Get
import dev.kaero.runtime.annotations.Patch
import dev.kaero.runtime.annotations.Post
import dev.kaero.sample.app.models.Todo
import dev.kaero.sample.app.services.TodoService
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * Sample CRUD controller showing Kaero's annotation-based routing.
 *
 * Routes:
 * - GET /api/todos
 * - GET /api/todos/:id
 * - POST /api/todos
 * - PATCH /api/todos/:id
 * - DELETE /api/todos/:id
 */
class TodosController : KaeroController() {

    @Serializable
    data class CreateTodoBody(val title: String)

    @Get("/api/todos")
    suspend fun index() = ok(TodoService.list())

    @Get("/api/todos/:id")
    suspend fun show(): Any {
        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")

        return TodoService.find(id)
            ?.let { ok(it) }
            ?: fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
    }

    @Post("/api/todos")
    suspend fun create(): Any {
        val body = runCatching { body<CreateTodoBody>() }
            .getOrElse { return fail("VALIDATION_ERROR", "Invalid JSON body") }

        if (body.title.isBlank()) {
            return fail("VALIDATION_ERROR", "Title is required")
        }

        val todo = TodoService.create(body.title)
        return created(todo)
    }

    @Patch("/api/todos/:id")
    suspend fun update(): Any {
        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")

        return TodoService.toggle(id)
            ?.let { ok(it) }
            ?: fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
    }

    @Delete("/api/todos/:id")
    suspend fun delete(): Any {
        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")

        val deleted = TodoService.delete(id)
        return if (deleted) noContent() else fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
    }
}
