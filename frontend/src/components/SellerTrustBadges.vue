<script setup lang="ts">
import { BadgeCheck, MessageCircle, PackageCheck, Star } from "@lucide/vue";
import type { SellerTrust } from "@/lib/types";

/** compact: one line for listing cards. Full: chips for the listing page and provider profile. */
const props = withDefaults(defineProps<{ trust: SellerTrust; compact?: boolean }>(), { compact: false });
</script>

<template>
  <p v-if="props.compact" class="flex min-w-0 items-center gap-2 truncate text-[11px] text-medium-grey">
    <span v-if="props.trust.rating !== null" class="inline-flex items-center gap-0.5 font-semibold text-charcoal">
      <Star class="h-3 w-3 fill-gold-400 text-gold-400" /> {{ props.trust.rating.toFixed(1) }}
      <span class="font-normal text-medium-grey">({{ props.trust.reviewCount }})</span>
    </span>
    <span v-if="props.trust.responseTimeLabel" class="truncate">Replies {{ props.trust.responseTimeLabel }}</span>
    <span v-else-if="props.trust.completedOrders > 0">{{ props.trust.completedOrders }} sold</span>
  </p>

  <ul v-else class="flex flex-wrap gap-2" aria-label="Seller trust signals">
    <li v-if="props.trust.verified" class="trust-chip"><BadgeCheck class="h-3.5 w-3.5 text-emerald-500" /> Verified</li>
    <li class="trust-chip">
      <Star class="h-3.5 w-3.5 fill-gold-400 text-gold-400" />
      <template v-if="props.trust.rating !== null">{{ props.trust.rating.toFixed(1) }} from {{ props.trust.reviewCount }} review{{ props.trust.reviewCount === 1 ? "" : "s" }}</template>
      <template v-else>No reviews yet</template>
    </li>
    <li class="trust-chip">
      <PackageCheck class="h-3.5 w-3.5 text-teal-600" />
      {{ props.trust.completedOrders }} order{{ props.trust.completedOrders === 1 ? "" : "s" }} completed
    </li>
    <li v-if="props.trust.responseTimeLabel" class="trust-chip">
      <MessageCircle class="h-3.5 w-3.5 text-navy-500" /> Usually replies {{ props.trust.responseTimeLabel }}
    </li>
  </ul>
</template>

<style scoped>
.trust-chip {
  @apply inline-flex items-center gap-1.5 rounded-full border border-light-grey bg-white px-2.5 py-1 text-xs font-medium text-charcoal;
}
</style>
