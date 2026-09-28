<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useCartStore } from "@/stores/cart";
import { useToastStore } from "@/stores/toast";
import type { OrderDTO } from "@/lib/types";

const router = useRouter();
const cart = useCartStore();
const toast = useToastStore();

const fulfilmentMethod = ref<"PICKUP" | "DELIVERY">("PICKUP");
const note = ref("");
const promoCode = ref("");
const placing = ref(false);
const error = ref("");

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

async function placeOrder() {
  if (cart.lines.length === 0) return;
  placing.value = true;
  error.value = "";
  try {
    const { data } = await api.post<OrderDTO[]>("/orders", {
      items: cart.lines.map((l) => ({ listingId: l.listing.id, quantity: l.quantity })),
      fulfilmentMethod: fulfilmentMethod.value,
      note: note.value || null,
      promoCode: promoCode.value || null,
    });
    cart.clear();
    toast.success("Order placed!", "The seller will confirm it shortly.");
    router.push({ name: "my-orders", query: { placed: data.length.toString() } });
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    placing.value = false;
  }
}
</script>

<template>
  <section class="mx-auto max-w-5xl space-y-6">
    <h1 class="font-display text-2xl font-bold text-uni-navy">Checkout</h1>

    <div v-if="cart.lines.length === 0" class="card text-sm text-medium-grey">
      Your cart is empty. <RouterLink to="/" class="text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600">Browse listings</RouterLink> to add something.
    </div>

    <div v-else class="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <div class="space-y-4 lg:col-span-2">
        <div class="card space-y-2">
          <h3 class="font-display text-base font-bold text-uni-navy">Fulfilment method</h3>
          <label
            class="flex items-center gap-3 rounded-control border px-3 py-2.5"
            :class="fulfilmentMethod === 'PICKUP' ? 'border-campus-teal bg-campus-teal/5' : 'border-light-grey'"
          >
            <input v-model="fulfilmentMethod" type="radio" value="PICKUP" class="accent-campus-teal" />
            <span class="text-sm text-charcoal"><span class="font-medium">Campus pickup</span> - arrange a meet-up with the seller</span>
          </label>
          <label
            class="flex items-center gap-3 rounded-control border px-3 py-2.5"
            :class="fulfilmentMethod === 'DELIVERY' ? 'border-campus-teal bg-campus-teal/5' : 'border-light-grey'"
          >
            <input v-model="fulfilmentMethod" type="radio" value="DELIVERY" class="accent-campus-teal" />
            <span class="text-sm text-charcoal"><span class="font-medium">Hand delivery</span> - seller drops off on campus (if offered)</span>
          </label>
        </div>

        <div class="card space-y-2">
          <h3 class="font-display text-base font-bold text-uni-navy">Note for the seller (optional)</h3>
          <textarea v-model="note" rows="2" class="input-field resize-y" placeholder="e.g. I'll be near the library after 2pm"></textarea>
        </div>

        <div class="card space-y-2">
          <h3 class="font-display text-base font-bold text-uni-navy">Promo code</h3>
          <input v-model="promoCode" class="input-field" placeholder="Enter code (optional)" />
          <p class="text-xs text-medium-grey">Applied automatically at checkout if valid for the seller.</p>
        </div>
      </div>

      <div class="card h-fit space-y-3">
        <h3 class="font-display text-base font-bold text-uni-navy">Order summary</h3>
        <div class="space-y-1.5 border-b border-light-grey pb-3 text-sm">
          <div v-for="line in cart.lines" :key="line.listing.id" class="flex justify-between text-charcoal">
            <span>{{ line.listing.name }} &times; {{ line.quantity }}</span>
            <span>{{ formatPrice(line.listing.price * line.quantity) }}</span>
          </div>
        </div>
        <div class="flex justify-between border-b border-light-grey pb-3 text-base font-bold text-charcoal">
          <span>Subtotal</span>
          <span>{{ formatPrice(cart.subtotal) }}</span>
        </div>
        <p v-if="error" class="text-sm text-danger">{{ error }}</p>
        <button class="btn-primary w-full text-sm" :disabled="placing" @click="placeOrder">
          {{ placing ? "Placing order..." : "Place order" }}
        </button>
        <p class="text-center text-[11px] text-medium-grey">Payment is arranged directly with the seller on pickup.</p>
      </div>
    </div>
  </section>
</template>
