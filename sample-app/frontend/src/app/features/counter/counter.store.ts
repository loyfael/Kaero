import { defineStore } from "pinia";
import { counterService } from "./counter.service";

/**
 * Counter feature store.
 *
 * Holds feature state and delegates domain logic to the service.
 */
export const useCounterStore = defineStore("counter", {
  state: () => ({
    value: 0,
  }),
  actions: {
    increment() {
      this.value = counterService.increment(this.value);
    },
  },
});
