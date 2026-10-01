<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Rss } from "@lucide/vue";
import { api } from "@/lib/api";
import PostCard from "@/components/posts/PostCard.vue";
import type { PostView } from "@/lib/types";

const posts = ref<PostView[]>([]);
const loaded = ref(false);
const showAll = ref(false);
const PREVIEW = 3;
const PAGE_SIZE = 20;
const hasMore = ref(false);
const loadingMore = ref(false);

async function loadMore() {
  const oldest = posts.value.at(-1);
  if (!oldest || loadingMore.value) return;
  loadingMore.value = true;
  try {
    const { data } = await api.get<PostView[]>("/posts/feed", { params: { before: oldest.createdAt } });
    posts.value = [...posts.value, ...data];
    hasMore.value = data.length >= PAGE_SIZE;
  } finally {
    loadingMore.value = false;
  }
}

onMounted(async () => {
  try {
    const { data } = await api.get<PostView[]>("/posts/feed");
    posts.value = data;
    hasMore.value = data.length >= PAGE_SIZE;
  } catch {
    // The feed is a nice-to-have on the dashboard; the rest of the page still works without it.
  } finally {
    loaded.value = true;
  }
});

function onUpdated(post: PostView) {
  posts.value = posts.value.map((p) => (p.id === post.id ? post : p));
}
</script>

<template>
  <div v-if="loaded && posts.length > 0">
    <h2 class="section-title flex items-center gap-2"><Rss class="h-5 w-5 text-teal-600" /> From businesses you follow</h2>
    <p class="mb-4 mt-1 text-xs text-medium-grey">Latest updates, specials and new stock.</p>
    <div class="mx-auto max-w-2xl space-y-4">
      <PostCard v-for="p in showAll ? posts : posts.slice(0, PREVIEW)" :key="p.id" :post="p" show-business @updated="onUpdated" />
      <button v-if="!showAll && posts.length > PREVIEW" class="btn-secondary w-full" @click="showAll = true">
        Show {{ posts.length - PREVIEW }} more
      </button>
      <button v-else-if="showAll && hasMore" class="btn-secondary w-full" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? "Loading…" : "Load older posts" }}
      </button>
    </div>
  </div>
</template>
