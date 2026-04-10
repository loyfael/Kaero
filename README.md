<p align="center">
  <img src="logo.png" alt="Kaero logo" width="320">
</p>

> [!WARNING]
> Kaero is a purely experimental framework prototype. It is not production-ready and its APIs, tooling, conventions, and project structure may change significantly.

# Kaero

Kaero is an opinionated full-stack framework prototype built around a simple idea: keep the backend and frontend easy to understand, wire them together with strong conventions, and remove as much boilerplate as possible.

Its architecture is strongly inspired by Angular on both sides of the stack:

- on the frontend, through a feature-oriented structure with clear separation between pages, stores, services, and core infrastructure
- on the backend, through a convention-driven application layout centered around controllers, routing, and explicit application modules

In this repository, Kaero currently consists of:

- a Kotlin runtime for Ktor applications
- a small JVM CLI prototype
- a repository-level Bash helper for local development
- a sample full-stack app using Kotlin, Vue 3, Vite, TypeScript, and Pinia

This README documents what is implemented today, not a future roadmap.

## What Kaero Is Today

Kaero is not yet a finished framework distribution. Right now, this repository is a working monorepo that demonstrates:

- annotation-based HTTP controllers on top of Ktor
- a central routing entrypoint
- a standardized JSON response format
- a minimal full-stack development workflow
- a sample app with a Todo CRUD backend and a Vue frontend

The architecture is intentionally simple and beginner-friendly.

## Repository Layout

```text
.
├── kaero                 # Bash helper for local repo development
├── dev.sh                # Compatibility wrapper for `./kaero dev`
├── kaero-cli/            # Cross-platform JVM CLI prototype
├── kaero-runtime/        # Runtime library used by apps
├── sample-app/           # Example application built with Kaero
├── build.gradle.kts      # Root Gradle config
├── settings.gradle.kts   # Multi-module settings
└── README.md
```

### Modules

#### `kaero-runtime`

The runtime module provides the core building blocks:

- `KaeroRouter`: route registration and controller discovery
- `KaeroController`: base class with request helpers
- `Ctx`: request context with body parsing and response helpers
- `installKaero(...)`: Ktor integration with JSON, CORS, compression, error mapping, and optional frontend static serving

#### `kaero-cli`

The JVM CLI currently provides two commands:

- `kaero doctor`: checks required tools and versions
- `kaero init <directory>`: creates a new standalone Kaero project

This CLI is cross-platform because it runs on the JVM.

#### `sample-app`

The sample app demonstrates the current Kaero workflow:

- Kotlin backend on Ktor
- annotation-based controller for Todo CRUD
- Vue 3 + Vite frontend
- Pinia stores and services
- frontend proxy to `/api`
- production mode where the backend serves the built frontend

## Requirements

The repository currently targets these tool versions:

- Java 25
- Gradle `>= 9.3.0`
- Bun `>= 1.2.0`

Notes:

- The Gradle build uses the Java 25 toolchain.
- Kotlin bytecode targets JVM 17, but Java 25 is still required to build and run this repo as configured.
- The Bash helper script also expects a Unix-like shell environment.

## Quick Start

There are two practical ways to run the sample app.

### Option 1: Cross-platform setup

This is the clearest option and works without the repository Bash helper.

If you are on Windows, this is the recommended workflow for running the sample app from this repository.

#### 1. Start the backend

From the repository root:

```bash
gradle :sample-app:run
```

Backend URL:

```text
http://localhost:8080
```

#### 2. Start the frontend

In another terminal:

```bash
cd sample-app/frontend
bun install
bun run dev
```

Frontend URL:

```text
http://localhost:5173
```

In development, Vite proxies `/api` requests to `http://localhost:8080`.

### Option 2: Repository helper script

If you are using Linux, macOS, WSL, or Git Bash, you can use the Bash helper from the repository root:

```bash
./kaero dev
```

This starts:

- the Ktor backend on port `8080`
- the Vite frontend on port `5173`

To stop both, press `Ctrl+C`.

## Development Commands

### Repository Bash helper

From the repository root:

```bash
./kaero dev
./kaero backend
./kaero frontend
./kaero build
./kaero test
./kaero new my-app
./kaero gen routes
./kaero logs backend
./kaero logs frontend
```

Useful options:

```bash
./kaero dev --app sample-app
./kaero dev --debug
./kaero dev --kill-ports
```

What these commands do:

