<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type Component } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  BadgeCheck,
  Bookmark,
  CornerDownLeft,
  History,
  LayoutDashboard,
  MessageCircle,
  Package,
  Receipt,
  Search,
  Settings,
  ShieldCheck,
  Store,
  Users,
} from "@lucide/vue";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useCommandPaletteStore } from "@/stores/commandPalette";
import { useCategories } from "@/lib/categories";
import { categoryMeta } from "@/lib/categoryMeta";
import { getRecentlyViewed } from "@/lib/recentlyViewed";
import { formatPrice } from "@/lib/format";
import type { ListingDTO, ProviderProfileDTO } from "@/lib/types";

interface Item {
  id: string;
  group: string;
  label: string;
  hint?: string;
  icon?: Component;
  iconClass?: string;
  imageUrl?: string | null;
  to: string | { path: string; query?: Record<string, string> };
}

const router = useRouter();
const auth = useAuthStore();
const palette = useCommandPaletteStore();
const categories = useCategories();

const query = ref("");
const input = ref<HTMLInputElement | null>(null);
const listRef = ref<HTMLElement | null>(null);
const active = ref(0);
const loading = ref(false);
const listings = ref<ListingDTO[]>([]);
const providers = ref<ProviderProfileDTO[]>([]);
let debounce: ReturnType<typeof setTimeout> | undefined;
let requestSeq = 0;

const pages = computed<Item[]>(() => {
  const all: Item[] = [
    { id: "p-explore", group: "Pages", label: "Explore the marketplace", icon: Search, to: "/" },
    { id: "p-providers", group: "Pages", label: "Provider directory", icon: Users, to: "/providers" },
    { id: "p-recent", group: "Pages", label: "Recently viewed", icon: History, to: "/recently-viewed" },
  ];
  if (auth.isAuthenticated) {
    all.push(
      { id: "p-dashboard", group: "Pages", label: "Dashboard", icon: LayoutDashboard, to: "/dashboard" },
      { id: "p-orders", group: "Pages", label: "My orders", icon: Receipt, to: "/orders" },
      { id: "p-messages", group: "Pages", label: "Messages", icon: MessageCircle, to: "/messages" },
      { id: "p-saved", group: "Pages", label: "Saved searches", icon: Bookmark, to: "/saved-searches" },
      { id: "p-account", group: "Pages", label: "Account settings", icon: Settings, to: "/account" },
    );
    if (auth.isSeller) {
      all.push(
        { id: "p-listings", group: "Pages", label: "My listings", icon: Store, to: "/my-listings" },
        { id: "p-selling", group: "Pages", label: "Orders to fulfil", icon: Package, to: "/orders/selling" },
      );
    }
    if (auth.isAdmin) all.push({ id: "p-admin", group: "Pages", label: "Admin console", icon: ShieldCheck, to: "/admin" });
  }
  return all;
});

const needle = computed(() => query.value.trim().toLowerCase());

const items = computed<Item[]>(() => {
  const q = needle.value;
  const result: Item[] = [];

  if (!q) {
    for (const l of getRecentlyViewed().slice(0, 4)) {
      result.push({ id: `r-${l.id}`, group: "Recently viewed", label: l.name, hint: formatPrice(l.price), imageUrl: l.imageUrl, ...iconFor(l.category), to: `/listings/${l.id}` });
    }
    for (const c of categories.value) {
      result.push({ id: `c-${c}`, group: "Browse categories", label: c, ...iconFor(c), to: { path: "/", query: { category: c } } });
    }
    return [...result, ...pages.value.slice(0, 6)];
  }

  for (const l of listings.value) {
    result.push({ id: `l-${l.id}`, group: "Listings", label: l.name, hint: formatPrice(l.price), imageUrl: l.imageUrl, ...iconFor(l.category), to: `/listings/${l.id}` });
  }
  for (const p of providers.value) {
    result.push({
      id: `b-${p.businessId}`,
      group: "Sellers",
      label: p.businessName,
      hint: `${p.category} · ${p.activeListingCount} listing${p.activeListingCount === 1 ? "" : "s"}`,
      icon: p.verificationStatus === "VERIFIED" ? BadgeCheck : Store,
      iconClass: p.verificationStatus === "VERIFIED" ? "bg-emerald-50 text-emerald-600" : "bg-slate-100 text-slate-600",
      imageUrl: p.imageUrl,
      to: `/providers/${p.businessId}`,
    });
  }
  for (const c of categories.value.filter((c) => c.toLowerCase().includes(q))) {
    result.push({ id: `c-${c}`, group: "Categories", label: c, ...iconFor(c), to: { path: "/", query: { category: c } } });
  }
  for (const p of pages.value.filter((p) => p.label.toLowerCase().includes(q))) result.push(p);
  result.push({ id: "search-all", group: "Search", label: `Search all listings for “${query.value.trim()}”`, icon: Search, to: { path: "/", query: { keyword: query.value.trim() } } });
  return result;
});

const grouped = computed(() => {
  const groups: { name: string; items: { item: Item; index: number }[] }[] = [];
  items.value.forEach((item, index) => {
    let g = groups.find((x) => x.name === item.group);
    if (!g) groups.push((g = { name: item.group, items: [] }));
    g.items.push({ item, index });
  });
  return groups;
});

function iconFor(category: string) {
  const meta = categoryMeta(category);
  return { icon: meta.icon, iconClass: meta.tile };
}

