<script setup lang="ts">
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import NotificationBell from "@/components/NotificationBell.vue";
import MessagesIcon from "@/components/MessagesIcon.vue";
import CartDrawer from "@/components/CartDrawer.vue";

const auth = useAuthStore();
const router = useRouter();

function handleLogout() {
  auth.logout();
  router.push({ name: "browse" });
}
</script>

<template>
  <header class="border-b border-light-grey bg-white">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
      <RouterLink to="/" class="flex items-baseline gap-2">
        <span class="font-display text-xl font-bold text-uni-navy">UniLinkHub</span>
        <span class="hidden text-xs text-medium-grey sm:inline">Connect. Buy. Sell &amp; Succeed.</span>
      </RouterLink>

      <nav class="flex items-center gap-3">
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
    </div>
  </header>
</template>
