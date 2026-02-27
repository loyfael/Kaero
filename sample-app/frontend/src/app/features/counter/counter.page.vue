<script setup lang="ts">
import { computed } from "vue";

import { useCounterStore } from "./counter.store";
import { useRouterStore } from "../../core/routing/router";

const counter = useCounterStore();
const router = useRouterStore();

const value = computed(() => counter.value);

function increment() {
  counter.increment();
}

function goTodos() {
  router.push({ name: "todos" });
}
</script>

<template>
  <main style="max-width: 680px; margin: 40px auto; font-family: system-ui, sans-serif;">
    <h1>Counter</h1>

    <p style="font-size: 20px;">Value: <strong>{{ value }}</strong></p>

    <div style="display: flex; gap: 8px;">
      <button @click="increment" style="padding: 8px 12px;">+1</button>
      <button @click="goTodos" style="padding: 8px 12px;">See Todos example</button>
    </div>
  </main>
</template>
