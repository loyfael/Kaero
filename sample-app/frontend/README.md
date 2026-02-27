# Kaero sample frontend (Vue + Pinia + Bun)

This frontend is intentionally **very simple** and organized for beginners.

## Getting started

In `sample-app/frontend`:

- Install: `bun install`
- Dev: `bun run dev`
- Build: `bun run build`

The dev server automatically proxies `/api` to `http://localhost:8080`.

### Easiest fullstack dev

From the repository root:

- `./dev.sh` (compat)
- `./kaero dev` (recommended)

This runs backend + frontend and `Ctrl+C` stops both.

## Architecture (Angular-like)

### `src/app/` = the application

Inspired by Angular:

- `app/core/`: infrastructure (routing, singletons)
- `app/features/<feature>/`: a feature = page + store + service
- `app/shared/`: (optional) reusable components

Example (Counter):

- Page: `src/app/features/counter/counter.page.vue`
- Store: `src/app/features/counter/counter.store.ts`
- Service: `src/app/features/counter/counter.service.ts`

### `src/app/core/` = routing (Pinia)

- A tiny “router” implemented as a Pinia store
- Uses the hash (`#/counter`, `#/todos`) to stay trivial and refresh-safe

File:

- `src/app/core/routing/router.ts`

### `src/app/features/*` = store + service + page

A feature contains:

- `*.page.vue`: UI
- `*.store.ts`: state + actions (Pinia)
- `*.service.ts`: domain logic + backend calls

Example: `src/app/features/counter/counter.store.ts`

### Backend calls (inside services)

- To keep things simple, URLs are called directly in `*.service.ts`.

Example:

- `src/app/features/todos/todos.service.ts`: HTTP calls to `/api/todos`

## Beginner example: Counter (+1)

Simple flow:

1) The view calls the store (`counter.increment()`)
2) The store delegates to the service (`counterService.increment(this.value)`)
3) The store updates state (`this.value = ...`)
4) Vue updates the UI automatically

## A more realistic example: Todos (backend)

- UI: `src/app/features/todos/todos.page.vue`
- Store: `src/app/features/todos/todos.store.ts`
- Service: `src/app/features/todos/todos.service.ts`

For this to work, the backend must be running on `http://localhost:8080`.
