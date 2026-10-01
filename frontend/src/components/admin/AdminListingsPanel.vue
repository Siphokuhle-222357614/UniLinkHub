<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { Ban, ExternalLink, Flag, Search, Undo2, X } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { categoryMeta } from "@/lib/categoryMeta";
import { formatPrice, relativeTime } from "@/lib/format";
import { useToastStore } from "@/stores/toast";
import type { AdminListingView, ListingDTO, PageResponse } from "@/lib/types";

const props = defineProps<{ businessId?: string | null; businessName?: string | null }>();
const emit = defineEmits<{ clearBusiness: [] }>();

const toast = useToastStore();

type StatusFilter = "ALL" | "ACTIVE" | "INACTIVE" | "SOLD_OUT" | "REMOVED";
const FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "ACTIVE", label: "Live" },
  { value: "SOLD_OUT", label: "Sold out" },
  { value: "INACTIVE", label: "Hidden by seller" },
  { value: "REMOVED", label: "Removed by admin" },
];
const TAKEDOWN_PRESETS = ["Selling alcohol", "Selling drugs", "Selling weapons", "Cigarettes or vapes", "Cheating service", "Fake or stolen goods"];

const rows = ref<AdminListingView[]>([]);
const loading = ref(false);
const error = ref("");
const status = ref<StatusFilter>("ALL");
const keyword = ref("");

const PAGE_SIZE = 30;
const page = ref(0);
const hasNext = ref(false);
const total = ref(0);
const loadingMore = ref(false);

async function loadMore() {
  loadingMore.value = true;
  try {
    const { data } = await api.get<PageResponse<AdminListingView>>("/admin/listings", {
      params: { status: status.value, businessId: props.businessId || undefined, page: page.value + 1, size: PAGE_SIZE },
    });
    rows.value = [...rows.value, ...data.items];
    page.value = data.page;
    hasNext.value = data.hasNext;
  } finally {
    loadingMore.value = false;
  }
}

const takedownFor = ref<AdminListingView | null>(null);
const takedownReason = ref("");
const acting = ref<string | null>(null);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<PageResponse<AdminListingView>>("/admin/listings", {
      params: { status: status.value, businessId: props.businessId || undefined, page: 0, size: PAGE_SIZE },
    });
    rows.value = data.items;
    page.value = 0;
    hasNext.value = data.hasNext;
    total.value = data.totalItems;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

const visible = computed(() => {
  const needle = keyword.value.trim().toLowerCase();
  if (!needle) return rows.value;
  return rows.value.filter(
    (r) =>
      r.listing.name.toLowerCase().includes(needle) ||
      r.businessName.toLowerCase().includes(needle) ||
      r.ownerName.toLowerCase().includes(needle),
  );
});

const reportedCount = computed(() => rows.value.filter((r) => r.openReports > 0).length);

function replace(updated: ListingDTO) {
  const row = rows.value.find((r) => r.listing.id === updated.id);
  if (row) row.listing = updated;
}

async function confirmTakedown() {
  const row = takedownFor.value;
  if (!row || !takedownReason.value.trim()) return;
  acting.value = row.listing.id;
  try {
    const { data } = await api.post<ListingDTO>(`/admin/listings/${row.listing.id}/take-down`, { reason: takedownReason.value.trim() });
    replace(data);
    takedownFor.value = null;
    toast.success("Listing taken down", `${row.ownerName} has been notified.`);
  } finally {
    acting.value = null;
  }
}

async function restore(row: AdminListingView) {
  acting.value = row.listing.id;
  try {
    const { data } = await api.post<ListingDTO>(`/admin/listings/${row.listing.id}/restore`);
    replace(data);
    toast.success("Listing restored", "It stays hidden until the seller switches it back on.");
  } finally {
    acting.value = null;
  }
}

function statusBadge(l: ListingDTO): { label: string; cls: string } {
  if (l.takenDownAt) return { label: "Removed by admin", cls: "bg-red-50 text-danger" };
  if (l.status === "ACTIVE") return { label: "Live", cls: "bg-emerald-50 text-emerald-700" };
  if (l.status === "SOLD_OUT") return { label: "Sold out", cls: "bg-slate-100 text-medium-grey" };
  return { label: "Hidden by seller", cls: "bg-amber-50 text-warning" };
}

watch([status, () => props.businessId], load);
onMounted(load);
</script>

