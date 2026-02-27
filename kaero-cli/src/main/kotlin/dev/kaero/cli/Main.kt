package dev.kaero.cli

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

private const val MIN_BUN = "1.2.0"
private const val MIN_GRADLE = "9.3.0"
private const val REQUIRED_JAVA_MAJOR = 25

fun main(args: Array<String>) {
    val argv = args.toList()

    if (argv.isEmpty() || argv.first() in setOf("-h", "--help", "help")) {
        usage()
        return
    }

    when (argv.first()) {
        "init" -> cmdInit(argv.drop(1))
        "doctor" -> cmdDoctor(argv.drop(1))
        else -> {
            System.err.println("Unknown command: ${argv.first()}")
            usage()
            kotlin.system.exitProcess(2)
        }
    }
}

private fun usage() {
    println(
        """
Kaero CLI

Usage:
  kaero doctor
  kaero init <directory>

Commands:
  doctor   Checks required tools and versions
  init     Creates a new Kaero project in <directory>

Requirements (checked by 'doctor' and 'init'):
  - Bun >= $MIN_BUN
  - Java major == $REQUIRED_JAVA_MAJOR
  - Gradle >= $MIN_GRADLE

Notes:
  - 'init' creates a self-contained Gradle multi-module project (runtime + app).
  - The generated project uses the Gradle Wrapper, but Gradle is still checked
    because this CLI is currently built and distributed via Gradle.
""".trimIndent(),
    )
}

private fun cmdDoctor(args: List<String>) {
    if (args.isNotEmpty()) {
        System.err.println("doctor takes no arguments")
        usage()
        kotlin.system.exitProcess(2)
    }

    val bun = checkBun()
    val java = checkJava()
    val gradle = checkGradle()

    if (!bun.ok || !java.ok || !gradle.ok) {
        System.err.println()
        System.err.println("Some requirements are not met.")
        kotlin.system.exitProcess(1)
    }
}

private fun cmdInit(args: List<String>) {
    if (args.size != 1) {
        System.err.println("init requires exactly one argument: <directory>")
        usage()
        kotlin.system.exitProcess(2)
    }

    val targetDir = Path.of(args[0]).toAbsolutePath().normalize()

    val bun = checkBun()
    val java = checkJava()
    val gradle = checkGradle()

    if (!bun.ok || !java.ok || !gradle.ok) {
        System.err.println()
        System.err.println("Cannot initialize project because requirements are not met.")
        kotlin.system.exitProcess(1)
    }

    if (targetDir.exists()) {
        val existing = Files.list(targetDir).use { it.findAny().isPresent }
        if (existing) {
            System.err.println("Target directory is not empty: $targetDir")
            kotlin.system.exitProcess(1)
        }
    } else {
        targetDir.createDirectories()
    }

    val appName = targetDir.name

    println("▶ Initializing Kaero project in: $targetDir")
    println("   App name: $appName")

    writeText(targetDir.resolve("README.md"), templateReadme(appName))
    writeText(targetDir.resolve(".gitignore"), templateGitignore())

    writeText(targetDir.resolve("gradle.properties"), templateGradleProperties())
    writeText(targetDir.resolve("settings.gradle.kts"), templateSettings(appName))
    writeText(targetDir.resolve("build.gradle.kts"), templateRootBuild())

    writeRuntimeModule(targetDir.resolve("kaero-runtime"))
    writeAppModule(targetDir.resolve(appName), appName)

    // Generate a real Gradle Wrapper (cross-platform scripts + wrapper JAR).
    // This requires Gradle to be installed, which we already checked above.
    println("▶ Generating Gradle Wrapper...")
    val wrapperOk = runInDir(
        dir = targetDir,
        command = listOf(
            "gradle",
            "wrapper",
            "--gradle-version",
            "9.3.1",
            "--distribution-type",
            "bin",
        ),
    )
    if (!wrapperOk) {
        System.err.println("❌ Failed to generate Gradle Wrapper. Try running: gradle wrapper")
        kotlin.system.exitProcess(1)
    }

    println("✅ Project created")
    println("Next steps:")
    println("  cd $targetDir")
    println("  bun install --cwd $appName/frontend")
    println("  ./gradlew :$appName:run")
    println("  (in another terminal) bun --cwd $appName/frontend run dev")
}

private data class CheckResult(val ok: Boolean, val label: String, val details: String)

private fun checkBun(): CheckResult {
    val version = runAndCapture(listOf("bun", "--version"))
    if (version == null) {
        val msg = "Bun not found in PATH"
        System.err.println("❌ Bun: $msg")
        return CheckResult(false, "bun", msg)
    }

    val v = version.trim()
    val ok = compareVersions(v, MIN_BUN) >= 0
    if (ok) {
        println("✅ Bun: $v")
        return CheckResult(true, "bun", v)
    }

    val msg = "Found $v, need >= $MIN_BUN"
    System.err.println("❌ Bun: $msg")
    return CheckResult(false, "bun", msg)
}

