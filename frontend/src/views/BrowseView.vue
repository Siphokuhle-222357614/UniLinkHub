<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowRight,
  BadgeCheck,
  Bookmark,
  Flame,
  MapPin,
  MessageCircle,
  Search,
  SearchX,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Store,
  UserPlus,
  X,
} from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useCategories } from "@/lib/categories";
import { campusLabel, useCampuses } from "@/lib/campuses";
import { categoryMeta } from "@/lib/categoryMeta";
import { formatPrice } from "@/lib/format";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";
import type { ListingDTO, PageResponse, ProviderProfileDTO } from "@/lib/types";
import ListingCard from "@/components/ListingCard.vue";
import SkeletonCard from "@/components/ui/SkeletonCard.vue";
import EmptyState from "@/components/ui/EmptyState.vue";

interface PublicStats {
  students: number;
  verifiedBusinesses: number;
  activeListings: number;
  completedOrders: number;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const listings = ref<ListingDTO[]>([]);
const categories = useCategories();
const categoryCounts = ref<Record<string, number>>({});
const trending = ref<ListingDTO[]>([]);
const featuredBusinesses = ref<ProviderProfileDTO[]>([]);
const stats = ref<PublicStats | null>(null);
const keyword = ref("");
const category = ref("");
const minPrice = ref("");
const maxPrice = ref("");
const kind = ref<"ALL" | "PRODUCT" | "SERVICE">("ALL");
const verifiedOnly = ref(false);
const sort = ref("newest");
const campus = ref("");
const campuses = useCampuses();
const KIND_OPTIONS = [
  { v: "ALL", l: "All" },
  { v: "PRODUCT", l: "Products" },
  { v: "SERVICE", l: "Services" },
] as const;
const mobileFiltersOpen = ref(false);
const loading = ref(true);
const error = ref("");
const suggestionsOpen = ref(false);
const savingSearch = ref(false);
const searchSaved = ref(false);

let debounceHandle: ReturnType<typeof setTimeout> | undefined;

async function loadCategoryCounts() {
  try {
    // Counted by the database - previously this downloaded every listing just to count them.
    const { data } = await api.get<Record<string, number>>("/listings/category-counts");
    categoryCounts.value = data;
  } catch {
    // Category tile counts are a nice-to-have; ignore failures here.
  }
}

async function loadTrending() {
  try {
    const { data } = await api.get<ListingDTO[]>("/listings", { params: { sort: "views", limit: 4 } });
    trending.value = data;
  } catch {
    // Trending is a nice-to-have; ignore failures here.
  }
}

async function loadFeaturedBusinesses() {
  try {
    const { data } = await api.get<ProviderProfileDTO[]>("/businesses", { params: { verifiedOnly: true } });
    featuredBusinesses.value = data.slice(0, 3);
  } catch {
    // Featured businesses are a nice-to-have; ignore failures here.
  }
}

async function loadStats() {
  try {
    const { data } = await api.get<PublicStats>("/stats/public");
    stats.value = data;
  } catch {
    // Headline stats are decorative; the hero still works without them.
  }
}

function scrollToResults() {
  nextTick(() => document.getElementById("browse")?.scrollIntoView({ behavior: "smooth", block: "start" }));
}

function browseCategory(c: string) {
  category.value = category.value === c ? "" : c;
  scrollToResults();
}

// ---- Search autocomplete ----
const matchedCategories = computed(() => {
  const needle = keyword.value.trim().toLowerCase();
  if (!needle) return [];
  return categories.value.filter((c) => c.toLowerCase().includes(needle)).slice(0, 3);
});

const matchedListings = computed(() => {
  const needle = keyword.value.trim().toLowerCase();
  if (!needle) return [];
  return listings.value.filter((l) => l.name.toLowerCase().includes(needle)).slice(0, 5);
});

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]!);
}

