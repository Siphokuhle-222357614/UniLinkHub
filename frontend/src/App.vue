<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import NavBar from "@/components/NavBar.vue";
import AnnouncementBanner from "@/components/AnnouncementBanner.vue";
import CompareTray from "@/components/CompareTray.vue";
import ToastContainer from "@/components/ToastContainer.vue";
import CommandPalette from "@/components/CommandPalette.vue";
import MobileTabBar from "@/components/MobileTabBar.vue";
import AppFooter from "@/components/AppFooter.vue";

const route = useRoute();

// "bare" routes (auth screens, the admin console) bring their own full-screen chrome.
const bare = computed(() => route.meta.layout === "bare");
</script>

<template>
  <RouterView v-if="bare" v-slot="{ Component }">
    <component :is="Component" />
  </RouterView>

  <div v-else class="flex min-h-screen flex-col">
    <NavBar />
    <AnnouncementBanner />
    <main class="mx-auto w-full max-w-7xl flex-1 px-4 pb-28 pt-6 sm:px-6 sm:pt-8 md:pb-12">
      <RouterView v-slot="{ Component, route: r }">
        <Transition name="page" mode="out-in">
          <!-- Keyed by path so /listings/a -> /listings/b remounts and reloads the view. -->
          <div :key="r.path">
            <component :is="Component" />
          </div>
        </Transition>
      </RouterView>
    </main>
    <div class="pb-16 md:pb-0">
      <AppFooter />
    </div>
    <CompareTray />
    <MobileTabBar />
  </div>

  <CommandPalette />
  <ToastContainer />
</template>
