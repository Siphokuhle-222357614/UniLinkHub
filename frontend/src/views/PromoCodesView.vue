<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toast";
import type { BusinessDTO, ListingDTO, PromoCodeDTO } from "@/lib/types";

const toast = useToastStore();
const businesses = ref<BusinessDTO[]>([]);
const selectedBusinessId = ref("");
const listings = ref<ListingDTO[]>([]);
const promoCodes = ref<PromoCodeDTO[]>([]);
const loading = ref(false);
const error = ref("");
const creating = ref(false);
const togglingId = ref<string | null>(null);

const form = ref({
  code: "",
  discountType: "PERCENT" as "PERCENT" | "FIXED",
  discountValue: 10,
  scopeListingId: "",
  expiresAt: "",
});

function formatDiscount(p: PromoCodeDTO) {
  return p.discountType === "PERCENT" ? `${p.discountValue}%` : `R${p.discountValue}`;
}

async function loadBusinesses() {
  try {
    const { data } = await api.get<BusinessDTO[]>("/businesses/mine");
    businesses.value = data;
    if (data.length > 0) selectedBusinessId.value = data[0].id;
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function loadForBusiness() {
  if (!selectedBusinessId.value) return;
  loading.value = true;
  error.value = "";
  try {
    const [{ data: codes }, { data: listingData }] = await Promise.all([
      api.get<PromoCodeDTO[]>(`/businesses/${selectedBusinessId.value}/promo-codes`),
      api.get<ListingDTO[]>(`/listings/business/${selectedBusinessId.value}`),
    ]);
    promoCodes.value = codes;
    listings.value = listingData;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function createCode() {
  if (!form.value.code.trim()) return;
  creating.value = true;
  error.value = "";
  try {
    await api.post(`/businesses/${selectedBusinessId.value}/promo-codes`, {
      code: form.value.code,
      discountType: form.value.discountType,
      discountValue: form.value.discountValue,
      scopeListingId: form.value.scopeListingId || null,
      expiresAt: form.value.expiresAt ? `${form.value.expiresAt}T23:59:59` : null,
    });
    toast.success("Promo code created!");
    form.value = { code: "", discountType: "PERCENT", discountValue: 10, scopeListingId: "", expiresAt: "" };
    await loadForBusiness();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    creating.value = false;
  }
}

async function toggleActive(p: PromoCodeDTO) {
  togglingId.value = p.id;
  try {
    await api.patch(`/promo-codes/${p.id}/active`, { active: !p.active });
    await loadForBusiness();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    togglingId.value = null;
  }
}

watch(selectedBusinessId, loadForBusiness);
onMounted(loadBusinesses);
</script>

<template>
  <section class="mx-auto max-w-4xl space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="font-display text-2xl font-bold text-uni-navy">Promo codes</h1>
      <select v-if="businesses.length > 1" v-model="selectedBusinessId" class="input-field w-56">
        <option v-for="b in businesses" :key="b.id" :value="b.id">{{ b.businessName }}</option>
      </select>
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="businesses.length === 0" class="card text-sm text-medium-grey">
      You need a business before you can create promo codes.
    </p>

    <template v-else>
      <div class="card space-y-3">
        <h3 class="font-display text-base font-bold text-uni-navy">Create a promo code</h3>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-4">
          <input v-model="form.code" class="input-field uppercase" placeholder="e.g. BACK2CAMPUS" />
          <div class="flex gap-2">
            <select v-model="form.discountType" class="input-field">
              <option value="PERCENT">%</option>
              <option value="FIXED">R off</option>
            </select>
            <input v-model.number="form.discountValue" type="number" min="0" class="input-field" placeholder="Value" />
          </div>
          <select v-model="form.scopeListingId" class="input-field">
            <option value="">All my listings</option>
            <option v-for="l in listings" :key="l.id" :value="l.id">{{ l.name }}</option>
          </select>
          <input v-model="form.expiresAt" type="date" class="input-field" />
        </div>
        <div class="flex justify-end">
          <button class="btn-primary text-sm" :disabled="creating || !form.code.trim()" @click="createCode">
            {{ creating ? "Creating..." : "Create code" }}
          </button>
        </div>
      </div>

      <p v-if="loading" class="text-sm text-medium-grey">Loading...</p>
      <div v-else-if="promoCodes.length === 0" class="card text-sm text-medium-grey">No promo codes yet.</div>

      <div v-else class="card overflow-x-auto p-0">
        <table class="w-full text-left text-sm">
          <thead>
            <tr class="border-b border-light-grey text-xs uppercase tracking-wide text-medium-grey">
              <th class="px-4 py-3 font-medium">Code</th>
              <th class="px-2 py-3 font-medium">Discount</th>
              <th class="px-2 py-3 font-medium">Scope</th>
              <th class="px-2 py-3 font-medium">Used</th>
              <th class="px-2 py-3 font-medium">Expires</th>
              <th class="px-4 py-3 font-medium">Active</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-light-grey">
            <tr v-for="p in promoCodes" :key="p.id" :class="{ 'opacity-60': !p.usable }">
              <td class="px-4 py-2.5 font-mono font-semibold text-charcoal">{{ p.code }}</td>
              <td class="px-2 py-2.5 text-charcoal">{{ formatDiscount(p) }}</td>
              <td class="px-2 py-2.5 text-charcoal">{{ p.scopeListingName ?? "All listings" }}</td>
              <td class="px-2 py-2.5 text-charcoal">{{ p.usageCount }} time{{ p.usageCount === 1 ? "" : "s" }}</td>
              <td class="px-2 py-2.5 text-charcoal">{{ p.expiresAt ? new Date(p.expiresAt).toLocaleDateString("en-ZA") : "No expiry" }}</td>
              <td class="px-4 py-2.5">
                <button
                  class="rounded-full px-3 py-1 text-xs font-semibold disabled:opacity-50"
                  :class="p.active ? 'bg-campus-teal text-white' : 'bg-light-grey text-medium-grey'"
                  :disabled="togglingId === p.id"
                  @click="toggleActive(p)"
                >
                  {{ p.active ? "Active" : "Off" }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>
