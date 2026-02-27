/**
 * Counter feature service.
 *
 * A service contains domain logic and is used by the store.
 */

export const counterService = {
  increment(current: number) {
    return current + 1;
  },
} as const;
