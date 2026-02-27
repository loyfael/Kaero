package dev.kaero.sample.app.services

import dev.kaero.sample.app.models.Todo
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory Todo service (no DB).
 *
 * Kept intentionally small to focus on framework conventions rather than persistence.
 */
object TodoService {
    private val idSeq = AtomicInteger(0)
    private val todos = mutableListOf<Todo>()

    fun list(): List<Todo> = todos.toList()

    fun find(id: Int): Todo? = todos.firstOrNull { it.id == id }

    fun create(title: String): Todo {
        val todo = Todo(id = idSeq.incrementAndGet(), title = title, done = false)
        todos += todo
        return todo
    }

    fun toggle(id: Int): Todo? {
        val idx = todos.indexOfFirst { it.id == id }
        if (idx < 0) return null

        val updated = todos[idx].copy(done = !todos[idx].done)
        todos[idx] = updated
        return updated
    }

    fun delete(id: Int): Boolean {
        return todos.removeIf { it.id == id }
    }
}
