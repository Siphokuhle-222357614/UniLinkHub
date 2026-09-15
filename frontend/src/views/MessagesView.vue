<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { useMessagesStore } from "@/stores/messages";

const router = useRouter();
const messages = useMessagesStore();

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

function open(id: string) {
  router.push(`/messages/${id}`);
}

onMounted(() => messages.fetchAll());
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="font-display text-2xl font-bold text-uni-navy">Messages</h1>
      <span v-if="messages.conversations.some((c) => c.unreadCount > 0)" class="badge bg-campus-teal/15 text-campus-teal">
        {{ messages.conversations.reduce((s, c) => s + c.unreadCount, 0) }} unread
      </span>
    </div>

    <div v-if="messages.conversations.length === 0" class="card space-y-2 py-12 text-center">
      <p class="text-sm font-medium text-charcoal">No messages yet</p>
      <p class="text-sm text-medium-grey">Message a seller from any listing to ask a question or arrange pickup.</p>
    </div>

    <div v-else class="overflow-hidden rounded-card border border-light-grey bg-white">
      <button
        v-for="c in messages.conversations"
        :key="c.id"
        class="flex w-full items-center gap-3 border-b border-light-grey px-4 py-4 text-left last:border-b-0 hover:bg-soft-grey"
        :class="{ 'bg-campus-teal/5': c.unreadCount > 0 }"
        @click="open(c.id)"
      >
        <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-uni-navy font-display text-sm font-semibold text-white">
          {{ c.counterpartName.charAt(0) }}
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between">
            <p class="truncate text-sm font-semibold text-charcoal">{{ c.counterpartName }}</p>
            <span class="shrink-0 text-xs text-medium-grey">{{ relativeTime(c.lastMessageAt) }}</span>
          </div>
          <p v-if="c.listingName" class="truncate text-xs text-medium-grey">Re: {{ c.listingName }}</p>
          <p class="mt-0.5 truncate text-sm text-charcoal">{{ c.lastMessage || "No messages yet" }}</p>
        </div>
        <span v-if="c.unreadCount > 0" class="h-2.5 w-2.5 shrink-0 rounded-full bg-campus-teal"></span>
      </button>
    </div>
  </section>
</template>
