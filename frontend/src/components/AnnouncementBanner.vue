<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Megaphone, X } from "@lucide/vue";
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
  <div v-if="announcement && !dismissed" class="border-b border-gold-200/60 bg-gradient-to-r from-gold-50 via-white to-teal-50 px-4 py-2.5 sm:px-6">
    <div class="mx-auto flex max-w-7xl 2xl:max-w-[1400px] 3xl:max-w-[1600px] items-center justify-between gap-3">
      <p class="flex min-w-0 items-center gap-2.5 text-sm text-navy-800">
        <span class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gold-100 text-gold-700">
          <Megaphone class="h-3.5 w-3.5" />
        </span>
        <span class="min-w-0 font-medium">{{ announcement.message }}</span>
      </p>
      <button class="btn-icon h-8 w-8 text-navy-400" aria-label="Dismiss announcement" @click="dismiss">
        <X class="h-4 w-4" />
      </button>
    </div>
  </div>
</template>
