<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ArrowRight, BadgeCheck, Eye, Search, SearchX, ShieldCheck } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { categoryMeta } from "@/lib/categoryMeta";
import { useCampuses } from "@/lib/campuses";
import EmptyState from "@/components/ui/EmptyState.vue";
import { useCategories } from "@/lib/categories";
import type { ProviderProfileDTO } from "@/lib/types";

const businesses = ref<ProviderProfileDTO[]>([]);
const categories = useCategories();
const keyword = ref("");
const category = ref("");
const verifiedOnly = ref(false);
const campus = ref("");
const campuses = useCampuses();
const loading = ref(false);
const error = ref("");

let debounceHandle: ReturnType<typeof setTimeout> | undefined;

async function search() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<ProviderProfileDTO[]>("/businesses", {
      params: {
        keyword: keyword.value || undefined,
        category: category.value || undefined,
        verifiedOnly: verifiedOnly.value || undefined,
        campus: campus.value || undefined,
      },
    });
    businesses.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

const resultsLabel = computed(() =>
  businesses.value.length === 1 ? "1 business found" : `${businesses.value.length} businesses found`,
);

watch([keyword, category, verifiedOnly, campus], () => {
  clearTimeout(debounceHandle);
  debounceHandle = setTimeout(search, 250);
});

onMounted(search);
</script>

<template>
  <section class="space-y-8">
    <div class="relative -mx-4 -mt-6 overflow-hidden bg-hero px-5 py-10 text-white sm:mx-0 sm:mt-0 sm:rounded-[24px] sm:px-10 sm:py-12">
      <div class="bg-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_right,black,transparent_70%)]"></div>
      <div class="relative max-w-2xl">
        <p class="eyebrow text-sky-blue">Provider directory</p>
        <h1 class="mt-2 font-display text-3xl font-bold tracking-tight text-white sm:text-4xl">Meet the businesses on campus</h1>
        <p class="mt-3 text-sm leading-relaxed text-white/70 sm:text-base">
          Every student-run business on UniLinkHub - see what they offer, how verified they are, and follow the ones you love.
        </p>
      </div>
    </div>

    <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div class="relative sm:max-w-sm sm:flex-1">
        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input v-model="keyword" type="search" placeholder="Search businesses…" aria-label="Search businesses" class="input-field pl-9" />
      </div>
      <select v-model="category" class="input-field sm:max-w-xs" aria-label="Category">
        <option value="">All categories</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="campus" class="input-field sm:max-w-[12rem]" aria-label="Campus">
        <option value="">All campuses</option>
        <option v-for="c in campuses" :key="c.key" :value="c.key">{{ c.label }}</option>
      </select>
      <button class="chip py-2.5" :class="{ 'chip-active': verifiedOnly }" :aria-pressed="verifiedOnly" @click="verifiedOnly = !verifiedOnly">
        <ShieldCheck class="h-4 w-4" /> Verified only
      </button>
      <p v-if="!loading && !error" class="text-sm text-medium-grey sm:ml-auto">{{ resultsLabel }}</p>
    </div>

    <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>

    <div v-else-if="loading" class="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3 3xl:grid-cols-4">
      <div v-for="n in 6" :key="n" class="skeleton h-44 rounded-card"></div>
    </div>

    <EmptyState v-else-if="businesses.length === 0" :icon="SearchX" title="No businesses match your search" description="Try another keyword or category." />

    <div v-else class="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3 3xl:grid-cols-4">
      <RouterLink v-for="b in businesses" :key="b.businessId" :to="`/providers/${b.businessId}`" class="group card card-interactive flex flex-col">
        <div class="flex items-start gap-3.5">
          <img v-if="b.imageUrl" :src="b.imageUrl" alt="" class="h-14 w-14 shrink-0 rounded-2xl object-cover ring-1 ring-light-grey" />
          <span v-else class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl font-display text-lg font-bold" :class="categoryMeta(b.category).tile">
            {{ b.businessName.charAt(0) }}
          </span>
          <div class="min-w-0 flex-1">
            <h3 class="flex items-center gap-1.5 font-display text-base font-semibold text-uni-navy">
              <span class="truncate">{{ b.businessName }}</span>
              <BadgeCheck v-if="b.verificationStatus === 'VERIFIED'" class="h-4 w-4 shrink-0 text-emerald-500" aria-label="Verified" />
            </h3>
            <p class="mt-0.5 flex flex-wrap items-center gap-x-1.5 gap-y-1 text-xs text-medium-grey">
              <span class="inline-flex min-w-0 items-center gap-1.5">
                <component :is="categoryMeta(b.category).icon" class="h-3.5 w-3.5 shrink-0" />
                <span>{{ b.category }}<template v-if="b.campusLabel"> · {{ b.campusLabel }}</template></span>
              </span>
              <span v-if="b.verificationStatus !== 'VERIFIED'" class="badge bg-amber-50 text-warning">Pending verification</span>
            </p>
          </div>
        </div>
        <p class="mb-4 mt-4 line-clamp-2 text-sm leading-relaxed text-charcoal">{{ b.description }}</p>
        <div class="mt-auto flex items-center justify-between border-t border-light-grey pt-3.5 text-xs text-medium-grey">
          <span class="flex items-center gap-3">
            <span><span class="font-semibold text-charcoal">{{ b.activeListingCount }}</span> listing{{ b.activeListingCount === 1 ? "" : "s" }}</span>
            <span class="inline-flex items-center gap-1"><Eye class="h-3.5 w-3.5" /> {{ b.totalViews }}</span>
          </span>
          <span class="inline-flex items-center gap-1 font-semibold text-teal-600">
            View profile <ArrowRight class="h-3.5 w-3.5 transition group-hover:translate-x-0.5" />
          </span>
        </div>
      </RouterLink>
    </div>
  </section>
</template>