async function runSearch(q: string) {
  const seq = ++requestSeq;
  loading.value = true;
  try {
    const [l, p] = await Promise.all([
      api.get<ListingDTO[]>("/listings", { params: { keyword: q, sort: "views", limit: 8 } }),
      api.get<ProviderProfileDTO[]>("/businesses", { params: { keyword: q } }),
    ]);
    if (seq !== requestSeq) return; // a newer keystroke already superseded this request
    listings.value = l.data.filter((x) => x.status !== "INACTIVE").slice(0, 6);
    providers.value = p.data.slice(0, 4);
  } catch {
    if (seq === requestSeq) {
      listings.value = [];
      providers.value = [];
    }
  } finally {
    if (seq === requestSeq) loading.value = false;
  }
}

watch(query, (q) => {
  active.value = 0;
  clearTimeout(debounce);
  if (!q.trim()) {
    listings.value = [];
    providers.value = [];
    loading.value = false;
    return;
  }
  debounce = setTimeout(() => runSearch(q.trim()), 180);
});

watch(
  () => palette.open,
  async (open) => {
    if (open) {
      query.value = "";
      active.value = 0;
      await nextTick();
      input.value?.focus();
    }
  },
);

function select(item: Item | undefined) {
  if (!item) return;
  palette.hide();
  router.push(item.to);
}

function move(delta: number) {
  const count = items.value.length;
  if (count === 0) return;
  active.value = (active.value + delta + count) % count;
  nextTick(() => listRef.value?.querySelector(`[data-index="${active.value}"]`)?.scrollIntoView({ block: "nearest" }));
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "ArrowDown") {
    e.preventDefault();
    move(1);
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    move(-1);
  } else if (e.key === "Enter") {
    e.preventDefault();
    select(items.value[active.value]);
  } else if (e.key === "Escape") {
    palette.hide();
  }
}

function onGlobalKeydown(e: KeyboardEvent) {
  const target = e.target as HTMLElement | null;
  const typing = !!target && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName));
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    palette.toggle();
  } else if (e.key === "/" && !typing && !palette.open) {
    e.preventDefault();
    palette.show();
  }
}

onMounted(() => window.addEventListener("keydown", onGlobalKeydown));
onBeforeUnmount(() => window.removeEventListener("keydown", onGlobalKeydown));
</script>

<template>
  <Teleport to="body">
    <div
      v-if="palette.open"
      class="fixed inset-0 z-[60] flex items-start justify-center bg-navy-950/50 px-3 pt-[10vh] backdrop-blur-sm animate-fade-in sm:px-4"
      @click.self="palette.hide()"
    >
      <div v-dialog="() => palette.hide()" class="w-full max-w-xl animate-scale-in overflow-hidden rounded-modal bg-white shadow-pop" role="dialog" aria-modal="true" aria-label="Search">
        <div class="flex items-center gap-3 border-b border-light-grey px-4">
          <Search class="h-5 w-5 shrink-0 text-teal-600" />
          <input
            ref="input"
            v-model="query"
            type="text"
            class="h-14 w-full bg-transparent text-base text-charcoal placeholder:text-slate-400 focus:outline-none"
            placeholder="Search listings, sellers, categories…"
            aria-label="Search"
            autocomplete="off"
            spellcheck="false"
            @keydown="onKeydown"
          />
          <span v-if="loading" class="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-teal-500 border-t-transparent"></span>
          <kbd class="hidden shrink-0 rounded-md border border-light-grey px-1.5 py-0.5 text-[11px] font-semibold text-medium-grey sm:inline">Esc</kbd>
        </div>

        <div ref="listRef" class="max-h-[60vh] overflow-y-auto p-2">
          <p v-if="needle && !loading && items.length <= 1" class="px-3 py-6 text-center text-sm text-medium-grey">
            No quick matches — press Enter to search all listings.
          </p>
          <div v-for="g in grouped" :key="g.name" class="mb-1">
            <p class="px-3 pb-1 pt-2.5 text-[11px] font-semibold uppercase tracking-wider text-medium-grey">{{ g.name }}</p>
            <button
              v-for="{ item, index } in g.items"
              :key="item.id"
              :data-index="index"
              class="flex w-full items-center gap-3 rounded-control px-3 py-2 text-left transition"
              :class="index === active ? 'bg-navy-50' : 'hover:bg-soft-grey'"
              @mousemove="active = index"
              @click="select(item)"
            >
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="h-9 w-9 shrink-0 rounded-lg object-cover" />
              <span v-else class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg" :class="item.iconClass ?? 'bg-navy-50 text-navy-600'">
                <component :is="item.icon" class="h-4 w-4" />
              </span>
              <span class="min-w-0 flex-1">
                <span class="block truncate text-sm font-medium text-charcoal">{{ item.label }}</span>
                <span v-if="item.hint" class="block truncate text-xs text-medium-grey">{{ item.hint }}</span>
              </span>
              <CornerDownLeft v-if="index === active" class="h-4 w-4 shrink-0 text-medium-grey" />
              <ArrowRight v-else class="h-4 w-4 shrink-0 text-transparent" />
            </button>
          </div>
        </div>

        <div class="hidden items-center gap-4 border-t border-light-grey bg-soft-grey/60 px-4 py-2.5 text-[11px] text-medium-grey sm:flex">
          <span class="flex items-center gap-1"><kbd class="kbd">↑</kbd><kbd class="kbd">↓</kbd> navigate</span>
          <span class="flex items-center gap-1"><kbd class="kbd">↵</kbd> open</span>
          <span class="flex items-center gap-1"><kbd class="kbd">/</kbd> open search anywhere</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.kbd {
  @apply rounded border border-light-grey bg-white px-1 font-sans font-semibold;
}
</style>
