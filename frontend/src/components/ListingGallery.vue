<script setup lang="ts">
import { computed, ref } from "vue";
import { ChevronLeft, ChevronRight } from "@lucide/vue";
import { categoryMeta } from "@/lib/categoryMeta";

const props = defineProps<{ photos: string[]; name: string; category: string }>();

const track = ref<HTMLElement | null>(null);
const index = ref(0);
const failed = ref(new Set<string>());
const shown = computed(() => props.photos.filter((p) => !failed.value.has(p)));
const meta = computed(() => categoryMeta(props.category));

/** Swipe (scroll-snap) on phones, arrows/thumbnails/keyboard everywhere - all drive the same scroll position. */
function go(i: number) {
  const el = track.value;
  if (!el || shown.value.length === 0) return;
  index.value = (i + shown.value.length) % shown.value.length;
  el.scrollTo({ left: el.clientWidth * index.value, behavior: "smooth" });
}

function onScroll() {
  const el = track.value;
  if (el) index.value = Math.round(el.scrollLeft / el.clientWidth);
}

function onKey(e: KeyboardEvent) {
  if (e.key === "ArrowRight") go(index.value + 1);
  if (e.key === "ArrowLeft") go(index.value - 1);
}

function onError(url: string) {
  failed.value = new Set([...failed.value, url]);
}
</script>

<template>
  <div class="space-y-3">
    <div
      class="group relative overflow-hidden rounded-modal bg-soft-grey shadow-card ring-1 ring-light-grey/80"
      role="region"
      aria-roledescription="carousel"
      :aria-label="`Photos of ${name}`"
      tabindex="0"
      @keydown="onKey"
    >
      <div v-if="shown.length > 0" ref="track" class="flex aspect-[4/3] snap-x snap-mandatory overflow-x-auto scroll-smooth [scrollbar-width:none]" @scroll.passive="onScroll">
        <img
          v-for="(url, i) in shown"
          :key="url"
          :src="url"
          :alt="`${name} - photo ${i + 1} of ${shown.length}`"
          class="h-full w-full shrink-0 snap-center object-cover"
          :loading="i === 0 ? 'eager' : 'lazy'"
          @error="onError(url)"
        />
      </div>
      <div v-else class="flex aspect-[4/3] w-full items-center justify-center" :style="{ background: meta.gradient }">
        <component :is="meta.icon" class="h-24 w-24 text-navy-900/15" :stroke-width="1.25" />
      </div>

      <template v-if="shown.length > 1">
        <button class="gallery-arrow left-3" aria-label="Previous photo" @click="go(index - 1)"><ChevronLeft class="h-5 w-5" /></button>
        <button class="gallery-arrow right-3" aria-label="Next photo" @click="go(index + 1)"><ChevronRight class="h-5 w-5" /></button>
        <div class="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5 rounded-full bg-navy-950/40 px-2 py-1 backdrop-blur" aria-hidden="true">
          <span v-for="(_, i) in shown" :key="i" class="h-1.5 rounded-full bg-white transition-all" :class="i === index ? 'w-4' : 'w-1.5 opacity-60'"></span>
        </div>
      </template>
      <slot />
    </div>

    <div v-if="shown.length > 1" class="flex gap-2 overflow-x-auto pb-1">
      <button
        v-for="(url, i) in shown"
        :key="url"
        class="h-16 w-16 shrink-0 overflow-hidden rounded-control ring-2 transition"
        :class="i === index ? 'ring-teal-500' : 'ring-transparent opacity-70 hover:opacity-100'"
        :aria-label="`Show photo ${i + 1}`"
        :aria-current="i === index"
        @click="go(i)"
      >
        <img :src="url" alt="" class="h-full w-full object-cover" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.gallery-arrow {
  @apply absolute top-1/2 hidden h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-navy-800 shadow-card transition hover:bg-white sm:flex sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100;
}
</style>
