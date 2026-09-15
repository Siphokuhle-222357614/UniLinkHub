<script setup lang="ts">
import LogoMark from "@/components/LogoMark.vue";

withDefaults(defineProps<{ markSize?: number; showTagline?: boolean }>(), {
  markSize: 96,
  showTagline: true,
});
</script>

<template>
  <div class="flex flex-col items-center">
    <LogoMark :size="markSize" />
    <p class="mt-1 font-display text-2xl font-bold tracking-tight">
      <span class="text-uni-navy">Uni</span><span class="text-campus-teal">Link</span><span class="text-sky-blue">Hub</span>
    </p>
    <div v-if="showTagline" class="mt-1.5 flex items-center gap-2">
      <span class="h-px w-10 bg-uni-navy/60"></span>
      <span class="h-1 w-1 rounded-full bg-academic-gold"></span>
      <span class="h-px w-10 bg-uni-navy/60"></span>
    </div>
    <p v-if="showTagline" class="mt-1 text-[11px] font-medium tracking-[0.2em] text-uni-navy">
      CONNECT. BUY. SELL &amp; SUCCEED
    </p>
  </div>
</template>
