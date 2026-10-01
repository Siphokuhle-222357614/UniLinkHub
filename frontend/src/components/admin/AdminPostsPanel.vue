<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { Megaphone } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import PostCard from "@/components/posts/PostCard.vue";
import EmptyState from "@/components/ui/EmptyState.vue";
import type { PostView } from "@/lib/types";

const posts = ref<PostView[]>([]);
const loading = ref(false);
const error = ref("");
const reportedOnly = ref(true);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<PostView[]>("/admin/posts", { params: { flaggedOnly: reportedOnly.value } });
    posts.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

function onUpdated(post: PostView) {
  posts.value = posts.value.map((p) => (p.id === post.id ? post : p));
}

watch(reportedOnly, load);
onMounted(load);
</script>

<template>
  <section class="space-y-5">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h2 class="section-title">Business posts</h2>
        <p class="text-sm text-medium-grey">Posts students have reported come first. Remove anything that breaks the marketplace rules - the business is told why.</p>
      </div>
      <div class="flex gap-1.5">
        <button class="chip" :class="{ 'chip-active': reportedOnly }" @click="reportedOnly = true">Reported</button>
        <button class="chip" :class="{ 'chip-active': !reportedOnly }" @click="reportedOnly = false">All posts</button>
      </div>
    </div>

    <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>
    <div v-else-if="loading" class="mx-auto max-w-2xl space-y-3">
      <div v-for="n in 2" :key="n" class="skeleton h-40 rounded-card"></div>
    </div>
    <EmptyState
      v-else-if="posts.length === 0"
      :icon="Megaphone"
      :title="reportedOnly ? 'No reported posts' : 'No posts yet'"
      :description="reportedOnly ? 'Nothing needs your attention right now.' : 'Verified businesses haven\'t posted anything yet.'"
    />
    <div v-else class="mx-auto max-w-2xl space-y-4">
      <PostCard v-for="p in posts" :key="p.id" :post="p" show-business @updated="onUpdated" />
    </div>
  </section>
</template>
