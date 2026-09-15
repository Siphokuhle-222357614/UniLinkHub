<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import type { SavedSearchDTO } from "@/lib/types";

const router = useRouter();
const searches = ref<SavedSearchDTO[]>([]);
const loading = ref(false);
const error = ref("");
const togglingId = ref<string | null>(null);
const deletingId = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<SavedSearchDTO[]>("/saved-searches");
    searches.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function toggleAlerts(s: SavedSearchDTO) {
  togglingId.value = s.id;
  try {
    await api.patch(`/saved-searches/${s.id}`, { alertsEnabled: !s.alertsEnabled });
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    togglingId.value = null;
  }
}

async function viewResults(s: SavedSearchDTO) {
  try {
    await api.post(`/saved-searches/${s.id}/view`);
  } catch {
    // Marking as viewed is best-effort; still navigate to the results either way.
  }
  router.push({
    name: "browse",
    query: {
      keyword: s.keyword || undefined,
      category: s.category || undefined,
      maxPrice: s.maxPrice ?? undefined,
      type: s.listingType || undefined,
    },
  });
}

async function remove(s: SavedSearchDTO) {
  deletingId.value = s.id;
  try {
    await api.delete(`/saved-searches/${s.id}`);
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    deletingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-4">
    <h1 class="font-display text-2xl font-bold text-uni-navy">My saved searches</h1>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="loading" class="text-sm text-medium-grey">Loading...</p>
    <div v-else-if="searches.length === 0" class="card text-sm text-medium-grey">
      Save a search from the browse page to get notified when new listings match it.
    </div>

    <div v-else class="space-y-3">
      <div v-for="s in searches" :key="s.id" class="card flex items-center justify-between gap-4">
        <div class="min-w-0">
          <p class="text-sm font-semibold text-charcoal">{{ s.label }}</p>
          <div class="mt-1 flex flex-wrap gap-1.5">
            <span v-if="s.category" class="badge bg-soft-grey text-medium-grey">Category: {{ s.category }}</span>
            <span v-if="s.maxPrice != null" class="badge bg-soft-grey text-medium-grey">Max R{{ s.maxPrice }}</span>
            <span v-if="s.listingType" class="badge bg-soft-grey text-medium-grey">{{ s.listingType === "PRODUCT" ? "Products" : "Services" }}</span>
          </div>
          <p class="mt-1.5 text-xs text-medium-grey">
            Saved {{ new Date(s.createdAt).toLocaleDateString("en-ZA") }}
            <template v-if="s.newMatchesCount > 0"> &middot; {{ s.newMatchesCount }} new match{{ s.newMatchesCount === 1 ? "" : "es" }}</template>
          </p>
        </div>
        <div class="flex shrink-0 items-center gap-3">
          <label class="flex items-center gap-1.5 text-xs text-medium-grey" title="Notify on new matches">
            <input type="checkbox" class="accent-campus-teal" :checked="s.alertsEnabled" :disabled="togglingId === s.id" @change="toggleAlerts(s)" />
            Alerts
          </label>
          <button class="btn-secondary text-xs" @click="viewResults(s)">View results</button>
          <button class="rounded-full p-1.5 text-medium-grey hover:bg-soft-grey hover:text-danger" :disabled="deletingId === s.id" @click="remove(s)" aria-label="Delete">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2L4 6" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
