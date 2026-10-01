<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Check, ChevronRight, Rocket, X } from "@lucide/vue";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { BusinessDTO, ListingDTO, PostView } from "@/lib/types";

/** "Get your shop ready" - the handful of things that make buyers trust a new seller, ticked off as they happen. */
const props = defineProps<{ businesses: BusinessDTO[]; listingsByBusiness: Record<string, ListingDTO[]> }>();
const emit = defineEmits<{ acceptRules: []; addBusiness: []; addListing: [] }>();

const auth = useAuthStore();
const hasPost = ref(false);

const dismissKey = computed(() => `unilinkhub.sellerChecklist.dismissed.${auth.user?.id}`);
const dismissed = ref(false);
watch(
  dismissKey,
  (key) => {
    try {
      dismissed.value = localStorage.getItem(key) === "1";
    } catch {
      dismissed.value = false;
    }
  },
  { immediate: true },
);

function dismiss() {
  dismissed.value = true;
  try {
    localStorage.setItem(dismissKey.value, "1");
  } catch {
    // Hidden for this visit only.
  }
}

const first = computed(() => props.businesses[0] ?? null);
const allListings = computed(() => Object.values(props.listingsByBusiness).flat());

// A step counts as done once any of the seller's businesses has done it.
const any = (test: (b: BusinessDTO) => boolean) => props.businesses.some(test);

watch(
  () => props.businesses.map((b) => b.id).join(),
  async () => {
    hasPost.value = false;
    const results = await Promise.allSettled(props.businesses.map((b) => api.get<PostView[]>(`/businesses/${b.id}/posts`)));
    hasPost.value = results.some((r) => r.status === "fulfilled" && r.value.data.length > 0);
  },
  { immediate: true },
);

interface Step {
  key: string;
  title: string;
  why: string;
  done: boolean;
  action?: { label: string; to?: string; run?: () => void };
}

const steps = computed<Step[]>(() => {
  const b = first.value;
  return [
    {
      key: "rules",
      title: "Agree to the marketplace rules",
      why: "Covers what can't be sold in residence and what happens if someone does.",
      done: !!auth.user?.sellerRulesAcceptedAt,
      action: { label: "Review rules", run: () => emit("acceptRules") },
    },
    {
      key: "business",
      title: "Add your business",
      why: "Your shop front - name, category and what you offer.",
      done: !!b,
      action: { label: "Add business", run: () => emit("addBusiness") },
    },
    {
      key: "pickup",
      title: "Set your campus and pickup spot",
      why: "Buyers filter by campus, and know exactly where to collect.",
      done: any((x) => !!x.campus && !!x.pickupLocation),
      action: { label: "Set location", to: "/account" },
    },
    {
      key: "logo",
      title: "Upload a logo",
      why: "Shops with a logo look established and get clicked more.",
      done: any((x) => !!x.imageUrl),
      action: { label: "Upload logo", to: "/account" },
    },
    {
      key: "listing",
      title: "List something with a photo",
      why: "Listings with photos sell far better than ones without.",
      done: allListings.value.some((l) => !!l.imageUrl),
      action: b ? { label: "New listing", run: () => emit("addListing") } : undefined,
    },
    {
      key: "post",
      title: "Share your first post",
      why: "Say hello to your followers - specials, new stock, opening hours.",
      done: hasPost.value,
      action: b ? { label: "Write a post", to: `/providers/${b.id}` } : undefined,
    },
    {
      key: "verified",
      title: "Get verified",
      why: "An admin checks new businesses. Verified sellers get a badge buyers look for.",
      done: any((x) => x.verificationStatus === "VERIFIED"),
    },
  ];
});

const doneCount = computed(() => steps.value.filter((s) => s.done).length);
const complete = computed(() => doneCount.value === steps.value.length);
const percent = computed(() => Math.round((doneCount.value / steps.value.length) * 100));
const next = computed(() => steps.value.find((s) => !s.done)?.key);
</script>

<template>
  <section v-if="!complete && !dismissed" class="card overflow-hidden p-0" aria-labelledby="checklist-title">
    <div class="flex items-start justify-between gap-3 border-b border-light-grey bg-gradient-to-r from-teal-50 to-navy-50 px-4 py-4 sm:px-5">
      <div class="flex items-center gap-3">
        <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white text-teal-600 shadow-xs"><Rocket class="h-5 w-5" /></span>
        <div>
          <h2 id="checklist-title" class="section-title">Get your shop ready</h2>
          <p class="text-xs text-medium-grey">{{ doneCount }} of {{ steps.length }} done - each step helps buyers trust you.</p>
        </div>
      </div>
      <button class="btn-icon h-8 w-8" aria-label="Hide the setup checklist" title="Hide" @click="dismiss"><X class="h-4 w-4" /></button>
    </div>

    <div class="h-1.5 bg-light-grey" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100" aria-label="Setup progress">
      <div class="h-full bg-teal-500 transition-all duration-500" :style="{ width: `${percent}%` }"></div>
    </div>

    <ol class="divide-y divide-light-grey">
      <li v-for="s in steps" :key="s.key" class="flex flex-wrap items-center gap-x-3 gap-y-1.5 px-4 py-3 sm:flex-nowrap sm:px-5" :class="{ 'bg-teal-50/40': s.key === next }">
        <span
          class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2"
          :class="s.done ? 'border-teal-500 bg-teal-500 text-white' : 'border-light-grey text-transparent'"
          :aria-label="s.done ? 'Done' : 'Not done yet'"
        >
          <Check class="h-3.5 w-3.5" :stroke-width="3" />
        </span>
        <div class="min-w-0 flex-1 basis-[calc(100%-2.25rem)] sm:basis-auto">
          <p class="text-sm font-semibold" :class="s.done ? 'text-medium-grey line-through' : 'text-charcoal'">{{ s.title }}</p>
          <p v-if="!s.done" class="text-xs text-medium-grey">{{ s.why }}</p>
        </div>
        <template v-if="!s.done && s.action">
          <RouterLink v-if="s.action.to" :to="s.action.to" class="btn-ghost ml-6 shrink-0 px-2.5 py-1.5 text-xs sm:ml-0" :class="{ '!text-teal-700': s.key === next }">
            {{ s.action.label }} <ChevronRight class="h-3.5 w-3.5" />
          </RouterLink>
          <button v-else class="btn-ghost ml-6 shrink-0 px-2.5 py-1.5 text-xs sm:ml-0" :class="{ '!text-teal-700': s.key === next }" @click="s.action.run?.()">
            {{ s.action.label }} <ChevronRight class="h-3.5 w-3.5" />
          </button>
        </template>
        <span v-else-if="!s.done && first" class="ml-9 shrink-0 text-xs text-medium-grey sm:ml-0">{{ s.key === "verified" && first?.verificationStatus === "REJECTED" ? "Not approved" : "In review" }}</span>
      </li>
    </ol>
  </section>
</template>
