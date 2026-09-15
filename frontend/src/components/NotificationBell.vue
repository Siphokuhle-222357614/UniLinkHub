<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useNotificationsStore } from "@/stores/notifications";

const router = useRouter();
const notifications = useNotificationsStore();
const open = ref(false);
let pollHandle: ReturnType<typeof setInterval> | undefined;

const CATEGORY_ICONS: Record<string, string> = {
  BUSINESS: "✓",
  BOOKING: "📅",
  STOCK: "📦",
  REVIEW: "⭐",
  ANNOUNCEMENT: "📢",
  MESSAGE: "💬",
  ORDER: "📦",
  QUESTION: "❓",
  SAVED_SEARCH: "🔍",
};

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

async function toggle() {
  open.value = !open.value;
  if (open.value) {
    await notifications.fetchAll();
  }
}

function viewAll() {
  open.value = false;
  router.push("/notifications");
}

onMounted(() => {
  notifications.fetchUnreadCount();
  pollHandle = setInterval(() => notifications.fetchUnreadCount(), 30000);
});

onUnmounted(() => {
  clearInterval(pollHandle);
});
</script>

<template>
  <div class="relative">
    <button
      class="relative flex h-9 w-9 items-center justify-center rounded-full border border-light-grey"
      aria-label="Notifications"
      @click="toggle"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" />
      </svg>
      <span
        v-if="notifications.unreadCount > 0"
        class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white"
      >
        {{ notifications.unreadCount > 9 ? "9+" : notifications.unreadCount }}
      </span>
    </button>

    <div
      v-if="open"
      class="absolute right-0 top-[calc(100%+8px)] z-20 w-80 rounded-card border border-light-grey bg-white shadow-md"
    >
      <div class="flex items-center justify-between border-b border-light-grey px-4 py-3">
        <span class="font-display text-sm font-semibold text-uni-navy">Notifications</span>
        <button
          v-if="notifications.unreadCount > 0"
          class="text-xs font-medium text-campus-teal"
          @click="notifications.markAllRead()"
        >
          Mark all read
        </button>
      </div>
      <div class="max-h-96 overflow-y-auto">
        <p v-if="notifications.notifications.length === 0" class="px-4 py-6 text-center text-sm text-medium-grey">
          Nothing yet - we'll let you know when something happens.
        </p>
        <div
          v-for="n in notifications.notifications.slice(0, 8)"
          :key="n.id"
          class="flex gap-2.5 border-b border-light-grey px-4 py-3 last:border-b-0"
          :class="n.read ? 'opacity-70' : 'bg-campus-teal/5'"
        >
          <span class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-sky-blue/20 text-uni-navy">
            {{ CATEGORY_ICONS[n.category] ?? "🔔" }}
          </span>
          <div>
            <p class="text-sm text-charcoal">{{ n.message }}</p>
            <p class="mt-0.5 text-xs text-medium-grey">{{ relativeTime(n.createdAt) }}</p>
          </div>
        </div>
      </div>
      <div class="border-t border-light-grey px-4 py-2.5 text-center">
        <button class="text-xs font-semibold text-campus-teal" @click="viewAll">View all notifications</button>
      </div>
    </div>
  </div>
</template>
