<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useMessagesStore } from "@/stores/messages";

const router = useRouter();
const messages = useMessagesStore();
const open = ref(false);
let pollHandle: ReturnType<typeof setInterval> | undefined;

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
  <div class="relative">
    <button
      class="relative flex h-9 w-9 items-center justify-center rounded-full border border-light-grey"
      aria-label="Messages"
      @click="toggle"
    >
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
        <path
          d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z"
        />
      </svg>
      <span
        v-if="messages.unreadCount > 0"
        class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white"
      >
        {{ messages.unreadCount > 9 ? "9+" : messages.unreadCount }}
      </span>
    </button>

    <div
      v-if="open"
      class="absolute right-0 top-[calc(100%+8px)] z-20 w-80 rounded-card border border-light-grey bg-white shadow-md"
    >
      <div class="flex items-center justify-between border-b border-light-grey px-4 py-3">
        <span class="font-display text-sm font-semibold text-uni-navy">Messages</span>
      </div>
      <div class="max-h-96 overflow-y-auto">
        <p v-if="messages.conversations.length === 0" class="px-4 py-6 text-center text-sm text-medium-grey">
          Message a seller from any listing to start a conversation.
        </p>
        <button
          v-for="c in messages.conversations.slice(0, 8)"
          :key="c.id"
          class="flex w-full items-center gap-2.5 border-b border-light-grey px-4 py-3 text-left last:border-b-0 hover:bg-soft-grey"
          :class="{ 'bg-campus-teal/5': c.unreadCount > 0 }"
          @click="openConversation(c.id)"
        >
          <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-uni-navy text-xs font-semibold text-white">
            {{ c.counterpartName.charAt(0) }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium text-charcoal">{{ c.counterpartName }}</p>
            <p class="truncate text-xs text-medium-grey">{{ c.lastMessage || "No messages yet" }}</p>
          </div>
          <span v-if="c.unreadCount > 0" class="h-2 w-2 shrink-0 rounded-full bg-campus-teal"></span>
          <span v-else class="shrink-0 text-[10px] text-medium-grey">{{ relativeTime(c.lastMessageAt) }}</span>
        </button>
      </div>
      <div class="border-t border-light-grey px-4 py-2.5 text-center">
        <button class="text-xs font-semibold text-campus-teal" @click="viewAll">View all messages</button>
      </div>
    </div>
  </div>
</template>
