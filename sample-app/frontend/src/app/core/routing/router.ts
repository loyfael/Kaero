import { defineStore } from "pinia";
import type { Pinia } from "pinia";

/**
 * CORE / ROUTING (Angular-style "core")
 *
 * This project intentionally avoids vue-router.
 * We keep a tiny router in Pinia and sync it with `location.hash`.
 */

export type ViewRoute = { name: "counter" } | { name: "todos" };

function parseHash(hash: string): ViewRoute {
  const clean = hash.replace(/^#/, "");
  if (clean === "/todos" || clean === "todos") return { name: "todos" };
  return { name: "counter" };
}

function toHash(route: ViewRoute): string {
  switch (route.name) {
    case "counter":
      return "#/counter";
    case "todos":
      return "#/todos";
  }
}

export const useRouterStore = defineStore("router", {
  state: () => ({
    current: parseHash(globalThis.location?.hash ?? "") as ViewRoute,
  }),
  actions: {
    syncFromHash() {
      this.current = parseHash(globalThis.location?.hash ?? "");
    },
    push(route: ViewRoute) {
      this.current = route;
      if (globalThis.location) globalThis.location.hash = toHash(route);
    },
  },
});

export function initRouting(pinia: Pinia) {
  const router = useRouterStore(pinia);

  router.syncFromHash();
  globalThis.addEventListener?.("hashchange", () => router.syncFromHash());

  if (globalThis.location && !globalThis.location.hash) {
    router.push(router.current);
  }
}
