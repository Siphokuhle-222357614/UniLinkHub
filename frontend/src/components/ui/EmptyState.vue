<script setup lang="ts">
import type { Component } from "vue";

defineProps<{ icon: Component; title: string; description?: string }>();
</script>

<template>
  <div class="flex flex-col items-center rounded-card border border-dashed border-light-grey bg-white/60 px-6 py-12 text-center">
    <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-navy-50 to-teal-50 ring-1 ring-navy-100">
      <component :is="icon" class="h-6 w-6 text-navy-500" />
    </div>
    <p class="mt-4 font-display text-base font-semibold text-uni-navy">{{ title }}</p>
    <p v-if="description" class="mt-1 max-w-sm text-sm text-medium-grey">{{ description }}</p>
    <div v-if="$slots.default" class="mt-5"><slot /></div>
  </div>
</template>
