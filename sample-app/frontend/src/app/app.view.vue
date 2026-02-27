<script setup lang="ts">
import { computed } from "vue";

// Angular-like idea:
// - a single root view
// - chooses which feature page to render

import { useRouterStore } from "./core/routing/router";
import CounterPage from "./features/counter/counter.page.vue";
import TodosPage from "./features/todos/todos.page.vue";

const router = useRouterStore();
const currentView = computed(() => router.current.name);
</script>

<template>
  <CounterPage v-if="currentView === 'counter'" />
  <TodosPage v-if="currentView === 'todos'" />
</template>
