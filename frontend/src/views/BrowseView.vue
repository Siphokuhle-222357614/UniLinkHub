<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useCategories } from "@/lib/categories";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";
import type { ListingDTO, ProviderProfileDTO } from "@/lib/types";
import ListingCard from "@/components/ListingCard.vue";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const toast = useToastStore();

const listings = ref<ListingDTO[]>([]);
const categories = useCategories();
const categoryCounts = ref<Record<string, number>>({});
const trending = ref<ListingDTO[]>([]);
const featuredBusinesses = ref<ProviderProfileDTO[]>([]);
const keyword = ref("");
const category = ref("");
const minPrice = ref("");
const maxPrice = ref("");
const kind = ref<"ALL" | "PRODUCT" | "SERVICE">("ALL");
const verifiedOnly = ref(false);
const sort = ref("newest");
const mobileFiltersOpen = ref(false);
const loading = ref(false);
const error = ref("");
const suggestionsOpen = ref(false);
const savingSearch = ref(false);
const searchSaved = ref(false);

let debounceHandle: ReturnType<typeof setTimeout> | undefined;

async function loadCategoryCounts() {
  try {
    const { data } = await api.get<ListingDTO[]>("/listings");
    const counts: Record<string, number> = {};
    for (const l of data) counts[l.category] = (counts[l.category] ?? 0) + 1;
    categoryCounts.value = counts;
  } catch {
    // Category tile counts are a nice-to-have; ignore failures here.
  }
}

