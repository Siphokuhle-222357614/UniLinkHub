<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import NotificationBell from "@/components/NotificationBell.vue";
import MessagesIcon from "@/components/MessagesIcon.vue";
import CartDrawer from "@/components/CartDrawer.vue";
import LogoMark from "@/components/LogoMark.vue";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const mobileMenuOpen = ref(false);

function closeMobileMenu() {
  mobileMenuOpen.value = false;
}

function handleLogout() {
  closeMobileMenu();
  auth.logout();
  router.push({ name: "browse" });
}

// A nav link click already navigates away, but a link to the CURRENT route (e.g. tapping
// "Browse" while already on it) fires no navigation event for the watcher below to catch.
watch(() => route.fullPath, closeMobileMenu);
</script>

<template>
  <header class="relative border-b border-light-grey bg-white">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
      <RouterLink to="/" class="flex min-w-0 items-center gap-2" @click="closeMobileMenu">
        <LogoMark :size="28" />
        <span class="truncate font-display text-lg font-bold text-uni-navy sm:text-xl">UniLinkHub</span>
        <span class="hidden text-xs text-medium-grey lg:inline">Connect. Buy. Sell &amp; Succeed.</span>
      </RouterLink>

      <!-- Desktop nav (>=640px) -->
      <nav class="hidden items-center gap-3 sm:flex">
        <RouterLink to="/" class="text-sm font-medium text-charcoal hover:text-campus-teal">
          Browse
        </RouterLink>
        <RouterLink to="/providers" class="text-sm font-medium text-charcoal hover:text-campus-teal">
          Providers
        </RouterLink>

        <template v-if="auth.isAuthenticated">
          <RouterLink to="/dashboard" class="text-sm font-medium text-charcoal hover:text-campus-teal">
            Dashboard
          </RouterLink>
          <RouterLink to="/account" class="text-sm font-medium text-charcoal hover:text-campus-teal">
            My account
          </RouterLink>
          <RouterLink
            v-if="auth.isAdmin"
            to="/admin"
            class="rounded-full border border-academic-gold bg-academic-gold/15 px-2.5 py-1 text-xs font-semibold text-uni-navy hover:bg-academic-gold/25"
          >
            Admin console
          </RouterLink>
          <CartDrawer />
          <MessagesIcon />
          <NotificationBell />
          <button class="btn-secondary text-sm" @click="handleLogout">Log out</button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="text-sm font-medium text-charcoal hover:text-campus-teal">
            Log in
          </RouterLink>
          <RouterLink to="/register" class="btn-primary text-sm">Get Started</RouterLink>
        </template>
      </nav>

      <!-- Mobile: always-visible quick actions + menu toggle (<640px) -->
      <div class="flex shrink-0 items-center gap-1.5 sm:hidden">
        <template v-if="auth.isAuthenticated">
          <CartDrawer />
          <NotificationBell />
        </template>
        <button
          class="flex h-9 w-9 items-center justify-center rounded-control border border-light-grey"
          :aria-expanded="mobileMenuOpen"
          aria-label="Menu"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <svg v-if="!mobileMenuOpen" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
            <path d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Mobile dropdown panel -->
    <div v-if="mobileMenuOpen" class="border-t border-light-grey bg-white px-4 py-3 sm:hidden">
      <nav class="flex flex-col gap-1">
        <RouterLink to="/" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
          Browse
        </RouterLink>
        <RouterLink to="/providers" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
          Providers
        </RouterLink>

        <template v-if="auth.isAuthenticated">
          <RouterLink to="/dashboard" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
            Dashboard
          </RouterLink>
          <RouterLink to="/messages" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
            Messages
          </RouterLink>
          <RouterLink to="/account" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
            My account
          </RouterLink>
          <RouterLink
            v-if="auth.isAdmin"
            to="/admin"
            class="mx-3 mt-1 w-fit rounded-full border border-academic-gold bg-academic-gold/15 px-2.5 py-1 text-xs font-semibold text-uni-navy"
            @click="closeMobileMenu"
          >
            Admin console
          </RouterLink>
          <button class="mt-2 border-t border-light-grey px-3 pt-3 text-left text-sm font-semibold text-danger" @click="handleLogout">
            Log out
          </button>
        </template>
        <template v-else>
          <RouterLink to="/login" class="rounded-control px-3 py-2.5 text-sm font-medium text-charcoal hover:bg-soft-grey" @click="closeMobileMenu">
            Log in
          </RouterLink>
          <RouterLink to="/register" class="btn-primary mt-1 text-center text-sm" @click="closeMobileMenu">Get Started</RouterLink>
        </template>
      </nav>
    </div>
  </header>
</template>
