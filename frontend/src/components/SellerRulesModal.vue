<script setup lang="ts">
import { ref } from "vue";
import { Ban, Gavel, Store, X } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";
import { CONSEQUENCES, useRestrictedCategories } from "@/lib/rules";

const emit = defineEmits<{ close: []; accepted: [] }>();

const auth = useAuthStore();
const toast = useToastStore();
const categories = useRestrictedCategories();
const agreed = ref(false);
const saving = ref(false);

async function accept() {
  if (!agreed.value) return;
  saving.value = true;
  try {
    await auth.becomeSeller(true);
    toast.success("You're now a seller!", "Add your first business to start listing.");
    emit("accepted");
  } catch {
    // The API interceptor shows the reason.
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <div v-dialog="() => emit('close')" class="modal-panel flex max-h-[90vh] max-w-2xl flex-col p-0" role="dialog" aria-modal="true" aria-labelledby="rules-title">
        <div class="flex items-start justify-between gap-3 border-b border-light-grey px-6 py-5">
          <div>
            <p class="eyebrow">Before you start selling</p>
            <h2 id="rules-title" class="mt-1 font-display text-xl font-bold text-uni-navy">UniLinkHub marketplace rules</h2>
          </div>
          <button class="btn-icon h-9 w-9" aria-label="Close" @click="emit('close')"><X class="h-5 w-5" /></button>
        </div>

        <div class="space-y-5 overflow-y-auto px-6 py-5">
          <p class="text-sm leading-relaxed text-charcoal">
            UniLinkHub is for legal, honest student businesses. These things <span class="font-semibold">can't be sold here or in CPUT
            residences</span> - listings that include them are blocked automatically, and anything that slips through is removed.
          </p>

          <ul class="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
            <li v-for="c in categories" :key="c.key" class="flex gap-3 rounded-control border border-red-100 bg-red-50/60 p-3">
              <Ban class="mt-0.5 h-4 w-4 shrink-0 text-danger" />
              <div>
                <p class="text-sm font-semibold text-charcoal">{{ c.label }}</p>
                <p class="text-xs leading-relaxed text-medium-grey">{{ c.description }}</p>
              </div>
            </li>
            <li v-if="categories.length === 0" class="skeleton h-16 rounded-control sm:col-span-2"></li>
          </ul>

          <div class="rounded-control border border-amber-200 bg-amber-50 p-4">
            <p class="flex items-center gap-2 text-sm font-semibold text-amber-900"><Gavel class="h-4 w-4" /> If someone breaks the rules</p>
            <ul class="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-900">
              <li v-for="c in CONSEQUENCES" :key="c">{{ c }}</li>
            </ul>
          </div>

          <label class="flex cursor-pointer items-start gap-3 rounded-control border border-light-grey p-3.5 transition hover:bg-soft-grey">
            <input v-model="agreed" type="checkbox" class="mt-0.5 h-4 w-4" />
            <span class="text-sm text-charcoal">
              I've read the rules. I won't sell restricted items, and I understand my account can be suspended and reported if I do.
            </span>
          </label>
        </div>

        <div class="flex flex-col-reverse gap-2 border-t border-light-grey px-4 py-4 sm:flex-row sm:px-6">
          <button class="btn-secondary flex-1" @click="emit('close')">Not now</button>
          <button class="btn-primary flex-1" :disabled="!agreed || saving" @click="accept">
            <Store class="h-4 w-4" /> {{ saving ? "Setting up…" : "Agree & become a seller" }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
