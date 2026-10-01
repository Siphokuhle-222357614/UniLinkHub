<script setup lang="ts">
import { ref } from "vue";
import { ChevronLeft, ChevronRight, ImagePlus, LoaderCircle, Star, X } from "@lucide/vue";
import { IMAGE_ACCEPT, useImageUpload } from "@/composables/useImageUpload";

const MAX = 6;
const model = defineModel<string[]>({ default: () => [] });
const props = withDefaults(defineProps<{ label?: string; id?: string }>(), { label: "Photos", id: undefined });

const input = ref<HTMLInputElement | null>(null);
const dragging = ref(false);
const { upload, uploading, error } = useImageUpload();

async function addFiles(files: FileList | File[] | null | undefined) {
  if (!files) return;
  for (const file of Array.from(files)) {
    if (model.value.length >= MAX) {
      error.value = `You can add up to ${MAX} photos to a listing.`;
      break;
    }
    const url = await upload(file);
    if (url) model.value = [...model.value, url];
  }
  if (input.value) input.value.value = "";
}

function remove(i: number) {
  model.value = model.value.filter((_, j) => j !== i);
}

/** Moving a photo to the front makes it the cover shown on listing cards. */
function move(i: number, delta: number) {
  const j = i + delta;
  if (j < 0 || j >= model.value.length) return;
  const next = [...model.value];
  [next[i], next[j]] = [next[j], next[i]];
  model.value = next;
}
</script>

<template>
  <div>
    <div class="mb-1.5 flex items-baseline justify-between">
      <p class="field-label mb-0">{{ props.label }}</p>
      <span class="text-[11px] text-medium-grey">{{ model.length }} / {{ MAX }} · the first photo is the cover</span>
    </div>
    <ul class="grid grid-cols-3 gap-2 sm:grid-cols-4" role="list">
      <li v-for="(url, i) in model" :key="url" class="group relative aspect-square overflow-hidden rounded-control ring-1 ring-light-grey">
        <img :src="url" alt="" class="h-full w-full object-cover" />
        <span v-if="i === 0" class="badge absolute left-1.5 top-1.5 bg-uni-navy py-0.5 text-[10px] text-white"><Star class="h-2.5 w-2.5" /> Cover</span>
        <button type="button" class="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-navy-950/70 text-white hover:bg-navy-950" :aria-label="`Remove photo ${i + 1}`" @click="remove(i)">
          <X class="h-3.5 w-3.5" />
        </button>
        <div class="absolute inset-x-1.5 bottom-1.5 flex justify-between opacity-100 sm:opacity-0 sm:transition sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
          <button type="button" class="photo-move" :disabled="i === 0" :aria-label="`Move photo ${i + 1} earlier`" @click="move(i, -1)"><ChevronLeft class="h-3.5 w-3.5" /></button>
          <button type="button" class="photo-move" :disabled="i === model.length - 1" :aria-label="`Move photo ${i + 1} later`" @click="move(i, 1)"><ChevronRight class="h-3.5 w-3.5" /></button>
        </div>
      </li>
      <li v-if="model.length < MAX">
        <button
          type="button"
          class="flex aspect-square w-full flex-col items-center justify-center gap-1 rounded-control border-2 border-dashed text-[11px] font-semibold text-medium-grey transition"
          :class="dragging ? 'border-teal-500 bg-teal-50' : 'border-light-grey bg-soft-grey hover:border-teal-400 hover:bg-teal-50/50'"
          :disabled="uploading"
          :aria-label="model.length ? 'Add more photos' : 'Add photos'"
          @click="input?.click()"
          @dragover.prevent="dragging = true"
          @dragleave="dragging = false"
          @drop.prevent="dragging = false; addFiles($event.dataTransfer?.files)"
        >
          <LoaderCircle v-if="uploading" class="h-6 w-6 animate-spin text-teal-600" />
          <ImagePlus v-else class="h-6 w-6 text-navy-300" />
          {{ uploading ? "Uploading…" : "Add photos" }}
        </button>
      </li>
    </ul>
    <p v-if="error" class="mt-1.5 text-xs font-medium text-danger" role="alert">{{ error }}</p>
    <input :id="props.id" ref="input" type="file" multiple :accept="IMAGE_ACCEPT" class="sr-only" @change="addFiles(($event.target as HTMLInputElement).files)" />
  </div>
</template>

<style scoped>
.photo-move {
  @apply flex h-7 w-7 items-center justify-center rounded-full bg-white/90 text-navy-800 shadow-xs disabled:invisible;
}
</style>
