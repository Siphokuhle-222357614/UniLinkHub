<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import type { OrderDTO, OrderStatus } from "@/lib/types";

const route = useRoute();
const orders = ref<OrderDTO[]>([]);
const loading = ref(false);
const error = ref("");

const STATUS_STYLES: Record<OrderStatus, string> = {
  PLACED: "bg-warning/15 text-warning",
  CONFIRMED: "bg-info/15 text-info",
  READY: "bg-slate-blue/15 text-slate-blue",
  COMPLETED: "bg-success/15 text-success",
  CANCELLED: "bg-danger/15 text-danger",
};

const STATUS_LABELS: Record<OrderStatus, string> = {
  PLACED: "Placed",
  CONFIRMED: "Confirmed",
  READY: "Ready for pickup",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-ZA", { day: "numeric", month: "short" });
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<OrderDTO[]>("/orders/mine");
    orders.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-4">
    <h1 class="font-display text-2xl font-bold text-uni-navy">My orders</h1>

    <div v-if="route.query.placed" class="rounded-control border border-success/30 bg-success/10 px-4 py-2.5 text-sm text-success">
      Order placed! The seller{{ Number(route.query.placed) > 1 ? "s" : "" }} will confirm it shortly.
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="loading" class="text-sm text-medium-grey">Loading...</p>
    <div v-else-if="orders.length === 0" class="card text-sm text-medium-grey">
      You haven't placed any orders yet.
    </div>

    <div v-else class="space-y-4">
      <div v-for="o in orders" :key="o.id" class="card" :class="{ 'opacity-70': o.status === 'CANCELLED' }">
        <div class="flex items-start justify-between gap-2">
          <div>
            <p class="font-mono text-xs text-medium-grey">#{{ o.id.slice(0, 8).toUpperCase() }} &middot; Placed {{ formatDate(o.createdAt) }}</p>
            <p class="mt-1 text-sm font-semibold text-charcoal">
              {{ o.items.map((i) => `${i.listingName} × ${i.quantity}`).join(", ") }}
            </p>
            <p class="text-xs text-medium-grey">{{ o.businessName }}</p>
          </div>
          <span class="badge shrink-0" :class="STATUS_STYLES[o.status]">{{ STATUS_LABELS[o.status] }}</span>
        </div>
        <p v-if="o.status === 'CANCELLED' && o.cancelReason" class="mt-2 border-t border-light-grey pt-2 text-xs text-medium-grey">
          Cancelled by seller: "{{ o.cancelReason }}"
        </p>
        <div class="mt-3 flex items-center justify-between border-t border-light-grey pt-3">
          <p class="text-sm font-bold text-charcoal">{{ formatPrice(o.total) }}</p>
          <RouterLink to="/messages" class="text-xs font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600">Message seller</RouterLink>
        </div>
      </div>
    </div>
  </section>
</template>
