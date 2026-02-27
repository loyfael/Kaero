<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useTodosStore } from "./todos.store";

const todosStore = useTodosStore();

const newTitle = ref("");
const error = ref<string | null>(null);

// This page is intentionally kept simple and uses inline styles.

async function addTodo() {
  const title = newTitle.value.trim();
  if (!title) return;
  await todosStore.add(title);
  newTitle.value = "";
}

async function toggle(id: number) {
  await todosStore.toggle(id);
}

async function remove(id: number) {
  await todosStore.remove(id);
}

onMounted(() => {
  todosStore.fetch();
});
</script>

<template>
  <main style="max-width: 680px; margin: 40px auto; font-family: system-ui, sans-serif;">
    <h1>Todos</h1>

    <form @submit.prevent="addTodo" style="display: flex; gap: 8px;">
      <input v-model="newTitle" placeholder="New todo" style="flex: 1; padding: 8px;" />
      <button type="submit" style="padding: 8px 12px;">Add</button>
    </form>

    <p v-if="error" style="color: #b00020;">{{ error }}</p>
    <p v-else-if="todosStore.error" style="color: #b00020;">{{ todosStore.error }}</p>
    <p v-else-if="todosStore.loading">Loading…</p>

    <ul style="padding: 0; list-style: none;">
      <li
        v-for="t in todosStore.items"
        :key="t.id"
        style="display: flex; gap: 8px; align-items: center; padding: 8px 0;"
      >
        <input type="checkbox" :checked="t.done" @change="toggle(t.id)" />
        <span :style="{ textDecoration: t.done ? 'line-through' : 'none' }">{{ t.title }}</span>
        <button @click="remove(t.id)" style="margin-left: auto;">Delete</button>
      </li>
    </ul>
  </main>
</template>