// Listing names are user-supplied, so every slice is escaped before the <mark> is added -
// this output goes through v-html.
function highlightMatch(text: string): string {
  const needle = keyword.value.trim();
  const index = needle ? text.toLowerCase().indexOf(needle.toLowerCase()) : -1;
  if (index === -1) return escapeHtml(text);
  return (
    escapeHtml(text.slice(0, index)) +
    "<mark class='rounded bg-gold-100 px-0.5 text-inherit'>" +
    escapeHtml(text.slice(index, index + needle.length)) +
    "</mark>" +
    escapeHtml(text.slice(index + needle.length))
  );
}

function goToListing(id: string) {
  suggestionsOpen.value = false;
  router.push(`/listings/${id}`);
}

function chooseSuggestedCategory(c: string) {
  category.value = c;
  keyword.value = "";
  suggestionsOpen.value = false;
}

// ---- Paged results: the first page replaces, "load more" appends ----
const PAGE_SIZE = 24;
const page = ref(0);
const totalItems = ref(0);
const hasNext = ref(false);
const loadingMore = ref(false);
let searchSeq = 0;

function searchParams(p: number) {
  return {
    keyword: keyword.value || undefined,
    category: category.value || undefined,
    minPrice: minPrice.value || undefined,
    maxPrice: maxPrice.value || undefined,
    type: kind.value === "ALL" ? undefined : kind.value,
    verifiedOnly: verifiedOnly.value || undefined,
    campus: campus.value || undefined,
    sort: sort.value,
    page: p,
    size: PAGE_SIZE,
  };
}

async function search() {
  const seq = ++searchSeq;
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<PageResponse<ListingDTO>>("/listings/search", { params: searchParams(0) });
    if (seq !== searchSeq) return; // a newer filter change already superseded this one
    listings.value = data.items;
    page.value = 0;
    totalItems.value = data.totalItems;
    hasNext.value = data.hasNext;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    if (seq === searchSeq) loading.value = false;
  }
}

async function loadMore() {
  if (!hasNext.value || loadingMore.value || loading.value) return;
  const seq = searchSeq;
  loadingMore.value = true;
  try {
    const { data } = await api.get<PageResponse<ListingDTO>>("/listings/search", { params: searchParams(page.value + 1) });
    if (seq !== searchSeq) return;
    listings.value = [...listings.value, ...data.items];
    page.value = data.page;
    hasNext.value = data.hasNext;
  } catch {
    // The button stays, so they can simply try again.
  } finally {
    loadingMore.value = false;
  }
}

// Load the next page automatically as the end of the results scrolls into view.
const sentinel = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | undefined;
watch(sentinel, (el) => {
  observer?.disconnect();
  if (el && "IntersectionObserver" in window) {
    observer = new IntersectionObserver((entries) => entries.some((e) => e.isIntersecting) && loadMore(), { rootMargin: "400px" });
    observer.observe(el);
  }
});
onBeforeUnmount(() => observer?.disconnect());

function clearFilters() {
  keyword.value = "";
  category.value = "";
  minPrice.value = "";
  maxPrice.value = "";
  kind.value = "ALL";
  verifiedOnly.value = false;
  campus.value = "";
  sort.value = "newest";
}

const resultsLabel = computed(() => (totalItems.value === 1 ? "1 listing" : `${totalItems.value.toLocaleString("en-ZA")} listings`));

const searchLabel = computed(() => {
  const parts: string[] = [];
  if (keyword.value) parts.push(`"${keyword.value}"`);
  if (category.value) parts.push(category.value);
  if (maxPrice.value) parts.push(`under R${maxPrice.value}`);
  if (kind.value !== "ALL") parts.push(kind.value === "PRODUCT" ? "products" : "services");
  return parts.length > 0 ? parts.join(" ") : "All listings";
});

const hasActiveFilters = computed(
  () => !!(keyword.value || category.value || minPrice.value || maxPrice.value || kind.value !== "ALL" || verifiedOnly.value),
);

