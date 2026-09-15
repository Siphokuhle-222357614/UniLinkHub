<script setup lang="ts">
import NavBar from "@/components/NavBar.vue";
import AnnouncementBanner from "@/components/AnnouncementBanner.vue";
import CompareTray from "@/components/CompareTray.vue";
import ToastContainer from "@/components/ToastContainer.vue";
</script>

<template>
  <div class="min-h-screen bg-soft-grey">
    <NavBar />
    <AnnouncementBanner />
    <main class="mx-auto max-w-6xl px-4 py-6 pb-20 sm:px-6">
      <RouterView />
    </main>
    <CompareTray />
    <ToastContainer />
  </div>
</template>
