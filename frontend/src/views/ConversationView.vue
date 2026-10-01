<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useMessagesStore } from "@/stores/messages";
import { onRealtime } from "@/lib/realtime";
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
    scrollToEnd("auto");
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

/** On tablets and up the thread has its own scrollbar; on phones the whole page scrolls. */
function threadScrollsItself(): boolean {
  const el = scrollEl.value;
  return !!el && getComputedStyle(el).overflowY === "auto";
}

function scrollToEnd(behavior: ScrollBehavior) {
  const el = scrollEl.value;
  if (threadScrollsItself()) el?.scrollTo({ top: el.scrollHeight, behavior });
  else window.scrollTo({ top: document.documentElement.scrollHeight, behavior });
}

async function send() {
  if (!draft.value.trim()) return;
  sending.value = true;
  try {
    const { data } = await api.post<MessageDTO>(`/conversations/${route.params.id}/messages`, { body: draft.value });
    thread.value.push(data);
    draft.value = "";
    await nextTick();
    scrollToEnd("smooth");
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

/** A reply arrived while this chat is open: re-read the thread, which also marks it as read. */
async function refreshThread() {
  const el = scrollEl.value;
  const atBottom = threadScrollsItself()
    ? !el || el.scrollHeight - el.scrollTop - el.clientHeight < 80
    : window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 160;
  const { data } = await api.get<MessageDTO[]>(`/conversations/${route.params.id}/messages`);
  thread.value = data;
  await messagesStore.fetchUnreadCount();
  await nextTick();
  if (atBottom) scrollToEnd("smooth");
}

const stopLive = onRealtime("message", ({ conversationId }) => {
  if (conversationId === route.params.id) refreshThread().catch(() => {});
});

onMounted(load);
onBeforeUnmount(stopLive);
</script>

<template>
  <!-- Phones: the page itself scrolls and the message box sticks above the bottom tab bar.
       Tablets and up: a fixed-height chat panel whose thread scrolls inside it. -->
  <section class="mx-auto flex max-w-2xl flex-col md:h-[calc(100dvh-9rem)]">
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

      <div ref="scrollEl" class="min-h-[40vh] flex-1 space-y-3 border-x border-light-grey bg-soft-grey px-3 py-5 xs:px-4 md:min-h-0 md:overflow-y-auto">
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

      <form
        class="sticky bottom-[calc(3.625rem+env(safe-area-inset-bottom,0px))] z-10 flex items-end gap-2 rounded-b-card border border-t-0 border-light-grey bg-white px-3 py-3 xs:px-4 md:static"
        @submit.prevent="send"
      >
        <textarea v-model="draft" rows="1" placeholder="Write a message..." aria-label="Write a message" class="input-field min-w-0 resize-none"></textarea>
        <button type="submit" class="btn-primary shrink-0 text-sm" :disabled="sending || !draft.trim()">
          {{ sending ? "Sending..." : "Send" }}
        </button>
      </form>
    </template>

    <p v-else-if="!error" class="text-sm text-medium-grey">Loading...</p>
  </section>
</template>
