# Kaero sample-app

## Dev (backend + frontend)

From the repository root:

- `./dev.sh` (compat wrapper)
- `./kaero dev` (recommended)
- `./kaero dev --app sample-app` (explicit)

This starts:

- Ktor backend: `http://localhost:8080`
- Vite frontend: `http://localhost:5173`

`Ctrl+C` stops both.

If you see `Address already in use`:

- It means something is already listening on 8080 or 5173.
- Option: `./kaero dev --kill-ports`
- Option (aggressive): `./kaero dev --kill-ports --debug`

To see backend/Gradle logs:

- `./kaero dev --debug`

Other useful commands:

- `./kaero test` (backend tests + frontend typecheck)
- `./kaero gen routes` (simple export of annotated routes → `.kaero/generated/`)

## Backend (Kotlin/Ktor)

From the repository root:

- Build: `gradle :sample-app:build`
- Run: `gradle :sample-app:run`

API: `http://localhost:8080/api/todos`

## Frontend (Vue/Vite)

In `sample-app/frontend`:

- Install: `bun install`
- Dev: `bun run dev`

The Vite dev server automatically proxies `/api` to `http://localhost:8080`.

### Kaero conventions (Vue ↔ Kotlin)

Goal: a beginner-friendly architecture with clear separation.

Frontend structure (Angular-like):

- `frontend/src/app/core/`: infrastructure (routing, etc.)
- `frontend/src/app/features/<feature>/`: a feature = page + store + service
- (optional) `frontend/src/app/shared/`: reusable components

#### Beginner example: Counter (+1)

This example shows the minimal separation:

- Page: `frontend/src/app/features/counter/counter.page.vue`
- Routing (Pinia): `frontend/src/app/core/routing/router.ts` (default route `#/counter`)
- Store: `frontend/src/app/features/counter/counter.store.ts`
- Domain/service: `frontend/src/app/features/counter/counter.service.ts`

Flow (read it like a recipe):

1) The view calls the store: `counter.increment()`
2) The store delegates to the service: `counterService.increment(this.value)`
3) The store updates state: `this.value = ...`
4) Vue re-renders automatically.

Where to change what:

- UI/markup: in the page
- Domain rule “+1”: in the service
- State (current value): in the store
- Navigation: in `routing/`

Example usage in a feature page (e.g. `frontend/src/app/features/todos/todos.page.vue`):

- `const todos = useTodosStore()`
- `await todos.fetch()`
- `await todos.add("Buy bread")`

## Production (serve the built frontend)

- `cd sample-app/frontend && bun run build`
- Start backend: `gradle :sample-app:run`

The backend serves static files from `sample-app/frontend/dist` (SPA fallback to `index.html`).
