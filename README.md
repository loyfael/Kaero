
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