const activeChips = computed(() => {
  const chips: { label: string; clear: () => void }[] = [];
  if (keyword.value) chips.push({ label: `“${keyword.value}”`, clear: () => (keyword.value = "") });
  if (category.value) chips.push({ label: category.value, clear: () => (category.value = "") });
  if (campus.value) chips.push({ label: `${campusLabel(campus.value)} campus`, clear: () => (campus.value = "") });
  if (minPrice.value) chips.push({ label: `From R${minPrice.value}`, clear: () => (minPrice.value = "") });
  if (maxPrice.value) chips.push({ label: `Up to R${maxPrice.value}`, clear: () => (maxPrice.value = "") });
  if (kind.value !== "ALL") chips.push({ label: kind.value === "PRODUCT" ? "Products" : "Services", clear: () => (kind.value = "ALL") });
  if (verifiedOnly.value) chips.push({ label: "Verified sellers", clear: () => (verifiedOnly.value = false) });
  return chips;
});

const heroStats = computed(() =>
  stats.value
    ? [
        { value: stats.value.students, label: "Students on campus" },
        { value: stats.value.verifiedBusinesses, label: "Verified businesses" },
        { value: stats.value.activeListings, label: "Live listings" },
        { value: stats.value.completedOrders, label: "Orders completed" },
      ]
    : [],
);

const STEPS = [
  { icon: UserPlus, title: "Sign up with your student email", body: "Your @mycput.ac.za address keeps the marketplace students-only and trusted." },
  { icon: Search, title: "Browse or list your hustle", body: "Find what you need in seconds - or turn your side hustle into a verified business." },
  { icon: MessageCircle, title: "Connect and meet up on campus", body: "Message sellers, order, book services and pick up at res. No courier fees." },
];

const greeting = computed(() => {
  const hour = new Date().getHours();
  return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
});

async function saveSearch() {
  savingSearch.value = true;
  try {
    await api.post("/saved-searches", {
      label: searchLabel.value,
      keyword: keyword.value || null,
      category: category.value || null,
      maxPrice: maxPrice.value ? Number(maxPrice.value) : null,
      listingType: kind.value === "ALL" ? null : kind.value,
    });
    searchSaved.value = true;
    toast.success("Search saved!", "We'll notify you about new matches.");
    setTimeout(() => (searchSaved.value = false), 4000);
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    savingSearch.value = false;
  }
}

function applyQuery(query: typeof route.query) {
  if (typeof query.keyword === "string") keyword.value = query.keyword;
  if (typeof query.category === "string") category.value = query.category;
  if (typeof query.campus === "string") campus.value = query.campus;
  if (typeof query.maxPrice === "string") maxPrice.value = query.maxPrice;
  if (typeof query.type === "string") kind.value = query.type as typeof kind.value;
}

watch([keyword, category, minPrice, maxPrice, kind, verifiedOnly, campus, sort], () => {
  clearTimeout(debounceHandle);
  debounceHandle = setTimeout(search, 250);
});

// The command palette and footer link here with ?category= / ?keyword= while this page may
// already be mounted, so query changes are applied live rather than only on mount.
watch(
  () => route.query,
  (query) => {
    if (Object.keys(query).length === 0) return;
    applyQuery(query);
    scrollToResults();
  },
);

onMounted(() => {
  applyQuery(route.query);
  if (Object.keys(route.query).length > 0) scrollToResults();

  loadCategoryCounts();
  loadTrending();
  loadFeaturedBusinesses();
  loadStats();
  search();
});
</script>

