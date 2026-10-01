<script setup lang="ts">
import { Moon, Sun } from "@lucide/vue";
import { isDark, toggleTheme } from "@/lib/theme";

/** One-tap light/dark switch, shown in the header of every page. More choices live in Account settings. */
</script>

<template>
  <button
    type="button"
    class="btn-icon"
    aria-label="Dark mode"
    :aria-pressed="isDark"
    :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
    @click="toggleTheme"
  >
    <Transition name="theme-icon" mode="out-in">
      <Sun v-if="isDark" key="sun" class="h-5 w-5 text-gold-400" />
      <Moon v-else key="moon" class="h-5 w-5" />
    </Transition>
  </button>
</template>

<style scoped>
.theme-icon-enter-active,
.theme-icon-leave-active {
  transition: opacity 0.15s ease, transform 0.2s ease;
}
.theme-icon-enter-from {
  opacity: 0;
  transform: rotate(-60deg) scale(0.6);
}
.theme-icon-leave-to {
  opacity: 0;
  transform: rotate(60deg) scale(0.6);
}
</style>
