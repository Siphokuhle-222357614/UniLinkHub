<script setup lang="ts">
import { TrendingDown, TrendingUp } from "@lucide/vue";
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import type { ListingDTO } from "@/lib/types";

const route = useRoute();

const listing = ref<ListingDTO | null>(null);
const siblingListings = ref<ListingDTO[]>([]);
const savedCounts = ref<Record<string, number>>({});
const error = ref("");

const STATUS_LABELS: Record<string, string> = { ACTIVE: "Active", INACTIVE: "Inactive", SOLD_OUT: "Sold out" };

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

function relativeDays(iso: string): string {
  const days = Math.round((Date.now() - new Date(iso).getTime()) / 86400000);
  if (days < 1) return "today";
  if (days === 1) return "1 day ago";
  return `${days} days ago`;
}

const maxViews = computed(() => Math.max(1, ...siblingListings.value.map((l) => l.viewCount)));

const saveRate = computed(() => {
  if (!listing.value || listing.value.viewCount === 0) return 0;
  return Math.round(((savedCounts.value[listing.value.id] ?? listing.value.savedCount) / listing.value.viewCount) * 100);
});

const businessAverageSaveRate = computed(() => {
  const rates = siblingListings.value
    .filter((l) => l.viewCount > 0)
    .map((l) => ((savedCounts.value[l.id] ?? l.savedCount) / l.viewCount) * 100);
  if (rates.length === 0) return 0;
  return Math.round(rates.reduce((a, b) => a + b, 0) / rates.length);
});

async function load() {
  error.value = "";
  try {
    const { data } = await api.get<ListingDTO>(`/listings/${route.params.id}`);
    listing.value = data;
    savedCounts.value[data.id] = data.savedCount;

    const { data: siblings } = await api.get<ListingDTO[]>(`/listings/business/${data.businessId}`);
    siblingListings.value = siblings.sort((a, b) => b.viewCount - a.viewCount);
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

onMounted(load);
</script>

<template>
  <section v-if="listing" class="mx-auto max-w-3xl space-y-4">
    <p class="text-sm text-medium-grey">
      <RouterLink to="/my-listings" class="hover:text-campus-teal">&larr; Back to My listings</RouterLink>
    </p>
    <div class="flex items-center justify-between">
      <div>
        <h1 class="font-display text-xl font-bold text-uni-navy">{{ listing.name }}</h1>
        <p class="text-sm text-medium-grey">{{ listing.category }} &middot; Listed {{ relativeDays(listing.createdAt) }}</p>
      </div>
      <span class="badge" :class="listing.status === 'ACTIVE' ? 'bg-success/15 text-success' : 'bg-medium-grey/15 text-medium-grey'">
        {{ STATUS_LABELS[listing.status] ?? listing.status }}
      </span>
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>

    <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <div class="card text-center">
        <p class="font-display text-2xl font-bold text-uni-navy">{{ listing.viewCount }}</p>
        <p class="text-xs text-medium-grey">Total views</p>
      </div>
      <div class="card text-center">
        <p class="font-display text-2xl font-bold text-uni-navy">{{ listing.savedCount }}</p>
        <p class="text-xs text-medium-grey">Saves</p>
      </div>
      <div class="card text-center">
        <p class="font-display text-2xl font-bold text-uni-navy">{{ formatPrice(listing.price) }}</p>
        <p class="text-xs text-medium-grey">Price</p>
      </div>
      <div class="card text-center">
        <p class="font-display text-2xl font-bold text-uni-navy">{{ saveRate }}%</p>
        <p class="text-xs text-medium-grey">Save rate</p>
      </div>
    </div>

    <div v-if="siblingListings.length > 1" class="card space-y-3">
      <h2 class="font-display text-sm font-semibold text-uni-navy">How this compares to your other listings</h2>
      <div class="space-y-2">
        <div v-for="l in siblingListings" :key="l.id">
          <div class="mb-1 flex items-center justify-between text-xs text-medium-grey">
            <span :class="{ 'font-semibold text-uni-navy': l.id === listing.id }">{{ l.name }}</span>
            <span>{{ l.viewCount }} views</span>
          </div>
          <div class="h-2.5 overflow-hidden rounded-full bg-soft-grey">
            <div
              class="h-full rounded-full"
              :class="l.id === listing.id ? 'bg-campus-teal' : 'bg-sky-blue'"
              :style="{ width: `${Math.max(4, (l.viewCount / maxViews) * 100)}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="listing.viewCount > 0" class="card">
      <p class="text-sm text-charcoal">
        <template v-if="saveRate >= businessAverageSaveRate">
          <TrendingUp class="mr-1 inline h-4 w-4 text-success" /><b>{{ saveRate }}%</b> of viewers saved this listing - at or above your business average of {{ businessAverageSaveRate }}%.
        </template>
        <template v-else>
          <TrendingDown class="mr-1 inline h-4 w-4 text-warning" /><b>{{ saveRate }}%</b> of viewers saved this listing, below your business average of {{ businessAverageSaveRate }}%.
        </template>
      </p>
    </div>
  </section>

  <p v-else-if="error" class="text-sm text-danger">{{ error }}</p>
  <p v-else class="text-sm text-medium-grey">Loading...</p>
</template>
