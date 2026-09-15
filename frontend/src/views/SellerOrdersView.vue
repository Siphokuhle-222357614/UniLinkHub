<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toast";
import type { OrderDTO, OrderStatus } from "@/lib/types";

const toast = useToastStore();
const orders = ref<OrderDTO[]>([]);
const loading = ref(false);
const error = ref("");
const acting = ref<string | null>(null);
const cancellingId = ref<string | null>(null);
const cancelReason = ref("");

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

const needingAction = computed(() => orders.value.filter((o) => o.status === "PLACED" || o.status === "CONFIRMED" || o.status === "READY").length);

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<OrderDTO[]>("/orders/seller");
    orders.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

const ACTION_LABELS: Record<string, string> = {
  confirm: "Order confirmed!",
  ready: "Marked ready for pickup!",
  complete: "Order completed!",
};

async function act(id: string, action: "confirm" | "ready" | "complete") {
  acting.value = id;
  error.value = "";
  try {
    await api.post(`/orders/${id}/${action}`);
    toast.success(ACTION_LABELS[action]);
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    acting.value = null;
  }
}

function startCancel(id: string) {
  cancellingId.value = id;
  cancelReason.value = "";
}

async function confirmCancel(id: string) {
  acting.value = id;
  error.value = "";
  try {
    await api.post(`/orders/${id}/cancel`, { reason: cancelReason.value });
    cancellingId.value = null;
    toast.info("Order cancelled");
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    acting.value = null;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-4xl space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="font-display text-2xl font-bold text-uni-navy">Orders</h1>
      <span v-if="needingAction > 0" class="badge bg-warning/15 text-warning">{{ needingAction }} need action</span>
    </div>
    <p class="text-sm text-medium-grey">Orders placed against your businesses' listings.</p>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="loading" class="text-sm text-medium-grey">Loading...</p>
    <div v-else-if="orders.length === 0" class="card text-sm text-medium-grey">No orders yet.</div>

    <div v-else class="space-y-3">
      <div v-for="o in orders" :key="o.id" class="card" :class="{ 'opacity-70': o.status === 'CANCELLED' || o.status === 'COMPLETED' }">
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p class="font-mono text-xs text-medium-grey">#{{ o.id.slice(0, 8).toUpperCase() }} &middot; {{ o.buyerName }}</p>
            <p class="mt-1 text-sm font-semibold text-charcoal">
              {{ o.items.map((i) => `${i.listingName} × ${i.quantity}`).join(", ") }}
            </p>
          </div>
          <span class="badge shrink-0" :class="STATUS_STYLES[o.status]">{{ STATUS_LABELS[o.status] }}</span>
        </div>

        <div v-if="cancellingId === o.id" class="mt-3 space-y-2 rounded-control border border-danger/30 bg-danger/5 p-3">
          <textarea v-model="cancelReason" rows="2" placeholder="Why is this order being cancelled?" class="input-field resize-y"></textarea>
          <div class="flex justify-end gap-2">
            <button class="btn-secondary text-xs" @click="cancellingId = null">Back</button>
            <button
              class="inline-flex items-center justify-center rounded-control bg-danger px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              :disabled="acting === o.id"
              @click="confirmCancel(o.id)"
            >
              Cancel order
            </button>
          </div>
        </div>

        <div v-else class="mt-3 flex items-center justify-between border-t border-light-grey pt-3">
          <p class="text-sm font-bold text-charcoal">{{ formatPrice(o.total) }}</p>
          <div class="flex gap-2">
            <button v-if="o.status === 'PLACED' || o.status === 'CONFIRMED'" class="btn-secondary text-xs" @click="startCancel(o.id)">
              Cancel
            </button>
            <button
              v-if="o.status === 'PLACED'"
              class="inline-flex items-center justify-center rounded-control bg-campus-teal px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              :disabled="acting === o.id"
              @click="act(o.id, 'confirm')"
            >
              Confirm order
            </button>
            <button
              v-if="o.status === 'CONFIRMED'"
              class="inline-flex items-center justify-center rounded-control bg-campus-teal px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              :disabled="acting === o.id"
              @click="act(o.id, 'ready')"
            >
              Mark ready for pickup
            </button>
            <button
              v-if="o.status === 'READY'"
              class="inline-flex items-center justify-center rounded-control bg-campus-teal px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              :disabled="acting === o.id"
              @click="act(o.id, 'complete')"
            >
              Mark completed
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