- `dev`: starts backend, waits for readiness, then starts the frontend
- `backend`: starts only the backend
- `frontend`: starts only the frontend
- `build`: runs the backend build and frontend production build
- `test`: runs backend tests and frontend type checking
- `new`: copies `sample-app` into a new module and registers it in `settings.gradle.kts`
- `gen routes`: extracts annotated routes into `.kaero/generated/`
- `logs`: prints recent backend or frontend logs

Important: this helper is a Bash script. It is convenient, but it is not the same thing as the JVM CLI in `kaero-cli`.

### JVM CLI prototype

Build the CLI:

```bash
gradle :kaero-cli:installDist
```

Run it:

```bash
./kaero-cli/build/install/kaero/bin/kaero doctor
./kaero-cli/build/install/kaero/bin/kaero init ./my-app
```

On Windows, use the generated batch launcher instead:

```powershell
.\kaero-cli\build\install\kaero\bin\kaero.bat doctor
.\kaero-cli\build\install\kaero\bin\kaero.bat init .\my-app
```

Current JVM CLI commands:

- `doctor`: validates Bun, Java, and Gradle versions
- `init <directory>`: creates a standalone multi-module Kaero project with a runtime module and an application module

## How the Backend Works

Kaero keeps backend routing intentionally centralized.

### Routing

Each app has a single routing entrypoint. In the sample app, that entrypoint is:

```text
sample-app/src/main/kotlin/dev/kaero/sample/kaero/routes.kt
```

The sample app registers controllers like this:

```kotlin
fun KaeroRouter.registerRoutes() {
    controller<TodosController>()
}
```

### Controllers

Controllers extend `KaeroController` and use route annotations:

```kotlin
class TodosController : KaeroController() {
    @Get("/api/todos")
    suspend fun index() = ok(TodoService.list())
}
```

Controller methods do not need to receive a request context parameter directly. The base controller exposes helpers such as:

- `param(name)`
- `paramInt(name)`
- `body<T>()`
- `ok(data)`
- `created(data)`
- `noContent()`
- `fail(...)`

### Supported HTTP annotations

The runtime supports these method annotations:

- `@Get`
- `@Post`
- `@Put`
- `@Patch`
- `@Delete`

Route paths use `:param` syntax in controller annotations and are normalized internally for Ktor.

Example:

```kotlin
@Get("/api/todos/:id")
```

### Request context and responses

The `Ctx` object wraps Ktor's `ApplicationCall` and provides:

- route params
- query params
- JSON body parsing
- standard success and error helpers

### Response format

Kaero standardizes JSON responses.

Success response:

```json
{
  "data": {
    "id": 1,
    "title": "Buy milk",
    "done": false
  }
}
```

Error response:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Title is required",
    "details": null
  }
}
```

Unhandled exceptions are mapped to:

- HTTP `500`
- error code `INTERNAL_ERROR`

### Ktor integration provided by the runtime

`installKaero(...)` installs and configures:

- request logging
- gzip and deflate compression
- JSON serialization with Kotlinx Serialization
- CORS
- status page error mapping
- route registration
- optional static frontend serving with SPA fallback

## Sample API

The sample app exposes a Todo API through `TodosController`.

### Routes

- `GET /api/todos`
- `GET /api/todos/:id`
- `POST /api/todos`
- `PATCH /api/todos/:id`
- `DELETE /api/todos/:id`

### Example requests

Create a todo:

```http
POST /api/todos
Content-Type: application/json

{
  "title": "Write documentation"
}
```

Toggle a todo:

```http
PATCH /api/todos/1
```

Delete a todo:

```http
DELETE /api/todos/1
```

## How the Frontend Works

The sample frontend is intentionally minimal.

### Stack

- Vue 3
- Vite
- TypeScript
- Pinia
- Bun

### Routing

The frontend does not use Vue Router.

Instead, it uses a tiny Pinia-based hash router with routes such as:

- `#/counter`
- `#/todos`

This keeps the frontend easy to understand and refresh-safe without introducing a larger routing layer.

### Feature structure

Frontend code is organized in an Angular-like way:

```text
src/app/
├── core/                 # infrastructure such as routing
├── features/counter/     # page + store + service
└── features/todos/       # page + store + service
```

A feature typically contains:

- `*.page.vue`: UI
- `*.store.ts`: state and actions
- `*.service.ts`: domain logic or HTTP calls

### API calls

