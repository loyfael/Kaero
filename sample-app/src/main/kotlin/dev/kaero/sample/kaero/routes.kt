/**
 * Central routing entrypoint for the sample app.
 *
 * Kaero intentionally keeps routing in one place.
 */
package dev.kaero.sample.kaero

import dev.kaero.runtime.KaeroRouter
import dev.kaero.sample.app.controllers.TodosController

fun KaeroRouter.registerRoutes() {
    controller<TodosController>()
}
