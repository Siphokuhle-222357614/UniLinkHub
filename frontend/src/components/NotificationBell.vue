<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Bell } from "@lucide/vue";
import { useNotificationsStore } from "@/stores/notifications";
import { useClickOutside } from "@/composables/useClickOutside";
import { realtimeConnected } from "@/lib/realtime";
import { relativeTime } from "@/lib/format";
import { notificationMeta } from "@/lib/notificationMeta";

const router = useRouter();
const notifications = useNotificationsStore();
const open = ref(false);
const root = ref<HTMLElement | null>(null);
let pollHandle: ReturnType<typeof setInterval> | undefined;

useClickOutside(root, () => (open.value = false));

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
  // Live updates arrive over the realtime stream; polling is only the fallback when it's down.
  pollHandle = setInterval(() => {
    if (!realtimeConnected.value) notifications.fetchUnreadCount();
  }, 30000);
});

onUnmounted(() => {
  clearInterval(pollHandle);
});
</script>

<template>
  <div ref="root" class="relative">
    <button class="btn-icon" aria-label="Notifications" :aria-expanded="open" @click="toggle">
      <Bell class="h-5 w-5" />
      <span v-if="notifications.unreadCount > 0" class="count-dot">
        {{ notifications.unreadCount > 9 ? "9+" : notifications.unreadCount }}
      </span>
    </button>

    <div v-if="open" class="dropdown-panel">
      <div class="flex items-center justify-between border-b border-light-grey px-4 py-3.5">
        <span class="font-display text-sm font-semibold text-uni-navy">Notifications</span>
        <button v-if="notifications.unreadCount > 0" class="text-xs font-semibold text-teal-600 hover:text-teal-700" @click="notifications.markAllRead()">
          Mark all read
        </button>
      </div>
      <div class="max-h-96 overflow-y-auto p-1.5">
        <div v-if="notifications.notifications.length === 0" class="px-4 py-8 text-center">
          <Bell class="mx-auto h-8 w-8 text-navy-200" />
          <p class="mt-2 text-sm text-medium-grey">You're all caught up. We'll let you know when something happens.</p>
        </div>
        <div
          v-for="n in notifications.notifications.slice(0, 8)"
          :key="n.id"
          class="relative flex gap-3 rounded-control px-3 py-2.5"
          :class="n.read ? '' : 'bg-teal-50/50'"
        >
          <span
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
            :class="notificationMeta(n.category).tone"
          >
            <component :is="notificationMeta(n.category).icon" class="h-4 w-4" />
          </span>
          <div class="min-w-0 flex-1 pr-3">
            <p class="text-sm leading-snug" :class="n.read ? 'text-medium-grey' : 'text-charcoal'">{{ n.message }}</p>
            <p class="mt-0.5 text-[11px] text-medium-grey">{{ relativeTime(n.createdAt) }}</p>
          </div>
          <span v-if="!n.read" class="absolute right-3 top-4 h-2 w-2 rounded-full bg-teal-500"></span>
        </div>
      </div>
      <div class="border-t border-light-grey p-2">
        <button class="btn-ghost w-full py-2 text-xs text-teal-700" @click="viewAll">View all notifications</button>
      </div>
    </div>
  </div>
</template>