async function loadTrending() {
  try {
    const { data } = await api.get<ListingDTO[]>("/listings", { params: { sort: "views" } });
    trending.value = data.slice(0, 4);
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

function browseCategory(c: string) {
  category.value = c;
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

function highlightMatch(text: string): string {
  const needle = keyword.value.trim();
  if (!needle) return text;
  const index = text.toLowerCase().indexOf(needle.toLowerCase());
  if (index === -1) return text;
  return (
    text.slice(0, index) +
    "<mark class='bg-academic-gold/30'>" +
    text.slice(index, index + needle.length) +
    "</mark>" +
    text.slice(index + needle.length)
  );
}

function goToListing(id: string) {
  suggestionsOpen.value = false;
  router.push(`/listings/${id}`);
}

function chooseSuggestedCategory(c: string) {
  category.value = c;
  suggestionsOpen.value = false;
}

async function search() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<ListingDTO[]>("/listings", {
      params: {
        keyword: keyword.value || undefined,
        category: category.value || undefined,
        minPrice: minPrice.value || undefined,
        maxPrice: maxPrice.value || undefined,
        type: kind.value === "ALL" ? undefined : kind.value,
        verifiedOnly: verifiedOnly.value || undefined,
        sort: sort.value,
      },
    });
    listings.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

function clearFilters() {
  category.value = "";
  minPrice.value = "";
  maxPrice.value = "";
  kind.value = "ALL";
  verifiedOnly.value = false;
  sort.value = "newest";
}

const resultsLabel = computed(() =>
  listings.value.length === 1 ? "1 listing found" : `${listings.value.length} listings found`,
);

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

watch([keyword, category, minPrice, maxPrice, kind, verifiedOnly, sort], () => {
  clearTimeout(debounceHandle);
  debounceHandle = setTimeout(search, 250);
});

onMounted(() => {
  if (typeof route.query.keyword === "string") keyword.value = route.query.keyword;
  if (typeof route.query.category === "string") category.value = route.query.category;
  if (typeof route.query.maxPrice === "string") maxPrice.value = route.query.maxPrice;
  if (typeof route.query.type === "string") kind.value = route.query.type as typeof kind.value;

  loadCategoryCounts();
  loadTrending();
  loadFeaturedBusinesses();
  search();
});
</script>

<template>
  <section class="space-y-6">
    <!-- Landing hero for logged-out visitors -->
    <template v-if="!auth.isAuthenticated">
      <div class="rounded-card bg-uni-navy px-6 py-12 text-center text-white sm:py-16">
        <span class="badge bg-white/15 text-white">For CPUT res students</span>
        <h1 class="mx-auto mt-4 max-w-2xl font-display text-3xl font-bold leading-tight sm:text-4xl">
          Connect. Buy. Sell &amp; Succeed.
        </h1>
        <p class="mx-auto mt-3 max-w-xl text-sm text-white/80">
          The marketplace built for student entrepreneurs on campus - print jobs, tutoring, food, hair &amp;
          beauty and more, all from students you actually share a res with.
        </p>
        <div class="mt-6 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <RouterLink to="/register" class="btn-primary w-full px-6 py-2.5 text-sm sm:w-auto">Create a free account</RouterLink>
          <a
            href="#browse"
            class="inline-flex w-full items-center justify-center rounded-control border border-white/40 bg-white/10 px-6 py-2.5 text-sm font-semibold text-white sm:w-auto"
          >
            Browse the marketplace
          </a>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div class="card space-y-1 text-center">
          <div class="mx-auto flex h-9 w-9 items-center justify-center rounded-full bg-sky-blue/20 font-display font-bold text-uni-navy">1</div>
          <p class="font-display text-sm font-semibold text-uni-navy">Sign up with your student email</p>
        </div>
        <div class="card space-y-1 text-center">
          <div class="mx-auto flex h-9 w-9 items-center justify-center rounded-full bg-sky-blue/20 font-display font-bold text-uni-navy">2</div>
          <p class="font-display text-sm font-semibold text-uni-navy">Browse or list your hustle</p>
        </div>
        <div class="card space-y-1 text-center">
          <div class="mx-auto flex h-9 w-9 items-center justify-center rounded-full bg-sky-blue/20 font-display font-bold text-uni-navy">3</div>
          <p class="font-display text-sm font-semibold text-uni-navy">Connect and succeed</p>
        </div>
      </div>

      <div v-if="featuredBusinesses.length > 0">
        <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Featured businesses</h2>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <RouterLink
            v-for="b in featuredBusinesses"
            :key="b.businessId"
            :to="`/providers/${b.businessId}`"
            class="card space-y-2 transition hover:shadow-md"
          >
            <div class="flex items-center gap-2.5">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-sky-blue/20 font-display text-sm font-bold text-uni-navy">
                {{ b.businessName.charAt(0) }}
              </div>
              <div>
                <h3 class="font-display text-sm font-semibold text-uni-navy">{{ b.businessName }}</h3>
                <span class="badge bg-success/15 text-success">Verified</span>
              </div>
            </div>
            <p class="text-xs text-medium-grey">{{ b.category }} &middot; {{ b.activeListingCount }} active listings</p>
          </RouterLink>
        </div>
      </div>
    </template>

    <div v-else class="rounded-card bg-uni-navy px-6 py-10 text-white">
      <h1 class="font-display text-2xl font-bold sm:text-3xl">Discover student businesses on campus</h1>
      <p class="mt-2 max-w-2xl text-sm text-white/80">
        One trusted, searchable place for everything your fellow students are offering - no more
        scattered WhatsApp groups and lost social posts.
      </p>
    </div>

    <!-- Browse by category -->
    <div id="browse" v-if="categories.length > 0">
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Browse by category</h2>
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
        <button
          v-for="c in categories"
          :key="c"
          class="card flex flex-col items-center gap-1 py-4 text-center transition hover:shadow-md"
          @click="browseCategory(c)"
        >
          <span class="text-sm font-semibold text-uni-navy">{{ c }}</span>
          <span class="text-xs text-medium-grey">{{ categoryCounts[c] ?? 0 }} listings</span>
        </button>
      </div>
    </div>

    <!-- Trending -->
    <div v-if="trending.length > 0">
      <div class="mb-3 flex items-center gap-2">
        <span class="text-lg">🔥</span>
        <h2 class="font-display text-lg font-semibold text-uni-navy">Trending this week</h2>
      </div>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ListingCard v-for="listing in trending" :key="listing.id" :listing="listing" />
      </div>
    </div>

    <div class="flex flex-col gap-3 sm:flex-row">
      <div class="relative sm:max-w-sm sm:flex-1">
        <input
          v-model="keyword"
          type="search"
          placeholder="Search listings, categories..."
          class="input-field"
          @focus="suggestionsOpen = true"
          @blur="suggestionsOpen = false"
        />

        <div
          v-if="suggestionsOpen && keyword.trim() && (matchedCategories.length > 0 || matchedListings.length > 0)"
          class="absolute left-0 right-0 top-[calc(100%+6px)] z-10 rounded-card border border-light-grey bg-white p-2 shadow-md"
        >
          <template v-if="matchedCategories.length > 0">
            <p class="px-2 pb-1 pt-1 text-[11px] font-semibold uppercase tracking-wide text-medium-grey">Categories</p>
            <div
              v-for="c in matchedCategories"
              :key="c"
              class="cursor-pointer rounded-control px-2 py-1.5 text-sm font-medium text-uni-navy hover:bg-soft-grey"
              @mousedown.prevent="chooseSuggestedCategory(c)"
            >
              {{ c }} <span class="text-xs text-medium-grey">&middot; {{ categoryCounts[c] ?? 0 }} listings</span>
            </div>
          </template>

          <template v-if="matchedListings.length > 0">
            <p class="mt-2 px-2 pb-1 pt-1 text-[11px] font-semibold uppercase tracking-wide text-medium-grey">Listings</p>
            <div
              v-for="l in matchedListings"
              :key="l.id"
              class="flex cursor-pointer items-center gap-2 rounded-control px-2 py-1.5 hover:bg-soft-grey"
              @mousedown.prevent="goToListing(l.id)"
            >
              <img v-if="l.imageUrl" :src="l.imageUrl" alt="" class="h-7 w-7 shrink-0 rounded object-cover" />
              <div v-else class="h-7 w-7 shrink-0 rounded bg-sky-blue/20"></div>
              <div class="flex-1">
                <p class="text-sm font-medium text-charcoal" v-html="highlightMatch(l.name)"></p>
                <p class="text-xs text-medium-grey">{{ l.category }}</p>
              </div>
            </div>
          </template>
        </div>
      </div>
      <button class="btn-secondary flex items-center gap-2 text-sm lg:hidden" @click="mobileFiltersOpen = !mobileFiltersOpen">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
          <path d="M4 6h16M7 12h10M10 18h4" />
        </svg>
        Filters
      </button>
    </div>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-[240px_1fr]">
      <aside class="card h-fit space-y-5" :class="mobileFiltersOpen ? 'block' : 'hidden lg:block'">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-sm font-semibold text-uni-navy">Filters</h2>
          <button class="text-xs font-medium text-campus-teal underline" @click="clearFilters">Clear all</button>
        </div>

        <div class="space-y-2">
          <p class="text-xs font-semibold uppercase tracking-wide text-medium-grey">Category</p>
          <div class="flex flex-wrap gap-2">
            <button
              class="badge cursor-pointer"
              :class="category === '' ? 'bg-uni-navy text-white' : 'bg-soft-grey text-charcoal'"
              @click="category = ''"
            >
              All
            </button>
            <button
              v-for="c in categories"
              :key="c"
              class="badge cursor-pointer"
              :class="category === c ? 'bg-uni-navy text-white' : 'bg-soft-grey text-charcoal'"
              @click="category = c"
            >
              {{ c }}
            </button>
          </div>
        </div>

        <div class="space-y-2 border-t border-light-grey pt-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-medium-grey">Price range (ZAR)</p>
          <div class="flex items-center gap-2">
            <input v-model="minPrice" type="number" min="0" placeholder="Min" class="input-field" />
            <span class="text-medium-grey">-</span>
            <input v-model="maxPrice" type="number" min="0" placeholder="Max" class="input-field" />
          </div>
        </div>

        <div class="space-y-2 border-t border-light-grey pt-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-medium-grey">Type</p>
          <label class="flex items-center gap-2 text-sm text-charcoal">
            <input v-model="kind" type="radio" value="ALL" class="accent-campus-teal" /> All
          </label>
          <label class="flex items-center gap-2 text-sm text-charcoal">
            <input v-model="kind" type="radio" value="PRODUCT" class="accent-campus-teal" /> Products
          </label>
          <label class="flex items-center gap-2 text-sm text-charcoal">
            <input v-model="kind" type="radio" value="SERVICE" class="accent-campus-teal" /> Services
          </label>
        </div>

        <div class="border-t border-light-grey pt-4">
          <label class="flex items-center gap-2 text-sm text-charcoal">
            <input v-model="verifiedOnly" type="checkbox" class="accent-campus-teal" /> Verified sellers only
          </label>
        </div>
      </aside>

      <div class="space-y-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="text-sm text-medium-grey">{{ resultsLabel }}</p>
          <div class="flex flex-wrap items-center gap-2">
            <button
              v-if="auth.isAuthenticated && hasActiveFilters"
              class="btn-secondary flex items-center gap-1.5 text-xs"
              :disabled="savingSearch"
              @click="saveSearch"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
                <path d="M19 21 12 16l-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16Z" />
              </svg>
              {{ savingSearch ? "Saving..." : "Save this search" }}
            </button>
            <label class="text-xs text-medium-grey">Sort by</label>
            <select v-model="sort" class="input-field w-44">
              <option value="newest">Newest</option>
              <option value="price_asc">Price: Low to high</option>
              <option value="price_desc">Price: High to low</option>
              <option value="views">Most viewed</option>
            </select>
          </div>
        </div>

        <div v-if="searchSaved" class="flex items-center justify-between rounded-control border border-success/30 bg-success/10 px-4 py-2.5 text-sm text-success">
          <span>Search saved! We'll notify you when new listings match "{{ searchLabel }}".</span>
          <RouterLink to="/saved-searches" class="font-semibold underline">Manage saved searches</RouterLink>
        </div>

        <p v-if="error" class="text-sm text-danger">{{ error }}</p>
        <p v-else-if="loading" class="text-sm text-medium-grey">Loading listings...</p>
        <p v-else-if="listings.length === 0" class="text-sm text-medium-grey">
          No listings match your filters yet. Try widening your search.
        </p>

        <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <ListingCard v-for="listing in listings" :key="listing.id" :listing="listing" />
        </div>
      </div>
    </div>
  </section>
</template>
