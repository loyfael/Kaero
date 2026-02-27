/**
 * Frontend entrypoint.
 *
 * Creates the Vue app, installs Pinia, and initializes the tiny hash router.
 */
import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";

import { initRouting } from "./app/core/routing/router";

const pinia = createPinia();
initRouting(pinia);

createApp(App).use(pinia).mount("#app");