private fun checkJava(): CheckResult {
    // `java -version` prints to stderr on most JDKs.
    val out = runAndCapture(listOf("java", "-version"), mergeStderr = true)
    if (out == null) {
        val msg = "Java not found in PATH"
        System.err.println("❌ Java: $msg")
        return CheckResult(false, "java", msg)
    }

    val major = Regex("version\\s+\"(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
    if (major == null) {
        val msg = "Could not parse Java version"
        System.err.println("❌ Java: $msg")
        return CheckResult(false, "java", msg)
    }

    val ok = major == REQUIRED_JAVA_MAJOR
    if (ok) {
        println("✅ Java: $major")
        return CheckResult(true, "java", major.toString())
    }

    val msg = "Found $major, need $REQUIRED_JAVA_MAJOR"
    System.err.println("❌ Java: $msg")
    return CheckResult(false, "java", msg)
}

private fun checkGradle(): CheckResult {
    val out = runAndCapture(listOf("gradle", "--version"))
    if (out == null) {
        val msg = "Gradle not found in PATH"
        System.err.println("❌ Gradle: $msg")
        return CheckResult(false, "gradle", msg)
    }

    val version = Regex("Gradle\\s+(\\d+\\.\\d+(?:\\.\\d+)?)").find(out)?.groupValues?.get(1)
    if (version == null) {
        val msg = "Could not parse Gradle version"
        System.err.println("❌ Gradle: $msg")
        return CheckResult(false, "gradle", msg)
    }

    val ok = compareVersions(version, MIN_GRADLE) >= 0
    if (ok) {
        println("✅ Gradle: $version")
        return CheckResult(true, "gradle", version)
    }

    val msg = "Found $version, need >= $MIN_GRADLE"
    System.err.println("❌ Gradle: $msg")
    return CheckResult(false, "gradle", msg)
}

private fun runAndCapture(command: List<String>, mergeStderr: Boolean = false): String? {
    return try {
        val pb = ProcessBuilder(command)
        if (mergeStderr) pb.redirectErrorStream(true)
        val process = pb.start()
        val bytes = process.inputStream.readBytes()
        val exit = process.waitFor()
        if (exit != 0) return null
        bytes.toString(Charsets.UTF_8)
    } catch (_: Throwable) {
        null
    }
}

/**
 * Compares versions like "1.2.3".
 * Returns > 0 if a > b, 0 if equal, < 0 if a < b.
 */
private fun compareVersions(a: String, b: String): Int {
    val pa = a.trim().split(".").mapNotNull { it.toIntOrNull() }
    val pb = b.trim().split(".").mapNotNull { it.toIntOrNull() }

    val max = maxOf(pa.size, pb.size)
    for (i in 0 until max) {
        val va = pa.getOrElse(i) { 0 }
        val vb = pb.getOrElse(i) { 0 }
        if (va != vb) return va.compareTo(vb)
    }
    return 0
}

private fun writeText(path: Path, content: String) {
    path.parent?.createDirectories()
    Files.writeString(path, content, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}

private fun runInDir(dir: Path, command: List<String>): Boolean {
    return try {
        val pb = ProcessBuilder(command)
            .directory(dir.toFile())
            .inheritIO()
        val process = pb.start()
        process.waitFor() == 0
    } catch (_: Throwable) {
        false
    }
}

private fun templateReadme(appName: String): String =
    """
+# $appName (Kaero)
+
+## Requirements
+
+- Java 25
+- Gradle >= $MIN_GRADLE
+- Bun >= $MIN_BUN
+
+## Run (dev)
+
+Backend:
+
+```bash
+./gradlew :$appName:run
+```
+
+Frontend:
+
+```bash
+cd $appName/frontend
+bun install
+bun run dev
+```
+""".trimIndent()

private fun templateGitignore(): String =
    """
+.gradle/
+**/build/
+.idea/
+*.iml
+*.log
+.DS_Store
+node_modules/
+frontend/dist/
+bun.lockb
+.bun/
+.vscode/
""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateGradleProperties(): String =
    """
+kotlinVersion=2.2.21
+ktorVersion=2.3.12
+serializationVersion=1.6.3
+kaeroVersion=0.1.0-SNAPSHOT
+org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
+org.gradle.java.installations.auto-download=true
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateSettings(appName: String): String =
    """
+// Generated by Kaero CLI.
+
+pluginManagement {
+    val kotlinVersion: String by settings
+
+    repositories {
+        gradlePluginPortal()
+        mavenCentral()
+    }
+
+    plugins {
+        kotlin("jvm") version kotlinVersion
+        kotlin("plugin.serialization") version kotlinVersion
+    }
+}
+
+plugins {
+    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
+}
+
+dependencyResolutionManagement {
+    repositories {
+        mavenCentral()
+    }
+}
+
+rootProject.name = "${appName}-kaero"
+include(":kaero-runtime")
+include(":$appName")
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRootBuild(): String =
    """
+// Root Gradle configuration shared by all subprojects.
+
+subprojects {
+    repositories {
+        mavenCentral()
+    }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun writeRuntimeModule(dir: Path) {
    writeText(dir.resolve("build.gradle.kts"), templateRuntimeBuild())

    val base = dir.resolve("src/main/kotlin/dev/kaero/runtime")
    writeText(base.resolve("ApiModels.kt"), templateRuntimeApiModels())
    writeText(base.resolve("Ctx.kt"), templateRuntimeCtx())
    writeText(base.resolve("KaeroController.kt"), templateRuntimeController())
    writeText(base.resolve("KaeroRouter.kt"), templateRuntimeRouter())
    writeText(base.resolve("Kaero.kt"), templateRuntimeKtor())
    writeText(base.resolve("annotations/Http.kt"), templateRuntimeHttpAnnotations())
}

private fun writeAppModule(dir: Path, appName: String) {
    writeText(dir.resolve("build.gradle.kts"), templateAppBuild(appName))

    val mainKt = dir.resolve("src/main/kotlin/dev/kaero/app/Main.kt")
    writeText(mainKt, templateAppMain())

    val routesKt = dir.resolve("src/main/kotlin/dev/kaero/app/kaero/routes.kt")
    writeText(routesKt, templateAppRoutes())

    val controllerKt = dir.resolve("src/main/kotlin/dev/kaero/app/app/controllers/TodosController.kt")
    writeText(controllerKt, templateTodosController())

    val serviceKt = dir.resolve("src/main/kotlin/dev/kaero/app/app/services/TodoService.kt")
    writeText(serviceKt, templateTodoService())

    val modelKt = dir.resolve("src/main/kotlin/dev/kaero/app/app/models/Todo.kt")
    writeText(modelKt, templateTodoModel())

    // Frontend
    val frontend = dir.resolve("frontend")
    writeText(frontend.resolve("package.json"), templateFrontendPackageJson())
    writeText(frontend.resolve("vite.config.ts"), templateViteConfig())
    writeText(frontend.resolve("index.html"), templateIndexHtml(appName))
    writeText(frontend.resolve("src/vite-env.d.ts"), "// Vite type definitions.\n/// <reference types=\"vite/client\" />\n")
    writeText(frontend.resolve("src/main.ts"), templateFrontendMainTs())
    writeText(frontend.resolve("src/App.vue"), templateFrontendAppVue())
    writeText(frontend.resolve("src/app/core/routing/router.ts"), templateFrontendRouterTs())
    writeText(frontend.resolve("src/app/app.view.vue"), templateFrontendAppViewVue())
    writeText(frontend.resolve("src/app/features/counter/counter.service.ts"), templateCounterServiceTs())
    writeText(frontend.resolve("src/app/features/counter/counter.store.ts"), templateCounterStoreTs())
    writeText(frontend.resolve("src/app/features/counter/counter.page.vue"), templateCounterPageVue())
}

// ---------------- templates (runtime) ----------------

private fun templateRuntimeBuild(): String =
    """
+// Kaero runtime module (Ktor integration + routing + standardized responses).
+
+import org.jetbrains.kotlin.gradle.dsl.JvmTarget
+import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
+
+val ktorVersion: String by project
+val serializationVersion: String by project
+
+plugins {
+    kotlin("jvm")
+    kotlin("plugin.serialization")
+    `java-library`
+}
+
+kotlin {
+    jvmToolchain(25)
+}
+
+group = "dev.kaero"
+version = (project.findProperty("kaeroVersion") as String?) ?: "0.1.0-SNAPSHOT"
+
+java {
+    toolchain {
+        languageVersion.set(JavaLanguageVersion.of(25))
+    }
+
+    sourceCompatibility = JavaVersion.VERSION_17
+    targetCompatibility = JavaVersion.VERSION_17
+}
+
+dependencies {
+    api(kotlin("reflect"))
+
+    api("io.ktor:ktor-server-core-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-netty-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-content-negotiation-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-serialization-kotlinx-json-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-status-pages-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-call-logging-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-cors-jvm:${'$'}ktorVersion")
+    api("io.ktor:ktor-server-compression-jvm:${'$'}ktorVersion")

+    api("org.jetbrains.kotlinx:kotlinx-serialization-json:${'$'}serializationVersion")
+
+    testImplementation(kotlin("test"))
+}
+
+tasks.withType<KotlinCompile>().configureEach {
+    compilerOptions {
+        jvmTarget.set(JvmTarget.JVM_17)
+        freeCompilerArgs.add("-Xjsr305=strict")
+    }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeApiModels(): String =
    """
+/**
+ * Kaero runtime response models.
+ *
+ * Kaero standardizes HTTP responses as either:
+ * - success: `{ \"data\": ... }`
+ * - error: `{ \"error\": { code, message, details? } }`
+ */
+package dev.kaero.runtime
+
+import io.ktor.http.HttpStatusCode
+import io.ktor.util.reflect.TypeInfo
+import io.ktor.util.reflect.typeInfo
+import kotlinx.serialization.Serializable
+import kotlinx.serialization.json.JsonElement
+
+@Serializable
+data class ApiSuccess<T>(val data: T)
+
+@Serializable
+data class ApiError(val error: ErrorBody)
+
+@Serializable
+data class ErrorBody(
+    val code: String,
+    val message: String,
+    val details: JsonElement? = null,
+)
+
+sealed interface KaeroOut {
+    val status: HttpStatusCode
+}
+
+data class KaeroTyped(
+    override val status: HttpStatusCode,
+    val body: Any,
+    val typeInfo: TypeInfo,
+) : KaeroOut
+
+data object KaeroNoContent : KaeroOut {
+    override val status: HttpStatusCode = HttpStatusCode.NoContent
+}
+
+@PublishedApi
+internal inline fun <reified T> kaeroSuccessTypeInfo(): TypeInfo = typeInfo<ApiSuccess<T>>()
+
+@PublishedApi
+internal fun kaeroErrorTypeInfo(): TypeInfo = typeInfo<ApiError>()
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeCtx(): String =
    """
+/**
+ * Request context passed to handlers.
+ */
+package dev.kaero.runtime
+
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.application.ApplicationCall
+import io.ktor.server.request.receive
+import kotlinx.serialization.json.JsonElement
+
+class Ctx(
+    val call: ApplicationCall,
+) {
+    val params: Map<String, String>
+        get() = call.parameters.names().associateWith { name -> call.parameters[name].orEmpty() }
+
+    val query: Map<String, List<String>>
+        get() = call.request.queryParameters.names().associateWith { name -> call.request.queryParameters.getAll(name).orEmpty() }
+
+    suspend inline fun <reified T : Any> body(): T = call.receive<T>()
+
+    inline fun <reified T> ok(data: T): KaeroOut =
+        KaeroTyped(HttpStatusCode.OK, ApiSuccess(data), kaeroSuccessTypeInfo<T>())
+
+    inline fun <reified T> created(data: T): KaeroOut =
+        KaeroTyped(HttpStatusCode.Created, ApiSuccess(data), kaeroSuccessTypeInfo<T>())
+
+    fun noContent(): KaeroOut = KaeroNoContent
+
+    fun fail(
+        code: String,
+        message: String,
+        details: JsonElement? = null,
+        status: HttpStatusCode = HttpStatusCode.BadRequest,
+    ): KaeroOut = KaeroTyped(
+        status,
+        ApiError(ErrorBody(code = code, message = message, details = details)),
+        kaeroErrorTypeInfo(),
+    )
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeController(): String =
    """
+package dev.kaero.runtime
+
+import io.ktor.http.HttpStatusCode
+import kotlinx.serialization.json.JsonElement
+
+/**
+ * Base controller (Symfony/Laravel style).
+ *
+ * - Methods do not need to accept [Ctx] as a parameter.
+ * - Kaero creates one controller instance per request and injects the context.
+ */
+abstract class KaeroController {
+    @PublishedApi
+    internal lateinit var ctx: Ctx
+
+    @PublishedApi
+    internal fun bind(ctx: Ctx) {
+        this.ctx = ctx
+    }
+
+    protected val params: Map<String, String> get() = ctx.params
+    protected val query: Map<String, List<String>> get() = ctx.query
+
+    protected fun param(name: String): String? = params[name]
+    protected fun paramInt(name: String): Int? = params[name]?.toIntOrNull()
+
+    protected suspend inline fun <reified T : Any> body(): T = ctx.body<T>()
+
+    protected inline fun <reified T> ok(data: T): KaeroOut = ctx.ok(data)
+    protected inline fun <reified T> created(data: T): KaeroOut = ctx.created(data)
+    protected fun noContent(): KaeroOut = ctx.noContent()
+
+    protected fun fail(
+        code: String,
+        message: String,
+        details: JsonElement? = null,
+        status: HttpStatusCode = HttpStatusCode.BadRequest,
+    ): KaeroOut = ctx.fail(code = code, message = message, details = details, status = status)
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeRouter(): String =
    """
+/**
+ * Kaero routing.
+ */
+package dev.kaero.runtime
+
+import dev.kaero.runtime.annotations.Delete
+import dev.kaero.runtime.annotations.Get
+import dev.kaero.runtime.annotations.Patch
+import dev.kaero.runtime.annotations.Post
+import dev.kaero.runtime.annotations.Put
+import kotlin.reflect.full.callSuspend
+import kotlin.reflect.full.createInstance
+import kotlin.reflect.full.findAnnotation
+import kotlin.reflect.full.memberFunctions
+
+enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }
+
+data class RouteDef(
+    val method: HttpMethod,
+    val path: String,
+    val handler: suspend (Ctx) -> Any?,
+)
+
+class KaeroRouter {
+    @PublishedApi
+    internal val routes: MutableList<RouteDef> = mutableListOf()
+
+    /** Registers all routes from annotations on a controller. */
+    inline fun <reified C> controller() where C : KaeroController {
+        val kClass = C::class
+
+        for (fn in kClass.memberFunctions) {
+            val get = fn.findAnnotation<Get>()
+            val post = fn.findAnnotation<Post>()
+            val put = fn.findAnnotation<Put>()
+            val patch = fn.findAnnotation<Patch>()
+            val delete = fn.findAnnotation<Delete>()
+
+            val (method, path) = when {
+                get != null -> HttpMethod.GET to get.path
+                post != null -> HttpMethod.POST to post.path
+                put != null -> HttpMethod.PUT to put.path
+                patch != null -> HttpMethod.PATCH to patch.path
+                delete != null -> HttpMethod.DELETE to delete.path
+                else -> continue
+            }
+
+            if (fn.parameters.size != 1) {
+                throw IllegalStateException(
+                    "@Route method ${'$'}{kClass.qualifiedName}.${'$'}{fn.name} must not declare parameters",
+                )
+            }
+
+            routes += RouteDef(method, path) { ctx ->
+                val controller = runCatching { kClass.createInstance() }
+                    .getOrElse { e ->
+                        throw IllegalStateException(
+                            "Controller ${'$'}{kClass.qualifiedName} must have a public no-arg constructor",
+                            e,
+                        )
+                    }
+
+                controller.bind(ctx)
+                fn.callSuspend(controller)
+            }
+        }
+    }
+}
+
+internal fun normalizePath(path: String): String {
+    val withSlash = if (path.startsWith('/')) path else "/${'$'}path"
+    return withSlash.replace(Regex(":([A-Za-z0-9_]+)")) { m -> "{${'$'}{m.groupValues[1]}}" }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeKtor(): String =
    """
+/**
+ * Kaero runtime Ktor integration.
+ */
+package dev.kaero.runtime
+
+import io.ktor.http.ContentType
+import io.ktor.http.HttpStatusCode
+import io.ktor.server.application.Application
+import io.ktor.server.application.call
+import io.ktor.server.application.install
+import io.ktor.server.plugins.callloging.CallLogging
+import io.ktor.server.plugins.compression.Compression
+import io.ktor.server.plugins.compression.deflate
+import io.ktor.server.plugins.compression.gzip
+import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
+import io.ktor.server.plugins.cors.routing.CORS
+import io.ktor.server.plugins.statuspages.StatusPages
+import io.ktor.server.request.path
+import io.ktor.server.response.respond
+import io.ktor.server.response.respondFile
+import io.ktor.server.routing.Route
+import io.ktor.server.routing.delete
+import io.ktor.server.routing.get
+import io.ktor.server.routing.patch
+import io.ktor.server.routing.post
+import io.ktor.server.routing.put
+import io.ktor.server.routing.routing
+import io.ktor.serialization.kotlinx.json.json
+import io.ktor.util.logging.KtorSimpleLogger
+import kotlinx.serialization.ExperimentalSerializationApi
+import kotlinx.serialization.json.Json
+import java.io.File
+
+data class KaeroConfig(
+    val corsAnyHost: Boolean = true,
+    val serveFrontendDist: File? = null,
+    val spaFallback: Boolean = true,
+)
+
+private val logger = KtorSimpleLogger("Kaero")
+
+fun Application.installKaero(router: KaeroRouter, config: KaeroConfig = KaeroConfig()) {
+    install(CallLogging)
+    install(Compression) {
+        gzip()
+        deflate()
+    }
+
+    install(ContentNegotiation) {
+        @OptIn(ExperimentalSerializationApi::class)
+        json(
+            Json {
+                ignoreUnknownKeys = true
+                explicitNulls = false
+            },
+            contentType = ContentType.Application.Json,
+        )
+    }
+
+    install(CORS) {
+        if (config.corsAnyHost) anyHost()
+        allowMethod(io.ktor.http.HttpMethod.Get)
+        allowMethod(io.ktor.http.HttpMethod.Post)
+        allowMethod(io.ktor.http.HttpMethod.Put)
+        allowMethod(io.ktor.http.HttpMethod.Patch)
+        allowMethod(io.ktor.http.HttpMethod.Delete)
+        allowHeader(io.ktor.http.HttpHeaders.ContentType)
+        allowHeader(io.ktor.http.HttpHeaders.Authorization)
+    }
+
+    install(StatusPages) {
+        exception<Throwable> { call, cause ->
+            logger.error("Unhandled exception on ${'$'}{call.request.path()}", cause)
+            call.respond(
+                HttpStatusCode.InternalServerError,
+                ApiError(ErrorBody(code = "INTERNAL_ERROR", message = "Internal error")),
+            )
+        }
+    }
+
+    routing {
+        registerKaeroRoutes(router)
+        serveFrontendIfConfigured(config)
+    }
+}
+
+private fun Route.registerKaeroRoutes(router: KaeroRouter) {
+    for (route in router.routes) {
+        val ktorPath = normalizePath(route.path)
+        when (route.method) {
+            HttpMethod.GET -> get(ktorPath) { handleKaero(call, route.handler) }
+            HttpMethod.POST -> post(ktorPath) { handleKaero(call, route.handler) }
+            HttpMethod.PUT -> put(ktorPath) { handleKaero(call, route.handler) }
+            HttpMethod.PATCH -> patch(ktorPath) { handleKaero(call, route.handler) }
+            HttpMethod.DELETE -> delete(ktorPath) { handleKaero(call, route.handler) }
+        }
+    }
+}
+
+private suspend fun handleKaero(call: io.ktor.server.application.ApplicationCall, handler: suspend (Ctx) -> Any?) {
+    val out = handler(Ctx(call))
+
+    when (out) {
+        is KaeroTyped -> call.respond(out.status, out.body, out.typeInfo)
+        is KaeroNoContent -> call.respond(HttpStatusCode.NoContent)
+        null -> call.respond(HttpStatusCode.NoContent)
+        else -> call.respond(HttpStatusCode.OK, ApiSuccess(out))
+    }
+}
+
+private fun Route.serveFrontendIfConfigured(config: KaeroConfig) {
+    val distDir = config.serveFrontendDist ?: return
+    if (!distDir.exists() || !distDir.isDirectory) return
+
+    get("/") {
+        val index = File(distDir, "index.html")
+        if (index.exists()) call.respondFile(index) else call.respond(HttpStatusCode.NotFound)
+    }
+
+    get("/{...}") {
+        val path = call.parameters.getAll("...")?.joinToString("/") ?: ""
+        val file = File(distDir, path)
+
+        if (file.exists() && file.isFile) {
+            call.respondFile(file)
+            return@get
+        }
+
+        if (config.spaFallback) {
+            val index = File(distDir, "index.html")
+            if (index.exists()) {
+                call.respondFile(index)
+                return@get
+            }
+        }
+
+        call.respond(HttpStatusCode.NotFound)
+    }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateRuntimeHttpAnnotations(): String =
    """
+/**
+ * HTTP method annotations used by Kaero's controller scanner.
+ */
+package dev.kaero.runtime.annotations
+
+@Target(AnnotationTarget.FUNCTION)
+@Retention(AnnotationRetention.RUNTIME)
+annotation class Get(val path: String)
+
+@Target(AnnotationTarget.FUNCTION)
+@Retention(AnnotationRetention.RUNTIME)
+annotation class Post(val path: String)
+
+@Target(AnnotationTarget.FUNCTION)
+@Retention(AnnotationRetention.RUNTIME)
+annotation class Put(val path: String)
+
+@Target(AnnotationTarget.FUNCTION)
+@Retention(AnnotationRetention.RUNTIME)
+annotation class Patch(val path: String)
+
+@Target(AnnotationTarget.FUNCTION)
+@Retention(AnnotationRetention.RUNTIME)
+annotation class Delete(val path: String)
+""".trimIndent().trimStart('+').replace("\n+", "\n")

// ---------------- templates (app) ----------------

private fun templateAppBuild(appName: String): String =
    """
+// Generated Kaero app module.
+
+import org.jetbrains.kotlin.gradle.dsl.JvmTarget
+import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
+
+val ktorVersion: String by project
+val serializationVersion: String by project
+
+plugins {
+    kotlin("jvm")
+    kotlin("plugin.serialization")
+    application
+}
+
+kotlin {
+    jvmToolchain(25)
+}
+
+group = "dev.kaero"
+version = (project.findProperty("kaeroVersion") as String?) ?: "0.1.0-SNAPSHOT"
+
+java {
+    toolchain {
+        languageVersion.set(JavaLanguageVersion.of(25))
+    }
+
+    sourceCompatibility = JavaVersion.VERSION_17
+    targetCompatibility = JavaVersion.VERSION_17
+}
+
+application {
+    mainClass.set("dev.kaero.app.MainKt")
+}
+
+dependencies {
+    implementation(project(":kaero-runtime"))
+
+    implementation("io.ktor:ktor-server-core-jvm:${'$'}ktorVersion")
+    implementation("io.ktor:ktor-server-netty-jvm:${'$'}ktorVersion")
+    implementation("io.ktor:ktor-server-content-negotiation-jvm:${'$'}ktorVersion")
+    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:${'$'}ktorVersion")

+    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${'$'}serializationVersion")
+
+    runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
+
+    testImplementation(kotlin("test"))
+}
+
+tasks.withType<KotlinCompile>().configureEach {
+    compilerOptions {
+        jvmTarget.set(JvmTarget.JVM_17)
+        freeCompilerArgs.add("-Xjsr305=strict")
+    }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateAppMain(): String =
    """
+/**
+ * Generated Kaero application entrypoint.
+ */
+package dev.kaero.app
+
+import dev.kaero.runtime.KaeroConfig
+import dev.kaero.runtime.KaeroRouter
+import dev.kaero.runtime.installKaero
+import dev.kaero.app.kaero.registerRoutes
+import io.ktor.server.engine.embeddedServer
+import io.ktor.server.netty.Netty
+import java.io.File
+
+fun main() {
+    val router = KaeroRouter().apply {
+        registerRoutes()
+    }
+
+    val dist = File("frontend/dist")
+
+    embeddedServer(Netty, port = 8080) {
+        installKaero(
+            router,
+            KaeroConfig(
+                corsAnyHost = true,
+                serveFrontendDist = dist.takeIf { it.exists() },
+                spaFallback = true,
+            ),
+        )
+    }.start(wait = true)
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateAppRoutes(): String =
    """
+/**
+ * Central routing entrypoint.
+ */
+package dev.kaero.app.kaero
+
+import dev.kaero.runtime.KaeroRouter
+import dev.kaero.app.app.controllers.TodosController
+
+fun KaeroRouter.registerRoutes() {
+    controller<TodosController>()
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateTodosController(): String =
    """
+package dev.kaero.app.app.controllers
+
+import dev.kaero.runtime.KaeroController
+import dev.kaero.runtime.annotations.Delete
+import dev.kaero.runtime.annotations.Get
+import dev.kaero.runtime.annotations.Patch
+import dev.kaero.runtime.annotations.Post
+import dev.kaero.app.app.services.TodoService
+import io.ktor.http.HttpStatusCode
+import kotlinx.serialization.Serializable
+
+/**
+ * Simple CRUD controller demonstrating Kaero's conventions.
+ */
+class TodosController : KaeroController() {
+
+    @Serializable
+    data class CreateTodoBody(val title: String)
+
+    @Get("/api/todos")
+    suspend fun index() = ok(TodoService.list())
+
+    @Get("/api/todos/:id")
+    suspend fun show(): Any {
+        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")
+
+        return TodoService.find(id)
+            ?.let { ok(it) }
+            ?: fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
+    }
+
+    @Post("/api/todos")
+    suspend fun create(): Any {
+        val body = runCatching { body<CreateTodoBody>() }
+            .getOrElse { return fail("VALIDATION_ERROR", "Invalid JSON body") }
+
+        if (body.title.isBlank()) {
+            return fail("VALIDATION_ERROR", "Title is required")
+        }
+
+        val todo = TodoService.create(body.title)
+        return created(todo)
+    }
+
+    @Patch("/api/todos/:id")
+    suspend fun toggle(): Any {
+        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")
+
+        return TodoService.toggle(id)
+            ?.let { ok(it) }
+            ?: fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
+    }
+
+    @Delete("/api/todos/:id")
+    suspend fun delete(): Any {
+        val id = paramInt("id") ?: return fail("VALIDATION_ERROR", "Invalid id")
+
+        val deleted = TodoService.delete(id)
+        return if (deleted) noContent() else fail("NOT_FOUND", "Todo not found", status = HttpStatusCode.NotFound)
+    }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateTodoService(): String =
    """
+package dev.kaero.app.app.services
+
+import dev.kaero.app.app.models.Todo
+import java.util.concurrent.atomic.AtomicInteger
+
+/**
+ * In-memory Todo service (no DB).
+ */
+object TodoService {
+    private val idSeq = AtomicInteger(0)
+    private val todos = mutableListOf<Todo>()
+
+    fun list(): List<Todo> = todos.toList()
+    fun find(id: Int): Todo? = todos.firstOrNull { it.id == id }
+
+    fun create(title: String): Todo {
+        val todo = Todo(id = idSeq.incrementAndGet(), title = title, done = false)
+        todos += todo
+        return todo
+    }
+
+    fun toggle(id: Int): Todo? {
+        val idx = todos.indexOfFirst { it.id == id }
+        if (idx < 0) return null
+
+        val updated = todos[idx].copy(done = !todos[idx].done)
+        todos[idx] = updated
+        return updated
+    }
+
+    fun delete(id: Int): Boolean = todos.removeIf { it.id == id }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateTodoModel(): String =
    """
+package dev.kaero.app.app.models
+
+import kotlinx.serialization.Serializable
+
+@Serializable
+data class Todo(
+    val id: Int,
+    val title: String,
+    val done: Boolean,
+)
+""".trimIndent().trimStart('+').replace("\n+", "\n")

// ---------------- templates (frontend) ----------------

private fun templateFrontendPackageJson(): String =
    """
+{
+  "name": "kaero-frontend",
+  "private": true,
+  "version": "0.1.0",
+  "type": "module",
+  "scripts": {
+    "dev": "bunx --bun vite",
+    "build": "bunx --bun vite build",
+    "preview": "bunx --bun vite preview"
+  },
+  "dependencies": {
+    "pinia": "^2.3.0",
+    "vue": "^3.4.0"
+  },
+  "devDependencies": {
+    "@vitejs/plugin-vue": "^5.2.0",
+    "typescript": "^5.6.0",
+    "vite": "^5.0.0",
+    "vue-tsc": "^2.1.0"
+  }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateViteConfig(): String =
    """
+/**
+ * Vite config.
+ *
+ * Proxies /api to the backend on :8080.
+ */
+import { defineConfig } from "vite";
+import vue from "@vitejs/plugin-vue";
+
+export default defineConfig({
+  plugins: [vue()],
+  server: {
+    proxy: {
+      "/api": {
+        target: "http://localhost:8080",
+        changeOrigin: true,
+      },
+    },
+  },
+});
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateIndexHtml(appName: String): String =
    """
+<!doctype html>
+<html lang="en">
+  <head>
+    <meta charset="UTF-8" />
+    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
+    <title>$appName</title>
+  </head>
+  <body>
+    <div id="app"></div>
+    <script type="module" src="/src/main.ts"></script>
+  </body>
+</html>
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateFrontendMainTs(): String =
    """
+/**
+ * Frontend entrypoint.
+ */
+import { createApp } from "vue";
+import { createPinia } from "pinia";
+import App from "./App.vue";
+
+import { initRouting } from "./app/core/routing/router";
+
+const pinia = createPinia();
+initRouting(pinia);
+
+createApp(App).use(pinia).mount("#app");
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateFrontendAppVue(): String =
    """
+<template>
+  <AppView />
+</template>
+
+<script setup lang="ts">
+// Root component kept intentionally minimal.
+import AppView from "./app/app.view.vue";
+</script>
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateFrontendRouterTs(): String =
    """
+import { defineStore } from "pinia";
+import type { Pinia } from "pinia";
+
+/**
+ * CORE / ROUTING (Angular-style "core")
+ *
+ * This project intentionally avoids vue-router.
+ * We keep a tiny router in Pinia and sync it with `location.hash`.
+ */
+
+export type ViewRoute = { name: "counter" };
+
+function parseHash(hash: string): ViewRoute {
+  const clean = hash.replace(/^#/, "");
+  if (clean === "/counter" || clean === "counter") return { name: "counter" };
+  return { name: "counter" };
+}
+
+function toHash(route: ViewRoute): string {
+  switch (route.name) {
+    case "counter":
+      return "#/counter";
+  }
+}
+
+export const useRouterStore = defineStore("router", {
+  state: () => ({
+    current: parseHash(globalThis.location?.hash ?? "") as ViewRoute,
+  }),
+  actions: {
+    syncFromHash() {
+      this.current = parseHash(globalThis.location?.hash ?? "");
+    },
+    push(route: ViewRoute) {
+      this.current = route;
+      if (globalThis.location) globalThis.location.hash = toHash(route);
+    },
+  },
+});
+
+export function initRouting(pinia: Pinia) {
+  const router = useRouterStore(pinia);
+
+  router.syncFromHash();
+  globalThis.addEventListener?.("hashchange", () => router.syncFromHash());
+
+  if (globalThis.location && !globalThis.location.hash) {
+    router.push(router.current);
+  }
+}
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateFrontendAppViewVue(): String =
    """
+<script setup lang="ts">
+import { computed } from "vue";
+
+import { useRouterStore } from "./core/routing/router";
+import CounterPage from "./features/counter/counter.page.vue";
+
+const router = useRouterStore();
+const currentView = computed(() => router.current.name);
+</script>
+
+<template>
+  <CounterPage v-if="currentView === 'counter'" />
+</template>
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateCounterServiceTs(): String =
    """
+/**
+ * Counter feature service.
+ */
+export const counterService = {
+  increment(current: number) {
+    return current + 1;
+  },
+} as const;
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateCounterStoreTs(): String =
    """
+import { defineStore } from "pinia";
+import { counterService } from "./counter.service";
+
+/**
+ * Counter feature store.
+ */
+export const useCounterStore = defineStore("counter", {
+  state: () => ({
+    value: 0,
+  }),
+  actions: {
+    increment() {
+      this.value = counterService.increment(this.value);
+    },
+  },
+});
+""".trimIndent().trimStart('+').replace("\n+", "\n")

private fun templateCounterPageVue(): String =
    """
+<script setup lang="ts">
+import { computed } from "vue";
+
+import { useCounterStore } from "./counter.store";
+
+const counter = useCounterStore();
+const value = computed(() => counter.value);
+
+function increment() {
+  counter.increment();
+}
+</script>
+
+<template>
+  <main style="max-width: 680px; margin: 40px auto; font-family: system-ui, sans-serif;">
+    <h1>Counter</h1>
+
+    <p style="font-size: 20px;">Value: <strong>{{ value }}</strong></p>
+
+    <button @click="increment" style="padding: 8px 12px;">+1</button>
+  </main>
+</template>
+""".trimIndent().trimStart('+').replace("\n+", "\n")
