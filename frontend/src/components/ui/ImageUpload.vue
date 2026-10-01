<script setup lang="ts">
import { ref } from "vue";
import { ImagePlus, LoaderCircle, RefreshCw, Trash } from "@lucide/vue";
import { IMAGE_ACCEPT, useImageUpload } from "@/composables/useImageUpload";

const model = defineModel<string>({ default: "" });
const props = withDefaults(defineProps<{ label?: string; shape?: "photo" | "logo"; id?: string }>(), {
  label: "Photo",
  shape: "photo",
  id: undefined,
});

const input = ref<HTMLInputElement | null>(null);
const dragging = ref(false);
const { upload, uploading, error } = useImageUpload();

async function handle(file: File | undefined) {
  const url = await upload(file);
  if (url) model.value = url;
  if (input.value) input.value.value = "";
}

function onDrop(e: DragEvent) {
  dragging.value = false;
  handle(e.dataTransfer?.files?.[0]);
}
</script>

<template>
  <div>
    <p class="field-label">{{ props.label }}</p>
    <div class="flex items-start gap-4">
      <button
        type="button"
        class="group relative flex shrink-0 items-center justify-center overflow-hidden border-2 border-dashed bg-soft-grey transition"
        :class="[
          props.shape === 'logo' ? 'h-24 w-24 rounded-2xl' : 'aspect-[4/3] w-40 rounded-card',
          dragging ? 'border-teal-500 bg-teal-50' : 'border-light-grey hover:border-teal-400 hover:bg-teal-50/50',
        ]"
        :aria-label="model ? 'Change photo' : 'Upload a photo'"
        :disabled="uploading"
        @click="input?.click()"
        @dragover.prevent="dragging = true"
        @dragleave="dragging = false"
        @drop.prevent="onDrop"
      >
        <img v-if="model" :src="model" alt="" class="h-full w-full object-cover" />
        <span v-else class="flex flex-col items-center gap-1 px-2 text-center text-[11px] font-semibold text-medium-grey">
          <ImagePlus class="h-6 w-6 text-navy-300 transition group-hover:text-teal-500" />
          Add photo
        </span>
        <span v-if="uploading" class="absolute inset-0 flex items-center justify-center bg-white/80">
          <LoaderCircle class="h-6 w-6 animate-spin text-teal-600" />
        </span>
      </button>

      <div class="min-w-0 space-y-2 pt-1">
        <p class="text-xs leading-relaxed text-medium-grey">
          {{ props.shape === "logo" ? "A square logo works best." : "A clear, well-lit photo of the real thing sells best." }}
          JPG, PNG or WebP - we'll resize it for you.
        </p>
        <div class="flex flex-wrap gap-2">
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="uploading" @click="input?.click()">
            <component :is="model ? RefreshCw : ImagePlus" class="h-3.5 w-3.5" /> {{ model ? "Change" : "Upload" }}
          </button>
          <button v-if="model" type="button" class="btn-ghost px-3 py-1.5 text-xs hover:text-danger" :disabled="uploading" @click="model = ''">
            <Trash class="h-3.5 w-3.5" /> Remove
          </button>
        </div>
        <p v-if="error" class="text-xs font-medium text-danger" role="alert">{{ error }}</p>
      </div>
    </div>
    <input :id="props.id" ref="input" type="file" :accept="IMAGE_ACCEPT" class="sr-only" @change="handle(($event.target as HTMLInputElement).files?.[0])" />
  </div>
</template>
