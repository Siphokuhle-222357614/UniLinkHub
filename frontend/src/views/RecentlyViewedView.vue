<script setup lang="ts">
import { ref } from "vue";
import { clearRecentlyViewed, getRecentlyViewed } from "@/lib/recentlyViewed";
import ListingCard from "@/components/ListingCard.vue";

const recentlyViewed = ref(getRecentlyViewed());

function clear() {
  clearRecentlyViewed();
  recentlyViewed.value = [];
}
</script>

<template>
  <section class="space-y-5">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="font-display text-xl font-bold text-uni-navy">Recently viewed</h1>
        <p class="text-sm text-medium-grey">Tracked on this device only &middot; last {{ recentlyViewed.length }} listings</p>
      </div>
      <button v-if="recentlyViewed.length > 0" class="inline-flex items-center gap-1.5 rounded-control border border-danger bg-white px-3 py-2 text-sm font-semibold text-danger" @click="clear">
        Clear history
      </button>
    </div>

    <div v-if="recentlyViewed.length === 0" class="card text-sm text-medium-grey">
      Listings you open will show up here so you can find your way back to them.
    </div>
    <div v-else class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">
      <ListingCard v-for="listing in recentlyViewed" :key="listing.id" :listing="listing" />
    </div>
  </section>
</template>
