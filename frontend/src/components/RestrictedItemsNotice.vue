<script setup lang="ts">
import { ShieldAlert } from "@lucide/vue";
</script>

<template>
  <div class="flex gap-3 rounded-control border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
    <ShieldAlert class="mt-0.5 h-4 w-4 shrink-0 text-warning" />
    <p class="leading-relaxed">
      <span class="font-semibold">Restricted items aren't allowed:</span> no alcohol, drugs, weapons, cigarettes or vapes, cheating
      services, or anything fake or stolen. Listings that break the rules are removed, and sellers can be suspended and reported to
      CPUT residence management.
      <RouterLink to="/marketplace-rules" class="font-semibold underline underline-offset-2">Read the marketplace rules</RouterLink>
    </p>
  </div>
</template>
