<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { ArrowLeft, LogOut, ShieldCheck } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import LogoMark from "@/components/LogoMark.vue";
import ThemeToggleButton from "@/components/ThemeToggleButton.vue";

const router = useRouter();
const auth = useAuthStore();

const initials = computed(() => {
  const first = auth.user?.firstName?.[0] ?? "";
  const last = auth.user?.lastName?.[0] ?? "";
  return (first + last).toUpperCase() || "A";
});

function handleLogout() {
  auth.logout();
  router.push({ name: "login" });
}
</script>

<template>
  <header class="sticky top-0 z-30 border-b border-light-grey/70 bg-white/80 backdrop-blur-xl">
    <div class="mx-auto flex h-16 max-w-7xl 2xl:max-w-[1400px] 3xl:max-w-[1600px] items-center justify-between gap-3 px-4 sm:px-6">
      <div class="flex min-w-0 items-center gap-3">
        <RouterLink to="/" class="flex shrink-0 items-center gap-2" aria-label="UniLinkHub home">
          <LogoMark :size="30" />
          <span class="hidden font-display text-lg font-bold text-uni-navy sm:inline">Uni<span class="text-teal-500">Link</span>Hub</span>
        </RouterLink>
        <!-- Small phones: just the shield (the bar also holds the theme toggle, back link, avatar and log out). -->
        <span class="badge min-w-0 border border-gold-200 bg-gold-50 py-1 text-gold-700" title="Admin console">
          <ShieldCheck class="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span class="sr-only xs:not-sr-only xs:truncate">Admin console</span>
        </span>
      </div>

      <nav class="flex shrink-0 items-center gap-1 sm:gap-2">
        <ThemeToggleButton />
        <RouterLink to="/" class="btn-ghost px-3 text-xs">
          <ArrowLeft class="h-4 w-4" /> <span class="hidden sm:inline">Marketplace</span>
        </RouterLink>
        <div class="flex items-center gap-2 pl-1">
          <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-uni-navy to-teal-500 font-display text-xs font-bold text-white">
            {{ initials }}
          </span>
          <span class="hidden text-sm font-medium text-charcoal lg:inline">{{ auth.user?.firstName }} {{ auth.user?.lastName }}</span>
        </div>
        <button class="btn-icon h-9 w-9 hover:text-danger" aria-label="Log out" title="Log out" @click="handleLogout">
          <LogOut class="h-4 w-4" />
        </button>
      </nav>
    </div>
  </header>
</template>
