<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api } from "@/lib/api";
import type { AnnouncementDTO } from "@/lib/types";

const DISMISSED_KEY = "unilinkhub.dismissedAnnouncementId";

const announcement = ref<AnnouncementDTO | null>(null);
const dismissed = ref(false);

async function load() {
  try {
    const { data } = await api.get<AnnouncementDTO | null>("/announcements/active");
    if (!data) return;
    announcement.value = data;
    dismissed.value = localStorage.getItem(DISMISSED_KEY) === data.id;
  } catch {
    // Announcements are a nice-to-have; ignore failures here.
  }
}

function dismiss() {
  if (!announcement.value) return;
  dismissed.value = true;
  try {
    localStorage.setItem(DISMISSED_KEY, announcement.value.id);
  } catch {
    // localStorage unavailable - dismissal just won't persist across reloads.
  }
}

onMounted(load);
</script>

<template>
  <div v-if="announcement && !dismissed" class="bg-academic-gold/15 px-4 py-2.5 sm:px-6">
    <div class="mx-auto flex max-w-6xl items-center justify-between gap-3">
      <p class="flex min-w-0 items-center gap-2 text-sm text-uni-navy">
        <span class="shrink-0">📢</span>
        <span class="min-w-0">{{ announcement.message }}</span>
      </p>
      <button class="shrink-0 text-sm text-uni-navy/60 hover:text-uni-navy" aria-label="Dismiss" @click="dismiss">&times;</button>
    </div>
  </div>
</template>