<template>
  <section class="space-y-5">
    <div>
      <h2 class="section-title">All listings</h2>
      <p class="text-sm text-medium-grey">
        Everything sellers have listed, with reported listings first. Take down anything that breaks the marketplace rules - the seller is
        told why and can't switch it back on.
      </p>
    </div>

    <div v-if="props.businessId" class="flex items-center justify-between gap-3 rounded-control border border-navy-100 bg-navy-50 px-4 py-2.5 text-sm text-navy-800">
      <span>Showing listings from <span class="font-semibold">{{ props.businessName ?? "one business" }}</span></span>
      <button class="inline-flex items-center gap-1 text-xs font-semibold hover:underline" @click="emit('clearBusiness')"><X class="h-3.5 w-3.5" /> Show all</button>
    </div>

    <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
      <div class="flex flex-wrap gap-1.5">
        <button v-for="f in FILTERS" :key="f.value" class="chip" :class="{ 'chip-active': status === f.value }" @click="status = f.value">
          {{ f.label }}
        </button>
      </div>
      <div class="relative lg:w-72">
        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input v-model="keyword" type="search" placeholder="Search listing, business or seller…" aria-label="Search listings" class="input-field pl-9" />
      </div>
    </div>

    <p v-if="reportedCount > 0 && !loading" class="flex items-center gap-2 text-sm font-medium text-danger">
      <Flag class="h-4 w-4" /> {{ reportedCount }} listing{{ reportedCount === 1 ? " has" : "s have" }} open reports
    </p>

    <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>
    <div v-else-if="loading" class="space-y-2">
      <div v-for="n in 4" :key="n" class="skeleton h-20 rounded-card"></div>
    </div>
    <p v-else-if="visible.length === 0" class="card text-sm text-medium-grey">No listings match this filter.</p>

    <ul v-else class="space-y-2.5">
      <li
        v-for="r in visible"
        :key="r.listing.id"
        class="card flex flex-col gap-4 p-4 sm:flex-row sm:items-center"
        :class="r.openReports > 0 && !r.listing.takenDownAt ? 'border-red-200' : ''"
      >
        <div class="flex min-w-0 flex-1 items-center gap-3.5">
          <img v-if="r.listing.imageUrl" :src="r.listing.imageUrl" alt="" class="h-14 w-14 shrink-0 rounded-xl object-cover ring-1 ring-light-grey" />
          <span v-else class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl" :class="categoryMeta(r.listing.category).tile">
            <component :is="categoryMeta(r.listing.category).icon" class="h-5 w-5" />
          </span>
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <RouterLink :to="`/listings/${r.listing.id}`" class="truncate font-semibold text-uni-navy hover:text-teal-700">{{ r.listing.name }}</RouterLink>
              <span class="badge" :class="statusBadge(r.listing).cls">{{ statusBadge(r.listing).label }}</span>
              <span v-if="r.openReports > 0" class="badge bg-red-50 text-danger"><Flag class="h-3 w-3" /> {{ r.openReports }} report{{ r.openReports === 1 ? "" : "s" }}</span>
            </div>
            <p class="truncate text-xs text-medium-grey">
              {{ r.businessName }} · {{ r.ownerName }} · {{ r.listing.category }} · {{ formatPrice(r.listing.price) }} · listed {{ relativeTime(r.listing.createdAt) }}
            </p>
            <p v-if="r.listing.takenDownAt" class="mt-0.5 text-xs text-danger">Reason: {{ r.listing.takedownReason }}</p>
          </div>
        </div>
        <div class="flex shrink-0 gap-2">
          <RouterLink :to="`/listings/${r.listing.id}`" class="btn-ghost px-3 py-2 text-xs"><ExternalLink class="h-3.5 w-3.5" /> Open</RouterLink>
          <button v-if="!r.listing.takenDownAt" class="btn-secondary px-3 py-2 text-xs text-danger" :disabled="acting === r.listing.id" @click="takedownFor = r; takedownReason = ''">
            <Ban class="h-3.5 w-3.5" /> Take down
          </button>
          <button v-else class="btn-secondary px-3 py-2 text-xs" :disabled="acting === r.listing.id" @click="restore(r)">
            <Undo2 class="h-3.5 w-3.5" /> Restore
          </button>
        </div>
      </li>
    </ul>
    <div v-if="!loading && rows.length > 0" class="flex flex-col items-center gap-2">
      <button v-if="hasNext" class="btn-secondary px-6" :disabled="loadingMore" @click="loadMore">{{ loadingMore ? "Loading…" : "Load more" }}</button>
      <p class="text-xs text-medium-grey">Showing {{ rows.length }} of {{ total }}</p>
    </div>

    <Teleport to="body">
      <div v-if="takedownFor" class="modal-backdrop" @click.self="takedownFor = null">
        <div v-dialog="() => (takedownFor = null)" class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="admin-takedown-title">
          <h2 id="admin-takedown-title" class="font-display text-lg font-bold text-uni-navy">Take down "{{ takedownFor.listing.name }}"?</h2>
          <p class="mb-4 mt-1 text-sm text-medium-grey">
            It disappears from the marketplace straight away. {{ takedownFor.ownerName }} is told your reason and warned that repeated or
            serious breaches can get their account suspended.
          </p>
          <div class="mb-3 flex flex-wrap gap-1.5">
            <button v-for="p in TAKEDOWN_PRESETS" :key="p" type="button" class="chip" :class="{ 'chip-active': takedownReason === p }" @click="takedownReason = p">{{ p }}</button>
          </div>
          <label for="admin-takedown-reason" class="field-label">Reason shown to the seller</label>
          <textarea id="admin-takedown-reason" v-model="takedownReason" rows="3" class="input-field" placeholder="e.g. Selling alcohol in residence"></textarea>
          <div class="mt-6 flex gap-2">
            <button class="btn-secondary flex-1" @click="takedownFor = null">Cancel</button>
            <button class="btn-danger flex-1" :disabled="!takedownReason.trim() || acting === takedownFor.listing.id" @click="confirmTakedown">Take down</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>
