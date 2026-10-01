<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { Compass, LayoutDashboard, LogIn, MessageCircle, Scale, Search, ShieldCheck, UserPlus, Users } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { useMessagesStore } from "@/stores/messages";
import { useCommandPaletteStore } from "@/stores/commandPalette";

const auth = useAuthStore();
const messages = useMessagesStore();
const palette = useCommandPaletteStore();
const route = useRoute();

const tabs = computed(() =>
  auth.isAdmin
    ? [
        { to: "/", label: "Explore", icon: Compass, exact: true },
        { to: "/providers", label: "Providers", icon: Users },
        { action: "search", label: "Search", icon: Search },
        { to: "/admin", label: "Admin", icon: ShieldCheck },
        { to: "/marketplace-rules", label: "Rules", icon: Scale },
      ]
    : auth.isAuthenticated
    ? [
        { to: "/", label: "Explore", icon: Compass, exact: true },
        { to: "/providers", label: "Providers", icon: Users },
        { action: "search", label: "Search", icon: Search },
        { to: "/messages", label: "Messages", icon: MessageCircle, badge: messages.unreadCount },
        { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
      ]
    : [
        { to: "/", label: "Explore", icon: Compass, exact: true },
        { to: "/providers", label: "Providers", icon: Users },
        { action: "search", label: "Search", icon: Search },
        { to: "/login", label: "Log in", icon: LogIn },
        { to: "/register", label: "Join", icon: UserPlus },
      ],
);

function isActive(tab: { to?: string; exact?: boolean }) {
  if (!tab.to) return palette.open;
  return tab.exact ? route.path === tab.to : route.path.startsWith(tab.to);
}
</script>

<template>
  <nav class="fixed inset-x-0 bottom-0 z-30 border-t border-light-grey/80 bg-white/90 pb-safe backdrop-blur-xl md:hidden" aria-label="Primary">
    <ul class="mx-auto grid max-w-md grid-cols-5">
      <li v-for="tab in tabs" :key="tab.label">
        <component
          :is="tab.to ? 'RouterLink' : 'button'"
          :to="tab.to"
          class="relative flex w-full flex-col items-center gap-0.5 pb-2 pt-2.5 text-[10px] font-semibold transition"
          :class="isActive(tab) ? 'text-uni-navy' : 'text-slate-400'"
          @click="tab.action === 'search' && palette.show()"
        >
          <span v-if="isActive(tab)" class="absolute top-0 h-0.5 w-8 rounded-full bg-teal-500"></span>
          <span class="relative">
            <component :is="tab.icon" class="h-[22px] w-[22px]" :stroke-width="isActive(tab) ? 2.25 : 1.75" />
            <span
              v-if="tab.badge"
              class="absolute -right-2 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[9px] font-bold text-white ring-2 ring-white"
            >
              {{ tab.badge > 9 ? "9+" : tab.badge }}
            </span>
          </span>
          {{ tab.label }}
        </component>
      </li>
    </ul>
  </nav>
</template>