The Todos feature talks directly to backend endpoints from its service.

In development, `/api` is proxied by Vite to the backend.

## Production Build

To build the frontend for production:

```bash
cd sample-app/frontend
bun run build
```

Then start the backend:

```bash
gradle :sample-app:run
```

If `sample-app/frontend/dist` exists, the backend serves it as static files and falls back to `index.html` for SPA routes.

## Creating a New App

There are currently two different creation workflows.

### 1. Create a new module inside this monorepo

Using the repository helper:

```bash
./kaero new my-app
```

This:

- copies `sample-app`
- removes generated frontend/backend build artifacts
- updates the new module README title
- appends the new module to `settings.gradle.kts`

Then you can run:

```bash
./kaero dev --app my-app
```

### 2. Create a standalone project outside this monorepo

Using the JVM CLI:

```bash
./kaero-cli/build/install/kaero/bin/kaero init ./my-app
```

This creates a new Gradle project containing:

- a `kaero-runtime` module
- one application module
- Gradle wrapper files
- a starter frontend

## Current Design Principles

The current codebase consistently pushes toward these constraints:

- one central routing entrypoint per app
- annotation-based controllers
- a small controller API
- one standard JSON envelope for success and error responses
- simple frontend architecture with explicit stores and services

The result is not a large abstraction layer. It is a small, opinionated setup intended to stay readable.

## Current Limitations

This repository is still an early-stage prototype. A few points are important to understand:

- the root `./kaero` helper is Bash-only
- the JVM CLI currently exposes only `doctor` and `init`
- the sample app is in-memory and does not include a real database yet
- the route generator is best-effort text extraction from annotations
- the framework conventions are more complete than the tooling around them

## Suggested Reading

If you want more detail, these files are the most useful starting points:

- `sample-app/README.md`
- `sample-app/frontend/README.md`
- `kaero-runtime/src/main/kotlin/dev/kaero/runtime/Kaero.kt`
- `kaero-runtime/src/main/kotlin/dev/kaero/runtime/KaeroRouter.kt`
- `sample-app/src/main/kotlin/dev/kaero/sample/app/controllers/TodosController.kt`

## License

This project is distributed under the terms of the license included in `LICENSE`.

# 📘 Kaero Framework — Spec & Current Repo State

## 1. Project overview

### 1.1 Name

**Kaero**

### 1.2 Positioning

An opinionated fullstack framework combining:

- **Frontend:** Vue 3 + Vite
- **Backend:** Kotlin (Ktor)
- **Architecture:** RMC (Routes – Models – Controllers)

### 1.3 Problem statement

Traditional Angular + Java stacks tend to be:

- Verbose
- Over-architected
- Slow to bootstrap
- Overkill for ~80% of projects

Kaero targets this goal:

> Build a fast, coherent fullstack app in minutes, with a minimal architecture and strong defaults.

### 1.4 Core goals

- Extreme simplicity
- Strong conventions
- Native performance
- Instant productivity
- Smooth developer experience

---

## 2. Product vision

Kaero should eventually enable:

```bash
kaero new my-app
cd my-app
kaero dev
```

### Dev CLI (this repo)

In this repository, a **prototype** CLI already exists:

```bash
./kaero dev
```

This repo also contains a **cross-platform JVM CLI** (Windows/macOS/Linux) in the `kaero-cli` module.

Build it:

```bash
gradle :kaero-cli:installDist
```

Run it:

```bash
./kaero-cli/build/install/kaero/bin/kaero doctor
./kaero-cli/build/install/kaero/bin/kaero init ./my-app
```

To distribute it as an archive (for a global install), you can also build `distZip` and add the extracted `bin/` folder to your `PATH`.

Other useful commands:

- `./kaero new <name>`: creates a new Gradle module by copying the template
- `./kaero test`: backend tests + frontend typecheck
- `./kaero gen routes`: best-effort export of annotated routes to `.kaero/generated/`

The template app is expected to work immediately with:

- A working backend
- A connected Vue frontend
- A working CRUD example
- (Later) a generated TypeScript API client

---

## 3. Global architecture

### 3.1 Tech stack

**Backend**

- Kotlin
- Ktor (Netty)
- Kotlinx Serialization
- Coroutines

**Frontend**

- Vue 3
- Vite
- TypeScript
- Pinia

Note: this repo intentionally uses a tiny Pinia-based hash router (no Vue Router) to keep things beginner-friendly.

