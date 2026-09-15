<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useMessagesStore } from "@/stores/messages";
import type { ConversationSummaryView, MessageDTO } from "@/lib/types";

const route = useRoute();
const auth = useAuthStore();
const messagesStore = useMessagesStore();

const conversation = ref<ConversationSummaryView | null>(null);
const thread = ref<MessageDTO[]>([]);
const error = ref("");
const draft = ref("");
const sending = ref(false);
const scrollEl = ref<HTMLElement | null>(null);

async function load() {
  try {
    const [{ data: convos }, { data: msgs }] = await Promise.all([
      api.get<ConversationSummaryView[]>("/conversations"),
      api.get<MessageDTO[]>(`/conversations/${route.params.id}/messages`),
    ]);
    conversation.value = convos.find((c) => c.id === route.params.id) ?? null;
    thread.value = msgs;
    await messagesStore.fetchUnreadCount();
    await nextTick();
    scrollEl.value?.scrollTo({ top: scrollEl.value.scrollHeight });
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function send() {
  if (!draft.value.trim()) return;
  sending.value = true;
  try {
    const { data } = await api.post<MessageDTO>(`/conversations/${route.params.id}/messages`, { body: draft.value });
    thread.value.push(data);
    draft.value = "";
    await nextTick();
    scrollEl.value?.scrollTo({ top: scrollEl.value.scrollHeight, behavior: "smooth" });
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    sending.value = false;
  }
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString("en-ZA", { hour: "2-digit", minute: "2-digit" });
}

const myId = computed(() => auth.user?.id);

onMounted(load);
</script>

<template>
  <section class="mx-auto flex h-[calc(100vh-120px)] max-w-2xl flex-col">
    <RouterLink to="/messages" class="mb-3 inline-flex w-fit items-center gap-1 text-sm font-medium text-medium-grey hover:text-campus-teal">
      &larr; Messages
    </RouterLink>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>

    <template v-if="conversation">
      <div class="flex items-center justify-between rounded-t-card border border-light-grey bg-white px-4 py-3">
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-uni-navy font-display text-sm font-semibold text-white">
            {{ conversation.counterpartName.charAt(0) }}
          </div>
          <div>
            <p class="text-sm font-semibold text-charcoal">{{ conversation.counterpartName }}</p>
            <p v-if="conversation.listingName" class="text-xs text-campus-teal">{{ conversation.listingName }}</p>
          </div>
        </div>
      </div>

      <div ref="scrollEl" class="flex-1 space-y-3 overflow-y-auto border-x border-light-grey bg-soft-grey px-4 py-5">
        <div v-for="m in thread" :key="m.id" class="flex" :class="m.senderId === myId ? 'justify-end' : 'justify-start'">
          <div
            class="max-w-[75%] rounded-2xl px-4 py-2.5 text-sm shadow-sm"
            :class="m.senderId === myId ? 'rounded-tr-sm bg-campus-teal text-white' : 'rounded-tl-sm bg-white text-charcoal'"
          >
            {{ m.body }}
            <div class="mt-1 text-[11px]" :class="m.senderId === myId ? 'text-white/70' : 'text-medium-grey'">
              {{ formatTime(m.createdAt) }}
            </div>
          </div>
        </div>
      </div>

      <form class="flex items-end gap-2 rounded-b-card border border-t-0 border-light-grey bg-white px-4 py-3" @submit.prevent="send">
        <textarea v-model="draft" rows="1" placeholder="Write a message..." class="input-field resize-none"></textarea>
        <button type="submit" class="btn-primary shrink-0 text-sm" :disabled="sending || !draft.trim()">
          {{ sending ? "Sending..." : "Send" }}
        </button>
      </form>
    </template>

    <p v-else-if="!error" class="text-sm text-medium-grey">Loading...</p>
  </section>
</template>
