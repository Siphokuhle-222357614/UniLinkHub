<script setup lang="ts">
import { computed, ref } from "vue";
import { ImagePlus, LoaderCircle, Package, X } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { IMAGE_ACCEPT, useImageUpload } from "@/composables/useImageUpload";
import type { ListingDTO, PostView } from "@/lib/types";

const MAX = 2000;

const props = defineProps<{
  businessId: string;
  businessName: string;
  logoUrl?: string | null;
  /** The business's own listings, offered as an attachment. */
  listings: ListingDTO[];
  /** When set, the composer edits this post instead of creating a new one. */
  editing?: PostView | null;
}>();
const emit = defineEmits<{ saved: [post: PostView]; cancel: [] }>();

const body = ref(props.editing?.body ?? "");
const imageUrl = ref(props.editing?.imageUrl ?? "");
const listingId = ref(props.editing?.listing?.id ?? "");
const saving = ref(false);
const error = ref("");
const fileInput = ref<HTMLInputElement | null>(null);
const { upload, uploading, error: uploadError } = useImageUpload();

const attachable = computed(() => props.listings.filter((l) => !l.takenDownAt && l.status !== "INACTIVE"));
const attached = computed(() => props.listings.find((l) => l.id === listingId.value) ?? null);
const canSubmit = computed(() => (body.value.trim().length > 0 || !!imageUrl.value) && body.value.length <= MAX && !uploading.value);

async function pickPhoto(e: Event) {
  const input = e.target as HTMLInputElement;
  const url = await upload(input.files?.[0]);
  if (url) imageUrl.value = url;
  input.value = "";
}

async function submit() {
  if (!canSubmit.value) return;
  saving.value = true;
  error.value = "";
  const payload = { body: body.value.trim() || null, imageUrl: imageUrl.value || null, listingId: listingId.value || null };
  try {
    const { data } = props.editing
      ? await api.patch<PostView>(`/posts/${props.editing.id}`, payload)
      : await api.post<PostView>(`/businesses/${props.businessId}/posts`, payload);
    emit("saved", data);
    if (!props.editing) {
      body.value = "";
      imageUrl.value = "";
      listingId.value = "";
    }
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    saving.value = false;
  }
}

function autoGrow(e: Event) {
  const el = e.target as HTMLTextAreaElement;
  el.style.height = "auto";
  el.style.height = `${Math.min(el.scrollHeight, 320)}px`;
}
</script>

<template>
  <div class="card space-y-3 p-4 sm:p-5" :class="{ 'border-teal-200 ring-4 ring-teal-500/10': props.editing }">
    <div class="flex gap-3">
      <img v-if="props.logoUrl" :src="props.logoUrl" alt="" class="h-10 w-10 shrink-0 rounded-full object-cover ring-1 ring-light-grey" />
      <span v-else class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-navy-100 font-display text-sm font-bold text-navy-700">
        {{ props.businessName.charAt(0) }}
      </span>
      <div class="min-w-0 flex-1">
        <label :for="`composer-${props.editing?.id ?? 'new'}`" class="sr-only">Post text</label>
        <textarea
          :id="`composer-${props.editing?.id ?? 'new'}`"
          v-model="body"
          rows="2"
          :maxlength="MAX + 200"
          class="w-full resize-none border-0 bg-transparent p-0 pt-2 text-[15px] leading-relaxed text-charcoal placeholder:text-slate-400 focus:outline-none focus:ring-0"
          :placeholder="props.editing ? 'Edit your post…' : `What's new at ${props.businessName}? Share a special, new stock or your hours…`"
          @input="autoGrow"
        ></textarea>
      </div>
    </div>

    <div v-if="imageUrl" class="relative overflow-hidden rounded-card ring-1 ring-light-grey">
      <img :src="imageUrl" alt="" class="max-h-80 w-full object-cover" />
      <button class="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-navy-950/70 text-white hover:bg-navy-950" aria-label="Remove photo" @click="imageUrl = ''">
        <X class="h-4 w-4" />
      </button>
    </div>

    <div v-if="attached" class="flex items-center gap-3 rounded-control border border-light-grey bg-soft-grey/60 p-2.5">
      <img v-if="attached.imageUrl" :src="attached.imageUrl" alt="" class="h-10 w-10 rounded-lg object-cover" />
      <span v-else class="flex h-10 w-10 items-center justify-center rounded-lg bg-white text-medium-grey"><Package class="h-4 w-4" /></span>
      <p class="min-w-0 flex-1 truncate text-sm font-medium text-charcoal">{{ attached.name }}</p>
      <button class="btn-icon h-8 w-8" aria-label="Remove attached listing" @click="listingId = ''"><X class="h-4 w-4" /></button>
    </div>

    <p v-if="error || uploadError" class="rounded-control border border-red-200 bg-red-50 px-3 py-2 text-sm text-danger" role="alert">{{ error || uploadError }}</p>

    <div class="flex flex-wrap items-center gap-2 border-t border-light-grey pt-3">
      <button type="button" class="btn-ghost px-3 py-1.5 text-xs" :disabled="uploading" @click="fileInput?.click()">
        <LoaderCircle v-if="uploading" class="h-4 w-4 animate-spin" />
        <ImagePlus v-else class="h-4 w-4 text-emerald-600" /> Photo
      </button>
      <input ref="fileInput" type="file" :accept="IMAGE_ACCEPT" class="sr-only" @change="pickPhoto" />
      <select
        v-if="attachable.length > 0 && !attached"
        v-model="listingId"
        class="input-field w-auto max-w-[13rem] border-transparent py-1.5 text-xs shadow-none hover:bg-soft-grey"
        aria-label="Attach a listing"
      >
        <option value="">Attach a listing…</option>
        <option v-for="l in attachable" :key="l.id" :value="l.id">{{ l.name }}</option>
      </select>
      <span class="ml-auto text-[11px]" :class="body.length > MAX ? 'font-semibold text-danger' : 'text-medium-grey'">
        {{ body.length.toLocaleString() }} / {{ MAX.toLocaleString() }}
      </span>
      <button v-if="props.editing" class="btn-secondary px-4 py-2 text-xs" @click="emit('cancel')">Cancel</button>
      <button class="btn-primary px-5 py-2 text-xs" :disabled="!canSubmit || saving" @click="submit">
        {{ saving ? (props.editing ? "Saving…" : "Posting…") : props.editing ? "Save" : "Post" }}
      </button>
    </div>
    <p v-if="!props.editing" class="text-[11px] text-medium-grey">
      Your followers are notified. Posts follow the <RouterLink to="/marketplace-rules" class="font-semibold underline underline-offset-2">marketplace rules</RouterLink>.
    </p>
  </div>
</template>
