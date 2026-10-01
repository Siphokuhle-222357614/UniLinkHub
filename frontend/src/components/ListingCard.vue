<script setup lang="ts">
import { computed, ref } from "vue";
import { CalendarClock, Columns3, Eye, Heart, MapPin, Package } from "@lucide/vue";
import SellerTrustBadges from "@/components/SellerTrustBadges.vue";
import type { ListingDTO } from "@/lib/types";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useCompareStore } from "@/stores/compare";
import { categoryMeta } from "@/lib/categoryMeta";
import { formatPrice } from "@/lib/format";

const props = defineProps<{ listing: ListingDTO }>();

const auth = useAuthStore();
const saved = useSavedListingsStore();
const compare = useCompareStore();

// Seeded/pasted image URLs can go dead; fall back to the category placeholder instead of a broken image.
const imageFailed = ref(false);

const meta = computed(() => categoryMeta(props.listing.category));
const isSaved = computed(() => saved.isSaved(props.listing.id));
const comparing = computed(() => compare.isComparing(props.listing.id));
const compareDisabled = computed(() => !comparing.value && compare.isFull);

const isNew = computed(() => Date.now() - new Date(props.listing.createdAt).getTime() < 3 * 86400000);
const lowStock = computed(() => {
  const l = props.listing;
  return l.type === "PRODUCT" && l.status === "ACTIVE" && l.lowStockThreshold != null && (l.stockQuantity ?? 0) <= l.lowStockThreshold
    ? l.stockQuantity ?? 0
    : null;
});
</script>

<template>
  <RouterLink
    :to="`/listings/${listing.id}`"
    class="group card card-interactive relative flex flex-col overflow-hidden p-0"
    :class="{ 'opacity-75 saturate-50': listing.status !== 'ACTIVE' }"
  >
    <div class="relative aspect-[4/3] overflow-hidden bg-soft-grey">
      <img
        v-if="listing.imageUrl && !imageFailed"
        :src="listing.imageUrl"
        :alt="listing.name"
        loading="lazy"
        class="h-full w-full object-cover transition duration-500 ease-out group-hover:scale-105"
        @error="imageFailed = true"
      />
      <div v-else class="flex h-full w-full items-center justify-center" :style="{ background: meta.gradient }">
        <component :is="meta.icon" class="h-12 w-12 text-navy-900/15 transition duration-500 group-hover:scale-110" :stroke-width="1.5" />
      </div>

      <div class="absolute left-2 top-2 flex flex-wrap gap-1.5 sm:left-3 sm:top-3">
        <span v-if="listing.status === 'SOLD_OUT'" class="badge bg-navy-900/85 text-white backdrop-blur">Sold out</span>
        <span v-else-if="listing.status === 'INACTIVE'" class="badge bg-danger/90 text-white">Inactive</span>
        <template v-else>
          <span class="badge hidden bg-white/90 text-navy-700 shadow-xs backdrop-blur sm:inline-flex">
            <component :is="listing.type === 'PRODUCT' ? Package : CalendarClock" class="h-3 w-3" />
            {{ listing.type === "PRODUCT" ? "Product" : "Service" }}
          </span>
          <span v-if="isNew" class="badge bg-teal-500 text-white shadow-xs">New</span>
        </template>
      </div>

      <button
        v-if="auth.isAuthenticated && !auth.isAdmin"
        class="absolute right-2 top-2 flex h-8 w-8 sm:right-3 sm:top-3 sm:h-9 sm:w-9 items-center justify-center rounded-full bg-white/90 shadow-xs backdrop-blur transition hover:scale-110 active:scale-95"
        :aria-label="isSaved ? 'Unsave listing' : 'Save listing'"
        :aria-pressed="isSaved"
        @click.stop.prevent="saved.toggleSave(props.listing)"
      >
        <Heart class="h-4 w-4 transition" :class="isSaved ? 'fill-danger text-danger' : 'text-navy-700'" />
      </button>

      <span
        v-if="lowStock !== null"
        class="badge absolute bottom-2 left-2 bg-warning sm:bottom-3 sm:left-3 text-white shadow-xs"
      >
        Only {{ lowStock }} left
      </span>
    </div>

    <div class="flex flex-1 flex-col p-3 sm:p-4">
      <p class="flex min-w-0 items-center gap-1.5 truncate text-[10px] font-semibold uppercase tracking-wider text-medium-grey sm:text-[11px]">
        <span class="flex h-5 w-5 items-center justify-center rounded-md" :class="meta.tile">
          <component :is="meta.icon" class="h-3 w-3" />
        </span>
        {{ listing.category }}
      </p>
      <h3 class="mt-1.5 line-clamp-2 font-display text-sm leading-snug sm:mt-2 sm:line-clamp-1 sm:text-base font-semibold text-uni-navy transition group-hover:text-teal-700">
        {{ listing.name }}
      </h3>
      <div class="hidden sm:block"><p class="mt-1 line-clamp-2 text-sm leading-relaxed text-medium-grey">{{ listing.description }}</p></div>
      <p v-if="listing.campusLabel" class="mt-2 flex min-w-0 items-center gap-1 truncate text-[11px] text-medium-grey">
        <MapPin class="h-3 w-3 shrink-0 text-teal-600" />
        <span class="truncate">{{ listing.campusLabel }}<template v-if="listing.pickupLocation"> · {{ listing.pickupLocation }}</template></span>
      </p>
      <SellerTrustBadges v-if="listing.seller" :trust="listing.seller" compact class="mt-1" />

      <div class="mt-auto flex items-end justify-between gap-2 pt-3 sm:pt-4">
        <div>
          <p class="font-display text-base font-bold text-uni-navy sm:text-lg">{{ formatPrice(listing.price) }}</p>
          <p class="flex items-center gap-3 text-[11px] text-medium-grey">
            <span class="inline-flex items-center gap-1"><Eye class="h-3 w-3" /> {{ listing.viewCount }}</span>
            <span v-if="listing.savedCount > 0" class="inline-flex items-center gap-1"><Heart class="h-3 w-3" /> {{ listing.savedCount }}</span>
          </p>
        </div>
        <button
          class="inline-flex shrink-0 items-center gap-1 rounded-full border p-1.5 text-[11px] sm:px-2.5 sm:py-1 font-semibold transition disabled:cursor-not-allowed disabled:opacity-40"
          :class="comparing ? 'border-teal-500 bg-teal-50 text-teal-700' : 'border-light-grey text-medium-grey hover:border-navy-200 hover:text-uni-navy'"
          :disabled="compareDisabled"
          :aria-pressed="comparing"
          :aria-label="comparing ? 'Remove from comparison' : 'Add to comparison'"
          :title="compareDisabled ? 'You can compare up to 3 listings' : 'Add to comparison'"
          @click.stop.prevent="compare.toggle(listing.id)"
        >
          <Columns3 class="h-3 w-3" />
          <span class="hidden sm:inline">{{ comparing ? "Comparing" : "Compare" }}</span>
        </button>
      </div>
    </div>
  </RouterLink>
</template>
