<script setup lang="ts">
import { useToastStore, type ToastType } from "@/stores/toast";

const toast = useToastStore();

const ICONS: Record<ToastType, string> = {
  success: "M20 6 9 17l-5-5",
  error: "M18 6 6 18M6 6l12 12",
  info: "M12 16v-4M12 8h.01",
};

const STYLES: Record<ToastType, { bg: string; ring: string; icon: string; bar: string }> = {
  success: { bg: "bg-white", ring: "ring-success/20", icon: "bg-success text-white", bar: "bg-success" },
  error: { bg: "bg-white", ring: "ring-danger/20", icon: "bg-danger text-white", bar: "bg-danger" },
  info: { bg: "bg-white", ring: "ring-campus-teal/20", icon: "bg-campus-teal text-white", bar: "bg-campus-teal" },
};

function onEnter(id: number) {
  toast.pause(id);
}

function onLeave(id: number) {
  toast.resume(id);
}
</script>

<template>
  <div class="pointer-events-none fixed inset-x-0 top-4 z-[9999] flex flex-col items-center gap-2.5 px-4 sm:items-end sm:px-6">
    <TransitionGroup name="toast">
      <div
        v-for="t in toast.items"
        :key="t.id"
        class="toast-card pointer-events-auto w-full max-w-sm overflow-hidden rounded-card shadow-lg ring-1"
        :class="[STYLES[t.type].bg, STYLES[t.type].ring]"
        @mouseenter="onEnter(t.id)"
        @mouseleave="onLeave(t.id)"
      >
        <div class="flex items-start gap-3 p-3.5">
          <span
            class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full"
            :class="STYLES[t.type].icon"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path :d="ICONS[t.type]" />
            </svg>
          </span>
          <div class="min-w-0 flex-1 pt-0.5">
            <p class="text-sm font-semibold text-charcoal">{{ t.title }}</p>
            <p v-if="t.message" class="mt-0.5 text-xs text-medium-grey">{{ t.message }}</p>
          </div>
          <button
            class="shrink-0 rounded-full p-1 text-medium-grey transition hover:bg-soft-grey hover:text-charcoal"
            aria-label="Dismiss"
            @click="toast.dismiss(t.id)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div v-if="t.duration > 0" class="h-[3px] w-full bg-soft-grey">
          <div
            class="toast-bar h-full origin-left"
            :class="STYLES[t.type].bar"
            :style="{ animationDuration: `${t.duration}ms` }"
          ></div>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-bar {
  animation-name: toast-shrink;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

.toast-card:hover .toast-bar {
  animation-play-state: paused;
}

@keyframes toast-shrink {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

.toast-enter-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1);
  position: absolute;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(-16px) scale(0.92);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(24px) scale(0.95);
}
.toast-move {
  transition: transform 0.3s ease;
}
</style>
