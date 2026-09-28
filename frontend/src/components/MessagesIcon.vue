<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { MessageCircle } from "@lucide/vue";
import { useMessagesStore } from "@/stores/messages";
import { useClickOutside } from "@/composables/useClickOutside";
import { initials, relativeTime } from "@/lib/format";

const router = useRouter();
const messages = useMessagesStore();
const open = ref(false);
const root = ref<HTMLElement | null>(null);
let pollHandle: ReturnType<typeof setInterval> | undefined;

useClickOutside(root, () => (open.value = false));

async function toggle() {
  open.value = !open.value;
  if (open.value) {
    await messages.fetchAll();
  }
}

function openConversation(id: string) {
  open.value = false;
  router.push(`/messages/${id}`);
}

function viewAll() {
  open.value = false;
  router.push("/messages");
}

onMounted(() => {
  messages.fetchUnreadCount();
  pollHandle = setInterval(() => messages.fetchUnreadCount(), 30000);
});

onUnmounted(() => {
  clearInterval(pollHandle);
});
</script>

<template>
  <div ref="root" class="relative">
    <button class="btn-icon" aria-label="Messages" :aria-expanded="open" @click="toggle">
      <MessageCircle class="h-5 w-5" />
      <span v-if="messages.unreadCount > 0" class="count-dot">
        {{ messages.unreadCount > 9 ? "9+" : messages.unreadCount }}
      </span>
    </button>

    <div v-if="open" class="dropdown-panel">
      <div class="flex items-center justify-between border-b border-light-grey px-4 py-3.5">
        <span class="font-display text-sm font-semibold text-uni-navy">Messages</span>
        <span v-if="messages.unreadCount > 0" class="badge bg-teal-50 text-teal-700">{{ messages.unreadCount }} unread</span>
      </div>
      <div class="max-h-96 overflow-y-auto p-1.5">
        <div v-if="messages.conversations.length === 0" class="px-4 py-8 text-center">
          <MessageCircle class="mx-auto h-8 w-8 text-navy-200" />
          <p class="mt-2 text-sm text-medium-grey">Message a seller from any listing to start a conversation.</p>
        </div>
        <button
          v-for="c in messages.conversations.slice(0, 8)"
          :key="c.id"
          class="flex w-full items-center gap-3 rounded-control px-3 py-2.5 text-left transition hover:bg-soft-grey"
          @click="openConversation(c.id)"
        >
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-navy-100 text-xs font-bold text-navy-700">
            {{ initials(c.counterpartName) }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-baseline justify-between gap-2">
              <p class="truncate text-sm" :class="c.unreadCount > 0 ? 'font-semibold text-charcoal' : 'font-medium text-charcoal'">
                {{ c.counterpartName }}
              </p>
              <span class="shrink-0 text-[11px] text-medium-grey">{{ relativeTime(c.lastMessageAt) }}</span>
            </div>
            <p class="truncate text-xs" :class="c.unreadCount > 0 ? 'text-charcoal' : 'text-medium-grey'">{{ c.lastMessage || "No messages yet" }}</p>
          </div>
          <span v-if="c.unreadCount > 0" class="h-2.5 w-2.5 shrink-0 rounded-full bg-teal-500"></span>
        </button>
      </div>
      <div class="border-t border-light-grey p-2">
        <button class="btn-ghost w-full py-2 text-xs text-teal-700" @click="viewAll">View all messages</button>
      </div>
    </div>
  </div>
</template>
