<script setup lang="ts">
import { ArrowRight, Columns3, X } from "@lucide/vue";
import { useCompareStore } from "@/stores/compare";

const compare = useCompareStore();
</script>

<template>
  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="translate-y-6 opacity-0"
    leave-active-class="transition duration-200 ease-in"
    leave-to-class="translate-y-6 opacity-0"
  >
    <div v-if="compare.ids.length > 0" class="fixed inset-x-3 bottom-20 z-30 flex justify-center md:bottom-6">
      <div class="flex w-full max-w-md items-center gap-3 rounded-full bg-navy-900/95 py-2 pl-5 pr-2 text-white shadow-pop backdrop-blur">
        <Columns3 class="h-4 w-4 shrink-0 text-teal-300" />
        <p class="flex-1 text-sm font-medium">
          <span class="font-bold">{{ compare.ids.length }}</span> listing{{ compare.ids.length === 1 ? "" : "s" }} selected
        </p>
        <button class="rounded-full p-2 text-white/60 transition hover:bg-white/10 hover:text-white" aria-label="Clear comparison" @click="compare.clear()">
          <X class="h-4 w-4" />
        </button>
        <RouterLink to="/compare" class="inline-flex items-center gap-1.5 rounded-full bg-teal-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-teal-400">
          Compare <ArrowRight class="h-4 w-4" />
        </RouterLink>
      </div>
    </div>
  </Transition>
</template>