**Database**

- SQLite by default (future)
- PostgreSQL support (future)

---

## 4. Kaero architecture (RMC)

Kaero uses a simplified model inspired by MVC.

### 4.1 Standard project structure (target)

```
my-app/
│
├── kaero/
│   ├── routes.kt
│   └── config.yml
│
├── app/
│   ├── controllers/
│   ├── models/
│   ├── policies/
│   └── services/
│
├── frontend/
│   └── (Vue app)
│
├── build.gradle.kts
└── settings.gradle.kts
```

**Fundamental rule:** zero mandatory configuration. Everything works by convention.

---

## 5. Backend

### 5.1 Routing

Routes are declared in:

```
kaero/routes.kt
```

This repo currently uses **annotation-based controllers** registered from `kaero/routes.kt`.

Constraints:

- Single central routing entry point
- REST-first

---

### 5.2 Controllers

Controllers use a Symfony/Laravel-like base controller (`KaeroController`) so methods do not need to accept `Ctx` directly.

The context provides:

- params / query
- `body<T>()`
- response helpers: `ok()`, `created()`, `noContent()`, `fail()`

---

### 5.3 Standard response format

**Success**

```json
{ "data": "…" }
```

**Error**

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": {}
  }
}
```

This format is intended to be the only one.

---

## 6. Frontend

### 6.1 Vue + Vite

In development:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Vite proxies `/api` to the backend

In production:

- Build Vue → `frontend/dist`
- Backend serves static files
- SPA fallback to `index.html`

---

### 6.2 TypeScript API client generation

Planned: Kaero will generate a typed TS API client in the frontend.

---

## 7. Kaero CLI

### 7.1 Installation

Planned methods:

- `curl` install script
- Homebrew
- Scoop
- SDKMAN

Example:

```bash
curl -fsSL https://kaero.dev/install.sh | sh
```

---

### 7.2 Main commands

#### `kaero new`

Creates a full project.

```bash
kaero new my-app
```

Options (planned):

- `--db sqlite|postgres|none`
- `--auth jwt|none`

---

#### `kaero dev`

- Starts backend
- Starts Vite
- Configures the dev proxy

---

#### `kaero build`

- Builds frontend
- Builds backend JAR
- Generates TS client (planned)

---

#### `kaero start`

- Starts the production app

---

## 8. Performance

### 8.1 Backend

- Fast cold start
- Kotlinx serialization
- Compression enabled
- Static cache headers
- Optimized DB pooling
- Native coroutines

### 8.2 Frontend

- Optimized Vite build
- Code splitting
- Compressed assets
- ETag enabled

---

## 9. Security

V1 includes:

- Configurable CORS
- Simple JWT
- `auth()` middleware
- Input validation
- Safe JSON parsing

---

## 10. Database

V1:

- SQLite by default
- Lightweight ORM (Exposed or Ktorm)
- Simplified migrations

Future command:

```bash
kaero generate model User
```

---

## 11. Code generation

V1 plans:

```bash
kaero generate controller Users
kaero generate model User
kaero generate resource Todo
```

A resource generates:

- Controller
- Model
- Routes
- TS types

---

## 12. Default template

The generated project should contain:

- Todo CRUD example
- A Vue UI listing todos
- A working API
- A pre-wired API client (planned)

---

## 13. Roadmap

## v0.1

* Runtime backend
* DSL routing
* Controller + ctx
* Serve frontend build

## v0.2

* Full CLI
* Todo template
* Dev orchestration

## v0.3

* TS client generation
* Standardized validation

## v0.4

* Auth JWT
* SQLite

## v1.0

* Generators
* Migrations
* PostgreSQL

---

## 14. Non-negotiable constraints

- Single official structure
- Single error format
- Single way to write controllers
- Zero mandatory configuration
- Ultra-short documentation

---

## 15. Strategic goal

Kaero should be:

- Simpler than Spring Boot
- Smoother than Angular + Java
- More direct than NestJS
- More coherent than a hand-made stack

---

## 16. Success metrics

- Project creation time < 30 seconds
- First working route < 5 minutes
- Production build < 10 seconds for a small project
- Fewer than 5 files to understand to be productive

---

## Conclusion

Kaero is a Kotlin + Vue fullstack framework:

- Opinionated
- Minimal
- Fast
- Radically simple

It enforces strong conventions to remove unnecessary complexity and provide an immediate developer experience.
