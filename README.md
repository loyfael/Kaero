<p align="center">
  <img src="logo.png" alt="Kaero logo" width="320">
</p>

> [!WARNING]
> Kaero is experimental. It is not production-ready, and its APIs, CLI behavior, project structure, and conventions may still change.

# Kaero

Kaero is an opinionated full-stack framework prototype built around a simple goal: start fast, keep the structure easy to read, and rely on conventions instead of ceremony.

Today, this repository gives you three concrete things:

- a Kotlin runtime built on Ktor
- a cross-platform JVM CLI
- a sample full-stack app using Kotlin, Vue 3, Vite, TypeScript, and Pinia

The CLI is the main way to use Kaero from this repository.

## Installation From GitHub

Clone the repository and move into it:

```bash
git clone https://github.com/<your-org-or-user>/kaero.git
cd kaero
```

### Prerequisites

Kaero currently expects:

- Java 25
- Gradle >= 9.3.0
- Bun >= 1.2.0

Quick checks:

```bash
java -version
gradle --version
bun --version
```

If one of these commands is missing, install it first before continuing.

## CLI Setup

This is the most important section of the README.

The repository ships a JVM CLI prototype in `kaero-cli/`. You build it from the clone, then run it locally or add it to your `PATH`.

### Build the CLI

From the repository root:

```bash
gradle :kaero-cli:installDist
```

This generates launchers here:

- macOS, Linux, WSL, Git Bash: `kaero-cli/build/install/kaero/bin/kaero`
- Windows: `kaero-cli/build/install/kaero/bin/kaero.bat`

### Run the CLI Directly

On macOS, Linux, WSL, or Git Bash:

```bash
./kaero-cli/build/install/kaero/bin/kaero doctor
./kaero-cli/build/install/kaero/bin/kaero init ./my-app
```

On Windows PowerShell or Command Prompt:

```powershell
.\kaero-cli\build\install\kaero\bin\kaero.bat doctor
.\kaero-cli\build\install\kaero\bin\kaero.bat init .\my-app
```

### Main CLI Commands

The current CLI intentionally stays small:

- `kaero doctor`: checks Java, Gradle, and Bun
- `kaero init <directory>`: creates a new standalone Kaero project

Recommended first check:

```bash
kaero doctor
```

### Add the CLI to PATH

If you want to run `kaero` from anywhere, add the generated `bin` directory to your `PATH`.

Add the directory, not the launcher file itself.

Example path inside this clone:

```text
<repo>/kaero-cli/build/install/kaero/bin
```

Windows example:

```text
C:\Users\Felke\Desktop\Kaero\kaero-cli\build\install\kaero\bin
```

Simple Windows PowerShell example:

```powershell
$kaeroBin = "C:\Users\Felke\Desktop\Kaero\kaero-cli\build\install\kaero\bin"
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$kaeroBin;$userPath", "User")
```

Then reopen your terminal and verify:

```bash
kaero doctor
```

If you prefer a more stable installation outside the repo build directory, you can also build a distributable archive:

```bash
gradle :kaero-cli:distZip
```

Then extract it somewhere permanent and add its `bin/` folder to your `PATH`.

## Quick Start

If you just want to see Kaero running, launch the sample app.

### 1. Start the backend

From the repository root:

```bash
gradle :sample-app:run
```

Backend URL:

```text
http://localhost:8080
```

### 2. Start the frontend

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

In development, Vite proxies `/api` to the backend.

## Create a Project With the CLI

Once the CLI works, create a new project like this:

```bash
kaero init example
```

This creates a standalone Gradle project with:

- a hidden `.kaero/` runtime module
- one application module named after your project
- a frontend inside that application module
- Gradle wrapper files

Typical generated structure:

```text
example/
├── .kaero/
├── example/
│   ├── frontend/
│   └── src/
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

Run the generated app like this:

```bash
cd example
bun install --cwd example/frontend
./gradlew :example:run
```

In another terminal:

```bash
bun --cwd example/frontend run dev
```

## Repository Structure

You do not need to understand the whole repository to get started. These are the main parts:

- `kaero-cli/`: the JVM CLI you build and run from this repository
- `kaero-runtime/`: the runtime used by the repository itself
- `sample-app/`: the example full-stack application
- `kaero`: a Bash helper script for repository development on Unix-like shells

## Backend Concepts

Kaero keeps the backend intentionally small and explicit.

### Controllers

Controllers extend `KaeroController` and use route annotations such as `@Get`, `@Post`, `@Patch`, and `@Delete`.

Example:

```kotlin
class TodosController : KaeroController() {
    @Get("/api/todos")
    suspend fun index() = ok(TodoService.list())
}
```

### Central Routing

Each app has one routing entrypoint where controllers are registered. The goal is to keep route wiring easy to find and easy to reason about.

### JSON Format

Kaero standardizes responses around two envelopes.

Success:

```json
{
  "data": {}
}
```

Error:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Something went wrong",
    "details": null
  }
}
```

## Frontend

The current frontend stack is intentionally light:

- Vue 3
- Vite
- TypeScript
- Pinia

The sample app keeps the frontend feature-oriented and easy to scan, with small folders for pages, stores, and services.

## Useful Commands

From the repository root:

```bash
gradle :kaero-cli:installDist
gradle :kaero-cli:distZip
gradle :sample-app:run
```

From the CLI:

```bash
kaero doctor
kaero init my-app
```

Repository helper script on Unix-like environments:

```bash
./kaero dev
./kaero backend
./kaero frontend
./kaero build
./kaero test
```

## Current Limitations

Kaero is still a prototype.

Current limitations worth knowing upfront:

- the CLI currently exposes only `doctor` and `init`
- the root `./kaero` helper is Bash-only
- the sample app is intentionally simple and uses in-memory data
- tooling and project structure are still evolving
- there is no polished installer yet for the CLI

## Conclusion

Kaero is trying to make a Kotlin + Vue full-stack setup feel simple, fast, and convention-first.

If you want the shortest path through this repository:

1. clone it
2. build the CLI
3. run `kaero doctor`
4. run `kaero init my-app`
5. launch the sample app when you want to see the current developer experience end to end