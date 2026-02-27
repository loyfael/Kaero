package dev.kaero.sample.app.models

import kotlinx.serialization.Serializable

/**
 * Serializable Todo model used by the sample CRUD.
 */
@Serializable
data class Todo(
    val id: Int,
    val title: String,
    val done: Boolean,
)
