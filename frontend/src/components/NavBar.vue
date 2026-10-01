<script setup lang="ts">
import { computed } from "vue";
import { Search } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { useCommandPaletteStore } from "@/stores/commandPalette";
import NotificationBell from "@/components/NotificationBell.vue";
import MessagesIcon from "@/components/MessagesIcon.vue";
import CartDrawer from "@/components/CartDrawer.vue";
import LogoMark from "@/components/LogoMark.vue";
import ThemeToggleButton from "@/components/ThemeToggleButton.vue";
import UserMenu from "@/components/UserMenu.vue";

const auth = useAuthStore();
const palette = useCommandPaletteStore();

const isMac = typeof navigator !== "undefined" && /Mac|iPhone|iPad/.test(navigator.platform);
const shortcut = isMac ? "⌘K" : "Ctrl K";

const links = computed(() => {
  const base = [
    { to: "/", label: "Explore", exact: true },
    { to: "/providers", label: "Providers", exact: false },
  ];
  if (auth.isAdmin) {
    base.push({ to: "/admin", label: "Admin console", exact: false });
  } else if (auth.isAuthenticated) {
    base.push({ to: "/dashboard", label: "Dashboard", exact: false });
    if (auth.isSeller) base.push({ to: "/my-listings", label: "My listings", exact: false });
  }
  return base;
});
</script>

<template>
  <header class="sticky top-0 z-30 border-b border-light-grey/70 bg-white/80 backdrop-blur-xl supports-[backdrop-filter]:bg-white/70">
    <div class="mx-auto flex h-16 max-w-7xl 2xl:max-w-[1400px] 3xl:max-w-[1600px] items-center gap-3 px-4 sm:px-6 lg:gap-6">
      <RouterLink to="/" class="flex shrink-0 items-center gap-2" aria-label="UniLinkHub home">
        <LogoMark :size="32" />
        <span class="font-display text-lg font-bold tracking-tight text-uni-navy" :class="{ 'max-[379px]:hidden md:max-lg:hidden': auth.isAuthenticated }">
          Uni<span class="text-teal-500">Link</span>Hub
        </span>
      </RouterLink>

      <nav class="hidden items-center gap-1 md:flex">
        <RouterLink
          v-for="l in links"
          :key="l.to"
          :to="l.to"
          class="nav-link"
          :class="{ 'nav-link-active': l.exact ? $route.path === l.to : $route.path.startsWith(l.to) }"
        >
          {{ l.label }}
        </RouterLink>
      </nav>

      <button
        class="group ml-auto hidden h-10 w-full min-w-0 max-w-xs items-center gap-2.5 rounded-full border border-light-grey bg-soft-grey/70 px-4 text-sm text-medium-grey transition hover:border-navy-200 hover:bg-white sm:flex md:hidden lg:flex lg:max-w-sm"
        @click="palette.show()"
      >
        <Search class="h-4 w-4 text-slate-400 group-hover:text-teal-600" />
        <span class="flex-1 truncate text-left">Search listings, sellers…</span>
        <kbd class="hidden rounded-md border border-light-grey bg-white px-1.5 py-0.5 font-sans text-[11px] font-semibold text-medium-grey lg:inline">
          {{ shortcut }}
        </kbd>
      </button>

      <!-- Phones and tablets (where the page links take the room) get a search icon; the full box returns at lg. -->
      <div class="ml-auto flex shrink-0 items-center gap-0.5 sm:ml-0 sm:gap-1 md:ml-auto lg:ml-0">
        <button class="btn-icon sm:hidden md:inline-flex lg:hidden" aria-label="Search" @click="palette.show()">
          <Search class="h-5 w-5" />
        </button>
        <ThemeToggleButton />

        <template v-if="auth.isAuthenticated">
          <!-- Admin accounts don't buy or message sellers, so they get no cart or inbox. -->
          <CartDrawer v-if="!auth.isAdmin" />
          <div v-if="!auth.isAdmin" class="hidden md:block"><MessagesIcon /></div>
          <NotificationBell />
          <div class="ml-1"><UserMenu /></div>
        </template>
        <template v-else>
          <RouterLink to="/login" class="btn-ghost px-3">Log in</RouterLink>
          <RouterLink to="/register" class="btn-primary hidden px-4 sm:inline-flex">Get started</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.nav-link {
  @apply rounded-full px-2.5 py-2 text-sm font-medium lg:px-3.5 text-medium-grey transition hover:bg-navy-50 hover:text-uni-navy;
}
.nav-link-active {
  @apply bg-navy-50 text-uni-navy;
}
</style>
