import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import { vDialog } from "./directives/dialog";
import "./style.css";
import "./lib/theme";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.directive("dialog", vDialog);
app.mount("#app");

router.isReady().finally(() => {
  const splash = document.getElementById("app-splash");
  if (splash) {
    splash.classList.add("splash-hidden");
    setTimeout(() => splash.remove(), 500);
  }
});