<template>
  <section class="space-y-14 sm:space-y-16">
    <!-- ============ Hero ============ -->
    <div class="relative -mx-4 -mt-6 overflow-hidden bg-hero px-5 pb-10 pt-12 sm:mt-0 text-white sm:mx-0 sm:rounded-[28px] sm:px-10 sm:pt-16 lg:px-14">
      <div class="bg-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_right,black,transparent_70%)]"></div>

      <div class="relative grid grid-cols-1 items-center gap-10 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
        <div class="animate-fade-up">
          <template v-if="!auth.isAuthenticated">
            <span class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-semibold text-sky-blue backdrop-blur">
              <Sparkles class="h-3.5 w-3.5" /> Built for CPUT residence students
            </span>
            <h1 class="mt-5 max-w-xl font-display text-4xl font-bold leading-[1.08] tracking-tight text-white sm:text-5xl lg:text-[3.4rem]">
              Everything your campus sells, <span class="text-gradient">in one place.</span>
            </h1>
            <p class="mt-5 max-w-lg text-base leading-relaxed text-white/75">
              Print jobs, tutoring, home-cooked food, hair &amp; beauty and more — from students you actually share a res with.
              No more scrolling through ten WhatsApp groups.
            </p>
          </template>
          <template v-else>
            <p class="text-sm font-medium text-sky-blue">{{ greeting }}, {{ auth.user?.firstName }}</p>
            <h1 class="mt-2 max-w-xl font-display text-3xl font-bold leading-tight tracking-tight text-white sm:text-4xl">
              What are you looking for <span class="text-gradient">today?</span>
            </h1>
          </template>

          <form class="mt-8 flex max-w-xl items-center gap-2 rounded-2xl bg-white p-2 shadow-pop" role="search" @submit.prevent="scrollToResults">
            <Search class="ml-2 h-5 w-5 shrink-0 text-slate-400" />
            <input
              v-model="keyword"
              type="search"
              aria-label="Search listings"
              placeholder="Try “poster printing” or “braids”"
              class="h-11 w-full min-w-0 flex-1 bg-transparent text-sm text-charcoal placeholder:text-slate-400 focus:outline-none sm:text-base"
            />
            <button type="submit" class="btn-accent h-11 shrink-0 rounded-xl px-4 sm:px-5">Search</button>
          </form>

          <div class="mt-4 flex flex-wrap items-center gap-2">
            <span class="text-xs text-white/50">Popular:</span>
            <button
              v-for="c in categories.slice(0, 4)"
              :key="c"
              class="rounded-full border border-white/15 bg-white/5 px-3 py-1 text-xs font-medium text-white/85 transition hover:border-white/30 hover:bg-white/15"
              @click="browseCategory(c)"
            >
              {{ c }}
            </button>
          </div>

          <div v-if="!auth.isAuthenticated" class="mt-8 flex flex-col gap-3 sm:flex-row">
            <RouterLink to="/register" class="btn bg-white px-6 py-3 text-uni-navy hover:bg-navy-50">
              Create a free account <ArrowRight class="h-4 w-4" />
            </RouterLink>
            <RouterLink to="/register" class="btn border border-white/25 bg-white/5 px-6 py-3 text-white hover:bg-white/15">
              <Store class="h-4 w-4" /> Start selling
            </RouterLink>
          </div>
        </div>

        <!-- Floating preview of real trending listings -->
        <div v-if="trending.length >= 2" class="relative hidden h-[22rem] lg:block" aria-hidden="true">
          <div class="absolute right-24 top-2 w-60 rotate-[-4deg] animate-float rounded-2xl bg-white p-2.5 text-charcoal shadow-pop">
            <div class="aspect-[4/3] overflow-hidden rounded-xl" :style="{ background: categoryMeta(trending[0].category).gradient }">
              <img v-if="trending[0].imageUrl" :src="trending[0].imageUrl" alt="" class="h-full w-full object-cover" @error="($event.target as HTMLImageElement).remove()" />
            </div>
            <p class="mt-2.5 truncate px-1 text-sm font-semibold text-uni-navy">{{ trending[0].name }}</p>
            <p class="px-1 pb-1 text-sm font-bold text-teal-600">{{ formatPrice(trending[0].price) }}</p>
          </div>
          <div class="absolute bottom-0 right-0 w-56 rotate-[5deg] animate-float rounded-2xl bg-white p-2.5 text-charcoal shadow-pop [animation-delay:1.2s]">
            <div class="aspect-[4/3] overflow-hidden rounded-xl" :style="{ background: categoryMeta(trending[1].category).gradient }">
              <img v-if="trending[1].imageUrl" :src="trending[1].imageUrl" alt="" class="h-full w-full object-cover" @error="($event.target as HTMLImageElement).remove()" />
            </div>
            <p class="mt-2.5 truncate px-1 text-sm font-semibold text-uni-navy">{{ trending[1].name }}</p>
            <p class="px-1 pb-1 text-sm font-bold text-teal-600">{{ formatPrice(trending[1].price) }}</p>
          </div>
          <div class="absolute bottom-16 left-0 flex items-center gap-3 rounded-2xl bg-white/95 px-4 py-3 text-charcoal shadow-pop backdrop-blur">
            <span class="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-50 text-emerald-600"><BadgeCheck class="h-5 w-5" /></span>
            <div>
              <p class="text-xs font-semibold text-uni-navy">Verified student sellers</p>
              <p class="text-[11px] text-medium-grey">Every business is reviewed by an admin</p>
            </div>
          </div>
        </div>
      </div>

      <dl v-if="heroStats.length" class="relative mt-12 grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-white/10 bg-white/10 sm:grid-cols-4">
        <div v-for="s in heroStats" :key="s.label" class="bg-navy-950/40 px-5 py-4 backdrop-blur">
          <dt class="text-xs text-white/60">{{ s.label }}</dt>
          <dd class="mt-1 font-display text-2xl font-bold text-white">{{ s.value.toLocaleString("en-ZA") }}</dd>
        </div>
      </dl>
    </div>

    <!-- ============ Categories ============ -->
    <div v-if="categories.length > 0">
      <div class="mb-5 flex items-end justify-between">
        <div>
          <p class="eyebrow">Categories</p>
          <h2 class="mt-1 font-display text-2xl font-bold text-uni-navy">Shop by category</h2>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-2.5 sm:grid-cols-4 sm:gap-3 xl:grid-cols-8">
        <button
          v-for="c in categories"
          :key="c"
          class="group flex items-center gap-3 rounded-card border bg-white p-3 text-left sm:flex-col sm:items-start sm:p-4 shadow-xs transition hover:-translate-y-0.5 hover:shadow-lift"
          :class="category === c ? 'border-teal-500 ring-4 ring-teal-500/10' : 'border-light-grey/80'"
          @click="browseCategory(c)"
        >
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition sm:h-11 sm:w-11 group-hover:scale-110" :class="categoryMeta(c).tile">
            <component :is="categoryMeta(c).icon" class="h-5 w-5" />
          </span>
          <span>
            <span class="block text-[13px] font-semibold leading-tight text-uni-navy sm:text-sm">{{ c }}</span>
            <span class="text-xs text-medium-grey">{{ categoryCounts[c] ?? 0 }} listing{{ (categoryCounts[c] ?? 0) === 1 ? "" : "s" }}</span>
          </span>
        </button>
      </div>
    </div>

    <!-- ============ Trending ============ -->
    <div v-if="trending.length > 0">
      <div class="mb-5">
        <p class="eyebrow flex items-center gap-1.5"><Flame class="h-3.5 w-3.5" /> Trending</p>
        <h2 class="mt-1 font-display text-2xl font-bold text-uni-navy">Most viewed this week</h2>
      </div>
      <div class="-mx-4 flex snap-x snap-mandatory scroll-px-4 gap-3 overflow-x-auto px-4 pb-2 sm:mx-0 sm:grid sm:grid-cols-2 sm:gap-5 sm:overflow-visible sm:px-0 sm:pb-0 xl:grid-cols-4">
        <ListingCard v-for="listing in trending" :key="listing.id" :listing="listing" class="w-[44vw] shrink-0 snap-start sm:w-auto" />
      </div>
    </div>

    <!-- ============ Guest: how it works + featured sellers ============ -->
    <template v-if="!auth.isAuthenticated">
      <div class="grid grid-cols-1 gap-5 md:grid-cols-3">
        <div v-for="(step, i) in STEPS" :key="step.title" class="card relative overflow-hidden">
          <span class="absolute right-4 top-3 font-display text-5xl font-bold text-navy-50">{{ i + 1 }}</span>
          <span class="relative flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-uni-navy to-teal-500 text-white shadow-lift">
            <component :is="step.icon" class="h-5 w-5" />
          </span>
          <h3 class="relative mt-4 font-display text-base font-semibold text-uni-navy">{{ step.title }}</h3>
          <p class="relative mt-1.5 text-sm leading-relaxed text-medium-grey">{{ step.body }}</p>
        </div>
      </div>

      <div v-if="featuredBusinesses.length > 0">
        <div class="mb-5 flex items-end justify-between gap-4">
          <div>
            <p class="eyebrow">Featured</p>
            <h2 class="mt-1 font-display text-2xl font-bold text-uni-navy">Verified student businesses</h2>
          </div>
          <RouterLink to="/providers" class="link hidden items-center gap-1 text-sm sm:inline-flex">All providers <ArrowRight class="h-4 w-4" /></RouterLink>
        </div>
        <div class="grid grid-cols-1 gap-5 sm:grid-cols-3">
          <RouterLink v-for="b in featuredBusinesses" :key="b.businessId" :to="`/providers/${b.businessId}`" class="group card card-interactive flex items-center gap-4">
            <img v-if="b.imageUrl" :src="b.imageUrl" alt="" class="h-14 w-14 shrink-0 rounded-2xl object-cover ring-1 ring-light-grey" />
            <span v-else class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl font-display text-lg font-bold" :class="categoryMeta(b.category).tile">
              {{ b.businessName.charAt(0) }}
            </span>
            <div class="min-w-0 flex-1">
              <h3 class="flex items-center gap-1.5 truncate font-display text-sm font-semibold text-uni-navy">
                {{ b.businessName }} <BadgeCheck class="h-4 w-4 shrink-0 text-emerald-500" />
              </h3>
              <p class="text-xs text-medium-grey">{{ b.category }} · {{ b.activeListingCount }} active listings</p>
            </div>
            <ArrowRight class="h-4 w-4 shrink-0 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-teal-600" />
          </RouterLink>
        </div>
      </div>
    </template>

    <!-- ============ Marketplace ============ -->
    <div id="browse" class="scroll-mt-24">
      <div class="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="eyebrow">Marketplace</p>
          <h2 class="mt-1 font-display text-2xl font-bold text-uni-navy">{{ category || "All listings" }}</h2>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-secondary lg:hidden" :aria-expanded="mobileFiltersOpen" @click="mobileFiltersOpen = !mobileFiltersOpen">
            <SlidersHorizontal class="h-4 w-4" /> Filters
            <span v-if="activeChips.length" class="rounded-full bg-uni-navy px-1.5 text-[10px] text-white">{{ activeChips.length }}</span>
          </button>
          <select v-model="sort" class="input-field w-auto min-w-[11rem]" aria-label="Sort listings">
            <option value="newest">Newest first</option>
            <option value="price_asc">Price: low to high</option>
            <option value="price_desc">Price: high to low</option>
            <option value="views">Most viewed</option>
          </select>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside class="h-fit space-y-6 lg:sticky lg:top-24" :class="mobileFiltersOpen ? 'card block animate-fade-up' : 'hidden lg:block'">
          <div class="relative">
            <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              v-model="keyword"
              type="search"
              placeholder="Search listings…"
              aria-label="Search listings"
              class="input-field pl-9"
              @focus="suggestionsOpen = true"
              @blur="suggestionsOpen = false"
            />
            <div
              v-if="suggestionsOpen && keyword.trim() && (matchedCategories.length > 0 || matchedListings.length > 0)"
              class="absolute left-0 right-0 top-[calc(100%+6px)] z-20 animate-scale-in rounded-card border border-light-grey bg-white p-1.5 shadow-pop"
            >
              <template v-if="matchedCategories.length > 0">
                <p class="px-2.5 pb-1 pt-1.5 text-[11px] font-semibold uppercase tracking-wider text-medium-grey">Categories</p>
                <div
                  v-for="c in matchedCategories"
                  :key="c"
                  class="flex cursor-pointer items-center gap-2 rounded-control px-2.5 py-1.5 text-sm font-medium text-uni-navy hover:bg-soft-grey"
                  @mousedown.prevent="chooseSuggestedCategory(c)"
                >
                  <component :is="categoryMeta(c).icon" class="h-4 w-4 text-medium-grey" />
                  {{ c }} <span class="ml-auto text-xs text-medium-grey">{{ categoryCounts[c] ?? 0 }}</span>
                </div>
              </template>
              <template v-if="matchedListings.length > 0">
                <p class="px-2.5 pb-1 pt-2 text-[11px] font-semibold uppercase tracking-wider text-medium-grey">Listings</p>
                <div
                  v-for="l in matchedListings"
                  :key="l.id"
                  class="flex cursor-pointer items-center gap-2.5 rounded-control px-2.5 py-1.5 hover:bg-soft-grey"
                  @mousedown.prevent="goToListing(l.id)"
                >
                  <img v-if="l.imageUrl" :src="l.imageUrl" alt="" class="h-8 w-8 shrink-0 rounded-lg object-cover" />
                  <span v-else class="h-8 w-8 shrink-0 rounded-lg" :style="{ background: categoryMeta(l.category).gradient }"></span>
                  <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-medium text-charcoal" v-html="highlightMatch(l.name)"></p>
                    <p class="text-xs text-medium-grey">{{ formatPrice(l.price) }}</p>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div>
            <p class="filter-heading">Campus</p>
            <div class="flex flex-wrap gap-1.5">
              <button
                v-if="auth.user?.campus"
                class="chip"
                :class="{ 'chip-active': campus === auth.user.campus }"
                :aria-pressed="campus === auth.user.campus"
                @click="campus = campus === auth.user.campus ? '' : auth.user.campus"
              >
                <MapPin class="h-3.5 w-3.5" /> Near me
              </button>
              <button class="chip" :class="{ 'chip-active': campus === '' }" @click="campus = ''">All campuses</button>
              <button v-for="c in campuses" :key="c.key" class="chip" :class="{ 'chip-active': campus === c.key }" @click="campus = c.key">
                {{ c.label }}
              </button>
            </div>
            <p v-if="!auth.user?.campus && auth.isAuthenticated" class="mt-2 text-[11px] text-medium-grey">
              <RouterLink to="/account" class="link">Set your campus</RouterLink> to see what's near you in one tap.
            </p>
          </div>

          <div>
            <p class="filter-heading">Category</p>
            <div class="flex flex-wrap gap-1.5">
              <button class="chip" :class="{ 'chip-active': category === '' }" @click="category = ''">All</button>
              <button v-for="c in categories" :key="c" class="chip" :class="{ 'chip-active': category === c }" @click="category = c">{{ c }}</button>
            </div>
          </div>

          <div>
            <p class="filter-heading">Type</p>
            <div class="grid grid-cols-3 gap-1 rounded-control bg-soft-grey p-1">
              <button
                v-for="opt in KIND_OPTIONS"
                :key="opt.v"
                class="rounded-lg py-1.5 text-xs font-semibold transition"
                :class="kind === opt.v ? 'bg-white text-uni-navy shadow-card' : 'text-medium-grey hover:text-uni-navy'"
                @click="kind = opt.v"
              >
                {{ opt.l }}
              </button>
            </div>
          </div>

          <div>
            <p class="filter-heading">Price range</p>
            <div class="flex items-center gap-2">
              <div class="relative flex-1">
                <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-medium-grey">R</span>
                <input v-model="minPrice" type="number" min="0" placeholder="Min" aria-label="Minimum price" class="input-field pl-7" />
              </div>
              <span class="text-light-grey">—</span>
              <div class="relative flex-1">
                <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-medium-grey">R</span>
                <input v-model="maxPrice" type="number" min="0" placeholder="Max" aria-label="Maximum price" class="input-field pl-7" />
              </div>
            </div>
          </div>

          <label class="flex cursor-pointer items-center justify-between gap-3 rounded-control border border-light-grey bg-white px-3.5 py-3">
            <span class="flex items-center gap-2 text-sm font-medium text-charcoal">
              <ShieldCheck class="h-4 w-4 text-emerald-500" /> Verified sellers only
            </span>
            <span class="relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition" :class="verifiedOnly ? 'bg-teal-500' : 'bg-slate-200'">
              <input v-model="verifiedOnly" type="checkbox" class="sr-only" />
              <span class="h-4 w-4 rounded-full bg-white shadow transition" :class="verifiedOnly ? 'translate-x-[18px]' : 'translate-x-0.5'"></span>
            </span>
          </label>

          <button v-if="hasActiveFilters" class="btn-ghost w-full" @click="clearFilters">Reset all filters</button>
        </aside>

        <div class="min-w-0 space-y-4">
          <div class="flex flex-wrap items-center gap-2">
            <p class="mr-1 text-sm text-medium-grey">
              <span v-if="!loading" class="font-semibold text-charcoal">{{ resultsLabel }}</span>
              <span v-else>Searching…</span>
            </p>
            <button v-for="chip in activeChips" :key="chip.label" class="chip py-1" @click="chip.clear()">
              {{ chip.label }} <X class="h-3 w-3" />
            </button>
            <button
              v-if="auth.isAuthenticated && !auth.isAdmin && hasActiveFilters"
              class="ml-auto inline-flex items-center gap-1.5 text-xs font-semibold text-teal-600 hover:text-teal-700 disabled:opacity-50"
              :disabled="savingSearch"
              @click="saveSearch"
            >
              <Bookmark class="h-3.5 w-3.5" /> {{ savingSearch ? "Saving…" : "Save this search" }}
            </button>
          </div>

          <div v-if="searchSaved" class="flex flex-wrap items-center justify-between gap-2 rounded-control border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            <span>Saved! We'll notify you when new listings match {{ searchLabel }}.</span>
            <RouterLink to="/saved-searches" class="font-semibold underline">Manage</RouterLink>
          </div>

          <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>

          <div v-else-if="loading" class="grid grid-cols-2 gap-3 sm:gap-5 xl:grid-cols-3 3xl:grid-cols-4">
            <SkeletonCard v-for="n in 6" :key="n" />
          </div>

          <EmptyState
            v-else-if="listings.length === 0"
            :icon="SearchX"
            title="No listings match those filters"
            description="Try a different keyword, widen the price range, or browse another category."
          >
            <button class="btn-secondary" @click="clearFilters">Clear filters</button>
          </EmptyState>

          <div v-else class="grid grid-cols-2 gap-3 sm:gap-5 xl:grid-cols-3 3xl:grid-cols-4">
            <ListingCard v-for="listing in listings" :key="listing.id" :listing="listing" />
          </div>
          <div v-if="!loading && listings.length > 0" ref="sentinel" class="flex flex-col items-center gap-2 pt-2">
            <button v-if="hasNext" class="btn-secondary px-6" :disabled="loadingMore" @click="loadMore">
              {{ loadingMore ? "Loading…" : "Load more" }}
            </button>
            <p class="text-xs text-medium-grey">Showing {{ listings.length.toLocaleString("en-ZA") }} of {{ resultsLabel }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ Seller CTA ============ -->
    <div
      v-if="!auth.isSeller && !auth.isAdmin"
      class="relative overflow-hidden rounded-[28px] bg-gradient-to-br from-gold-50 via-white to-teal-50 px-6 py-10 ring-1 ring-light-grey sm:px-12"
    >
      <div class="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-teal-200/30 blur-3xl"></div>
      <div class="relative flex flex-col items-start gap-6 md:flex-row md:items-center md:justify-between">
        <div class="max-w-xl">
          <p class="eyebrow text-gold-700">For student entrepreneurs</p>
          <h2 class="mt-2 font-display text-2xl font-bold text-uni-navy sm:text-3xl">Turn your side hustle into a verified business.</h2>
          <p class="mt-2 text-sm leading-relaxed text-medium-grey">
            List products and services for free, take orders and bookings, run promo codes, and track your sales — all from one dashboard.
          </p>
        </div>
        <RouterLink :to="auth.isAuthenticated ? '/dashboard' : '/register'" class="btn-primary px-6 py-3">
          <Store class="h-4 w-4" /> {{ auth.isAuthenticated ? "Become a seller" : "Start selling for free" }}
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.filter-heading {
  @apply mb-2.5 text-xs font-semibold uppercase tracking-wider text-medium-grey;
}
</style>
