<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { Bell } from "@lucide/vue";
import { useNotificationsStore } from "@/stores/notifications";
import { notificationMeta } from "@/lib/notificationMeta";
import EmptyState from "@/components/ui/EmptyState.vue";
import type { NotificationCategory } from "@/lib/types";

const notifications = useNotificationsStore();
const filter = ref<"ALL" | NotificationCategory>("ALL");

const filters: { value: "ALL" | NotificationCategory; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "BUSINESS", label: "Verifications" },
  { value: "BOOKING", label: "Bookings" },
  { value: "ORDER", label: "Orders" },
  { value: "MESSAGE", label: "Messages" },
  { value: "QUESTION", label: "Q&A" },
  { value: "STOCK", label: "Stock & price" },
  { value: "REVIEW", label: "Reviews" },
  { value: "SAVED_SEARCH", label: "Saved searches" },
  { value: "POST", label: "Posts" },
  { value: "ANNOUNCEMENT", label: "Announcements" },
];

const filtered = computed(() =>
  filter.value === "ALL" ? notifications.notifications : notifications.notifications.filter((n) => n.category === filter.value),
);

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

onMounted(() => notifications.fetchAll());
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="font-display text-xl font-bold text-uni-navy">Notifications</h1>
      <button v-if="notifications.unreadCount > 0" class="text-sm font-medium text-campus-teal" @click="notifications.markAllRead()">
        Mark all read
      </button>
    </div>

    <div class="flex flex-wrap gap-2">
      <button
        v-for="f in filters"
        :key="f.value"
        class="badge cursor-pointer"
        :class="filter === f.value ? 'bg-uni-navy text-white' : 'border border-light-grey bg-white text-charcoal'"
        @click="filter = f.value"
      >
        {{ f.label }}
      </button>
    </div>

    <EmptyState v-if="filtered.length === 0" :icon="Bell" title="Nothing here yet" description="We'll let you know when something happens." />

    <div v-else class="card divide-y divide-light-grey !p-0">
      <div
        v-for="n in filtered"
        :key="n.id"
        class="flex gap-3 px-4 py-3.5"
        :class="n.read ? '' : 'bg-teal-50/50'"
      >
        <span class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-full" :class="notificationMeta(n.category).tone">
          <component :is="notificationMeta(n.category).icon" class="h-4 w-4" />
        </span>
        <div class="flex-1">
          <p class="text-sm" :class="n.read ? 'text-medium-grey' : 'font-medium text-charcoal'">{{ n.message }}</p>
          <p class="mt-1 text-xs text-medium-grey">{{ relativeTime(n.createdAt) }}</p>
        </div>
        <span v-if="!n.read" class="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-teal-500"></span>
      </div>
    </div>
  </section>
</template>
