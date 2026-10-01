<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { Megaphone, ShieldCheck } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import PostCard from "@/components/posts/PostCard.vue";
import PostComposer from "@/components/posts/PostComposer.vue";
import EmptyState from "@/components/ui/EmptyState.vue";
import type { ListingDTO, PostView } from "@/lib/types";

const props = defineProps<{
  businessId: string;
  businessName: string;
  logoUrl: string | null;
  ownerId: string;
  verified: boolean;
  listings: ListingDTO[];
}>();
const emit = defineEmits<{ count: [n: number] }>();

const auth = useAuthStore();
const route = useRoute();
const posts = ref<PostView[]>([]);
const loading = ref(true);
const error = ref("");

const isOwner = computed(() => auth.user?.id === props.ownerId);
const PAGE_SIZE = 20;
const hasMore = ref(false);
const loadingMore = ref(false);

/** Older posts: everything before the oldest unpinned post we already have. */
async function loadMore() {
  const oldest = posts.value.filter((p) => !p.pinned).at(-1);
  if (!oldest || loadingMore.value) return;
  loadingMore.value = true;
  try {
    const { data } = await api.get<PostView[]>(`/businesses/${props.businessId}/posts`, { params: { before: oldest.createdAt } });
    posts.value = [...posts.value, ...data.filter((p) => !posts.value.some((q) => q.id === p.id))];
    hasMore.value = data.length >= PAGE_SIZE;
  } finally {
    loadingMore.value = false;
  }
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<PostView[]>(`/businesses/${props.businessId}/posts`);
    posts.value = data;
    hasMore.value = data.filter((p) => !p.pinned).length >= PAGE_SIZE;
    emit("count", data.length);
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
  // A shared post link (/providers/:id#post-:postId) scrolls to and highlights that post.
  if (route.hash.startsWith("#post-")) {
    await nextTick();
    const el = document.querySelector(route.hash);
    el?.scrollIntoView({ behavior: "smooth", block: "start" });
    el?.classList.add("ring-4", "ring-teal-500/20");
    setTimeout(() => el?.classList.remove("ring-4", "ring-teal-500/20"), 2500);
  }
}

function onCreated(post: PostView) {
  // Keep a pinned post on top; the new post goes right under it.
  const pinned = posts.value.filter((p) => p.pinned);
  posts.value = [...pinned, post, ...posts.value.filter((p) => !p.pinned)];
  emit("count", posts.value.length);
}

function onUpdated(post: PostView) {
  if (post.pinned) {
    // Only one post can be pinned; mirror what the server did and move it to the top.
    posts.value = [post, ...posts.value.filter((p) => p.id !== post.id).map((p) => ({ ...p, pinned: false }))];
  } else {
    posts.value = posts.value.map((p) => (p.id === post.id ? post : p));
  }
}

function onDeleted(id: string) {
  posts.value = posts.value.filter((p) => p.id !== id);
  emit("count", posts.value.length);
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-4">
    <template v-if="isOwner && !auth.isAdmin">
      <PostComposer
        v-if="props.verified"
        :business-id="props.businessId"
        :business-name="props.businessName"
        :logo-url="props.logoUrl"
        :listings="props.listings"
        @saved="onCreated"
      />
      <div v-else class="flex gap-3 rounded-card border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <ShieldCheck class="mt-0.5 h-5 w-5 shrink-0 text-warning" />
        <p>
          <span class="font-semibold">Posting unlocks once your business is verified.</span> It keeps the feed free of spam - an admin
          usually reviews new businesses within a few days.
        </p>
      </div>
    </template>

    <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>
    <template v-else-if="loading">
      <div v-for="n in 2" :key="n" class="card space-y-3">
        <div class="flex items-center gap-3"><div class="skeleton h-10 w-10 rounded-full"></div><div class="skeleton h-4 w-40"></div></div>
        <div class="skeleton h-4 w-full"></div>
        <div class="skeleton h-4 w-2/3"></div>
      </div>
    </template>
    <EmptyState
      v-else-if="posts.length === 0"
      :icon="Megaphone"
      :title="isOwner ? 'Share your first update' : 'No posts yet'"
      :description="isOwner
        ? 'Post specials, new stock, opening hours or behind-the-scenes photos. Everyone who follows you gets notified.'
        : `When ${props.businessName} posts updates, they'll show up here. Follow them to get notified.`"
    />
    <template v-else>
      <PostCard v-for="p in posts" :key="p.id" :post="p" :listings="props.listings" @updated="onUpdated" @deleted="onDeleted" />
      <button v-if="hasMore" class="btn-secondary w-full" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? "Loading…" : "Load older posts" }}
      </button>
    </template>
  </section>
</template>
