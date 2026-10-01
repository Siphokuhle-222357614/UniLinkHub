<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Bookmark,
  CalendarDays,
  ChevronDown,
  History,
  LayoutDashboard,
  LogOut,
  MessageCircle,
  CircleQuestionMark,
  Scale,
  Package,
  Receipt,
  Settings,
  ShieldCheck,
  Store,
  TicketPercent,
} from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { useClickOutside } from "@/composables/useClickOutside";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const open = ref(false);
const root = ref<HTMLElement | null>(null);
useClickOutside(root, () => (open.value = false));
watch(() => route.fullPath, () => (open.value = false));

const initials = computed(() => {
  const first = auth.user?.firstName?.[0] ?? "";
  const last = auth.user?.lastName?.[0] ?? "";
  return (first + last).toUpperCase() || "?";
});

const buyerLinks = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/orders", label: "My orders", icon: Receipt },
  { to: "/bookings", label: "My bookings", icon: CalendarDays },
  { to: "/messages", label: "Messages", icon: MessageCircle },
  { to: "/saved-searches", label: "Saved searches", icon: Bookmark },
  { to: "/recently-viewed", label: "Recently viewed", icon: History },
];

const sellerLinks = [
  { to: "/my-listings", label: "My listings", icon: Store },
  { to: "/orders/selling", label: "Orders to fulfil", icon: Package },
  { to: "/questions", label: "Listing Q&A", icon: CircleQuestionMark },
  { to: "/promo-codes", label: "Promo codes", icon: TicketPercent },
];

function logout() {
  open.value = false;
  auth.logout();
  router.push({ name: "browse" });
}
</script>

<template>
  <div ref="root" class="relative">
    <button
      class="flex items-center gap-1.5 rounded-full p-0.5 pr-1.5 transition hover:bg-navy-50"
      :aria-expanded="open"
      aria-haspopup="menu"
      aria-label="Account menu"
      @click="open = !open"
    >
      <span
        class="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-uni-navy to-teal-500 font-display text-xs font-bold text-white ring-2 ring-white"
      >
        {{ initials }}
      </span>
      <ChevronDown class="hidden h-4 w-4 text-medium-grey transition sm:block" :class="{ 'rotate-180': open }" />
    </button>

    <div
      v-if="open"
      role="menu"
      class="absolute right-0 top-[calc(100%+10px)] z-40 w-64 origin-top-right animate-scale-in overflow-hidden rounded-card border border-light-grey bg-white shadow-pop"
    >
      <div class="border-b border-light-grey bg-soft-grey/60 px-4 py-3">
        <p class="truncate text-sm font-semibold text-charcoal">{{ auth.user?.firstName }} {{ auth.user?.lastName }}</p>
        <p class="truncate text-xs text-medium-grey">{{ auth.user?.email }}</p>
        <div class="mt-2 flex gap-1.5">
          <span v-if="auth.isSeller" class="badge bg-teal-50 text-teal-700">Seller</span>
          <span v-if="auth.isAdmin" class="badge bg-gold-50 text-gold-700">Admin</span>
          <span v-if="!auth.isSeller && !auth.isAdmin" class="badge bg-navy-50 text-navy-600">Student</span>
        </div>
      </div>

      <nav class="max-h-[60vh] overflow-y-auto p-1.5">
        <template v-if="!auth.isAdmin">
          <RouterLink v-for="l in buyerLinks" :key="l.to" :to="l.to" class="menu-item" role="menuitem">
            <component :is="l.icon" class="h-4 w-4" /> {{ l.label }}
          </RouterLink>
        </template>

        <template v-if="auth.isSeller">
          <p class="px-3 pb-1 pt-3 text-[11px] font-semibold uppercase tracking-wider text-medium-grey">Selling</p>
          <RouterLink v-for="l in sellerLinks" :key="l.to" :to="l.to" class="menu-item" role="menuitem">
            <component :is="l.icon" class="h-4 w-4" /> {{ l.label }}
          </RouterLink>
        </template>

        <div v-if="!auth.isAdmin" class="my-1.5 h-px bg-light-grey"></div>
        <RouterLink v-if="auth.isAdmin" to="/admin" class="menu-item" role="menuitem">
          <ShieldCheck class="h-4 w-4 text-gold-600" /> Admin console
        </RouterLink>
        <RouterLink to="/account" class="menu-item" role="menuitem">
          <Settings class="h-4 w-4" /> Account settings
        </RouterLink>
        <RouterLink to="/marketplace-rules" class="menu-item" role="menuitem">
          <Scale class="h-4 w-4" /> Marketplace rules
        </RouterLink>
        <button class="menu-item w-full !text-danger hover:!bg-red-50" role="menuitem" @click="logout">
          <LogOut class="h-4 w-4" /> Log out
        </button>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.menu-item {
  @apply flex items-center gap-2.5 rounded-control px-3 py-2 text-sm font-medium text-charcoal transition hover:bg-navy-50 hover:text-uni-navy;
}
.menu-item.router-link-exact-active {
  @apply bg-navy-50 text-uni-navy;
}
</style>
