<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toast";
import type { BusinessDTO, ListingDTO } from "@/lib/types";

const router = useRouter();
const toast = useToastStore();

const listings = ref<ListingDTO[]>([]);
const businesses = ref<BusinessDTO[]>([]);
const loading = ref(false);
const error = ref("");
const statusFilter = ref<"ALL" | "ACTIVE" | "SOLD_OUT" | "INACTIVE">("ALL");
const keyword = ref("");
const selected = ref(new Set<string>());
const bulkActing = ref(false);
const openMenuId = ref<string | null>(null);
const duplicatingId = ref<string | null>(null);

function exportCsv() {
  const header = ["Listing", "Business", "Price", "Status", "Views"];
  const rows = filteredListings.value.map((l) => [
    l.name,
    businessNameById.value[l.businessId] ?? "-",
    l.price,
    l.status,
    l.viewCount,
  ]);
  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `my-listings-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

const businessNameById = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {};
  for (const b of businesses.value) map[b.id] = b.businessName;
  return map;
});

function statusCountFor(value: string): number {
  if (value === "ALL") return listings.value.length;
  return listings.value.filter((l) => l.status === value).length;
}

const filteredListings = computed(() => {
  let list = listings.value;
  if (statusFilter.value !== "ALL") {
    list = list.filter((l) => l.status === statusFilter.value);
  }
  const needle = keyword.value.trim().toLowerCase();
  if (needle) {
    list = list.filter((l) => l.name.toLowerCase().includes(needle) || l.category.toLowerCase().includes(needle));
  }
  return list;
});

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: "bg-success/15 text-success",
  SOLD_OUT: "bg-medium-grey/15 text-medium-grey",
  INACTIVE: "bg-danger/15 text-danger",
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Active",
  SOLD_OUT: "Sold out",
  INACTIVE: "Inactive",
};

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [{ data: listingData }, { data: businessData }] = await Promise.all([
      api.get<ListingDTO[]>("/listings/mine"),
      api.get<BusinessDTO[]>("/businesses/mine"),
    ]);
    listings.value = listingData;
    businesses.value = businessData;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

function manage() {
  router.push("/dashboard");
}

function toggleSelected(id: string) {
  if (selected.value.has(id)) {
    selected.value.delete(id);
  } else {
    selected.value.add(id);
  }
}

function toggleSelectAll() {
  if (selected.value.size === filteredListings.value.length) {
    selected.value.clear();
  } else {
    selected.value = new Set(filteredListings.value.map((l) => l.id));
  }
}

async function bulkAction(action: "reactivate" | "deactivate") {
  bulkActing.value = true;
  error.value = "";
  try {
    const count = selected.value.size;
    await Promise.all([...selected.value].map((id) => api.post(`/listings/${id}/${action}`)));
    selected.value.clear();
    toast.success(`${count} listing${count === 1 ? "" : "s"} ${action === "reactivate" ? "activated" : "deactivated"}!`);
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    bulkActing.value = false;
  }
}

function toggleMenu(id: string) {
  openMenuId.value = openMenuId.value === id ? null : id;
}

async function toggleStatus(listing: ListingDTO) {
  openMenuId.value = null;
  try {
    await api.post(`/listings/${listing.id}/${listing.status === "ACTIVE" ? "deactivate" : "reactivate"}`);
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function duplicateListing(listing: ListingDTO) {
  openMenuId.value = null;
  duplicatingId.value = listing.id;
  error.value = "";
  try {
    const payload = {
      businessId: listing.businessId,
      name: `${listing.name} (copy)`,
      description: listing.description,
      category: listing.category,
      price: listing.price,
      // Only photos uploaded to UniLinkHub can be reused - older listings may have links to other sites.
      imageUrl: listing.imageUrl?.startsWith("/api/images/") ? listing.imageUrl : null,
    };
    const { data: created } = listing.type === "PRODUCT"
      ? await api.post<ListingDTO>("/listings/products", {
          ...payload,
          stockQuantity: listing.stockQuantity,
        })
      : await api.post<ListingDTO>("/listings/services", {
          ...payload,
          durationMinutes: listing.durationMinutes,
          availabilitySchedule: listing.availabilitySchedule,
        });
    await api.post(`/listings/${created.id}/deactivate`);
    toast.success("Listing duplicated!", `Saved as "${created.name}" — inactive, ready to edit.`);
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    duplicatingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-4xl space-y-5 pb-16">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="font-display text-xl font-bold text-uni-navy">My listings</h1>
        <p class="text-sm text-medium-grey">
          Every listing across all your businesses, in one table instead of hunting per-business.
        </p>
      </div>
      <button
        class="inline-flex shrink-0 items-center gap-1.5 rounded-control border border-uni-navy bg-white px-3 py-2 text-sm font-semibold text-uni-navy disabled:opacity-50"
        :disabled="filteredListings.length === 0"
        @click="exportCsv"
      >
        Export CSV
      </button>
    </div>

    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="s in ['ALL', 'ACTIVE', 'SOLD_OUT', 'INACTIVE']"
          :key="s"
          class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
          :class="statusFilter === s ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
          @click="statusFilter = s as typeof statusFilter"
        >
          {{ s === "ALL" ? "All" : STATUS_LABELS[s] }} &nbsp;{{ statusCountFor(s) }}
        </button>
      </div>
      <input v-model="keyword" type="search" placeholder="Search my listings..." class="input-field sm:w-64" />
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="loading" class="text-sm text-medium-grey">Loading...</p>
    <p v-else-if="filteredListings.length === 0" class="card text-sm text-medium-grey">No listings match this filter.</p>

    <div v-else>
      <!-- Table (>=640px) - a dropdown menu that needs to escape the row means this can't also
           scroll horizontally without clipping it, so mobile gets its own layout below instead
           of a scrollable version of the same table. -->
      <div class="hidden overflow-visible rounded-card border border-light-grey bg-white p-0 sm:block">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-light-grey text-left text-xs uppercase tracking-wide text-medium-grey">
              <th class="w-10 px-4 py-3">
                <input type="checkbox" :checked="selected.size > 0 && selected.size === filteredListings.length" @change="toggleSelectAll" />
              </th>
              <th class="px-2 py-3">Listing</th>
              <th class="px-2 py-3">Business</th>
              <th class="px-2 py-3">Price</th>
              <th class="px-2 py-3">Status</th>
              <th class="px-2 py-3">Views</th>
              <th class="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="l in filteredListings"
              :key="l.id"
              class="relative border-b border-light-grey last:border-b-0"
              :class="{ 'bg-campus-teal/5': selected.has(l.id) }"
            >
              <td class="px-4 py-3"><input type="checkbox" :checked="selected.has(l.id)" @change="toggleSelected(l.id)" /></td>
              <td class="px-2 py-3 font-medium text-charcoal">{{ l.name }}</td>
              <td class="px-2 py-3 text-medium-grey">{{ businessNameById[l.businessId] ?? "-" }}</td>
              <td class="px-2 py-3 font-medium text-campus-teal">{{ formatPrice(l.price) }}</td>
              <td class="px-2 py-3">
                <span v-if="l.takenDownAt" class="badge bg-red-50 text-danger" :title="`Removed by an admin: ${l.takedownReason}`">Removed by admin</span>
                <span v-else class="badge" :class="STATUS_STYLES[l.status]">{{ STATUS_LABELS[l.status] }}</span>
              </td>
              <td class="px-2 py-3 text-medium-grey">{{ l.viewCount }}</td>
              <td class="relative px-4 py-3 text-right">
                <button class="px-1 text-medium-grey hover:text-charcoal" :disabled="duplicatingId === l.id" @click="toggleMenu(l.id)">
                  {{ duplicatingId === l.id ? "..." : "⋮" }}
                </button>
                <div
                  v-if="openMenuId === l.id"
                  class="absolute right-4 top-[calc(100%-4px)] z-10 w-44 rounded-card border border-light-grey bg-white py-1 text-left shadow-md"
                  @click.stop
                >
                  <button class="block w-full px-3 py-1.5 text-left text-sm text-charcoal hover:bg-soft-grey" @click="manage(); openMenuId = null">Edit</button>
                  <RouterLink
                    :to="`/listings/${l.id}/insights`"
                    class="block w-full px-3 py-1.5 text-left text-sm text-charcoal hover:bg-soft-grey"
                    @click="openMenuId = null"
                  >
                    Insights
                  </RouterLink>
                  <button class="block w-full px-3 py-1.5 text-left text-sm font-semibold text-campus-teal hover:bg-campus-teal/10" @click="duplicateListing(l)">Duplicate</button>
                  <button
                    v-if="l.status !== 'SOLD_OUT' && !l.takenDownAt"
                    class="block w-full px-3 py-1.5 text-left text-sm text-charcoal hover:bg-soft-grey"
                    @click="toggleStatus(l)"
                  >
                    {{ l.status === "ACTIVE" ? "Deactivate" : "Reactivate" }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Stacked cards (<640px) - direct action buttons instead of a dropdown, since a menu
           positioned to escape a narrow card is more trouble than it's worth on a phone. -->
      <div class="space-y-2 sm:hidden">
        <div v-for="l in filteredListings" :key="l.id" class="card" :class="{ '!border-campus-teal bg-campus-teal/5': selected.has(l.id) }">
          <div class="flex items-start justify-between gap-2">
            <label class="flex min-w-0 items-start gap-2">
              <input type="checkbox" class="mt-1 shrink-0" :checked="selected.has(l.id)" @change="toggleSelected(l.id)" />
              <span class="min-w-0">
                <span class="block truncate text-sm font-medium text-charcoal">{{ l.name }}</span>
                <span class="block truncate text-xs text-medium-grey">{{ businessNameById[l.businessId] ?? "-" }}</span>
              </span>
            </label>
            <span v-if="l.takenDownAt" class="badge shrink-0 bg-red-50 text-danger">Removed by admin</span>
            <span v-else class="badge shrink-0" :class="STATUS_STYLES[l.status]">{{ STATUS_LABELS[l.status] }}</span>
          </div>
          <div class="mt-2 flex items-center justify-between text-sm">
            <span class="font-semibold text-campus-teal">{{ formatPrice(l.price) }}</span>
            <span class="text-xs text-medium-grey">{{ l.viewCount }} views</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-x-3 gap-y-1 border-t border-light-grey pt-2">
            <button class="text-xs font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600" @click="manage">Edit</button>
            <RouterLink :to="`/listings/${l.id}/insights`" class="text-xs font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600">Insights</RouterLink>
            <button class="text-xs font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600 disabled:opacity-50" :disabled="duplicatingId === l.id" @click="duplicateListing(l)">
              {{ duplicatingId === l.id ? "Duplicating..." : "Duplicate" }}
            </button>
            <button v-if="l.status !== 'SOLD_OUT' && !l.takenDownAt" class="text-xs font-medium text-charcoal underline" @click="toggleStatus(l)">
              {{ l.status === "ACTIVE" ? "Deactivate" : "Reactivate" }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selected.size > 0" class="fixed inset-x-3 bottom-20 z-30 mx-auto max-w-3xl rounded-2xl bg-navy-900 px-4 py-3 shadow-pop sm:px-6 md:bottom-6">
      <div class="mx-auto flex max-w-4xl items-center justify-between">
        <p class="text-sm font-semibold text-white">{{ selected.size }} listing{{ selected.size === 1 ? "" : "s" }} selected</p>
        <div class="flex items-center gap-2">
          <button class="inline-flex items-center gap-1.5 rounded-control bg-success px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50" :disabled="bulkActing" @click="bulkAction('reactivate')">
            Activate
          </button>
          <button class="inline-flex items-center gap-1.5 rounded-control bg-white/10 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50" :disabled="bulkActing" @click="bulkAction('deactivate')">
            Deactivate
          </button>
          <button class="text-sm font-medium text-white/70" @click="selected.clear()">Cancel</button>
        </div>
      </div>
    </div>
  </section>
</template>
