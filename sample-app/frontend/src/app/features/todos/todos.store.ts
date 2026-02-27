import { defineStore } from "pinia";
import { todosService, type Todo } from "./todos.service";

/**
 * Todos feature store.
 *
 * Keeps UI state (loading/error/items) and calls the service for I/O.
 */
export const useTodosStore = defineStore("todos", {
  state: () => ({
    items: [] as Todo[],
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetch() {
      this.loading = true;
      this.error = null;
      try {
        this.items = await todosService.list();
      } catch (e) {
        this.error = e instanceof Error ? e.message : String(e);
      } finally {
        this.loading = false;
      }
    },

    async add(title: string) {
      this.error = null;
      await todosService.create(title);
      await this.fetch();
    },

    async toggle(id: number) {
      this.error = null;
      await todosService.toggle(id);
      await this.fetch();
    },

    async remove(id: number) {
      this.error = null;
      await todosService.remove(id);
      await this.fetch();
    },
  },
});
