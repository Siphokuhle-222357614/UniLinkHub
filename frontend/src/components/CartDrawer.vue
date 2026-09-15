<script setup lang="ts">
import { useRouter } from "vue-router";
import { useCartStore } from "@/stores/cart";

const router = useRouter();
const cart = useCartStore();

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

function goToCheckout() {
  cart.open = false;
  router.push("/checkout");
}
</script>

<template>
  <div>
    <button
      class="relative flex h-9 w-9 items-center justify-center rounded-full border border-light-grey"
      aria-label="Cart"
      @click="cart.open = !cart.open"
    >
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
        <path
          d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 4.6A1 1 0 0 0 5.6 19H17M17 19a2 2 0 1 0 0 4 2 2 0 0 0 0-4ZM9 19a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z"
        />
      </svg>
      <span
        v-if="cart.count > 0"
        class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-danger text-[10px] font-bold text-white"
      >
        {{ cart.count > 9 ? "9+" : cart.count }}
      </span>
    </button>

    <Teleport to="body">
      <div v-if="cart.open" class="fixed inset-0 z-30 bg-charcoal/40" @click="cart.open = false"></div>
      <aside
        v-if="cart.open"
        class="fixed inset-y-0 right-0 z-30 flex w-full max-w-sm flex-col bg-white shadow-xl"
      >
        <div class="flex items-center justify-between border-b border-light-grey px-5 py-4">
          <h2 class="font-display text-lg font-bold text-uni-navy">Your cart</h2>
          <button class="rounded-full p-1 text-medium-grey hover:bg-soft-grey" aria-label="Close" @click="cart.open = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="flex-1 space-y-4 overflow-y-auto px-5 py-4">
          <p v-if="cart.lines.length === 0" class="text-sm text-medium-grey">
            Your cart is empty. Add a product from its listing page.
          </p>
          <div v-for="line in cart.lines" :key="line.listing.id" class="flex gap-3 border-b border-light-grey pb-4 last:border-b-0">
            <img v-if="line.listing.imageUrl" :src="line.listing.imageUrl" alt="" class="h-16 w-16 shrink-0 rounded-control object-cover" />
            <div v-else class="h-16 w-16 shrink-0 rounded-control bg-soft-grey"></div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold text-charcoal">{{ line.listing.name }}</p>
              <div class="mt-2 flex items-center justify-between">
                <div class="flex items-center rounded-control border border-light-grey">
                  <button class="px-2.5 py-1 text-medium-grey hover:text-charcoal" @click="cart.setQuantity(line.listing.id, line.quantity - 1)">
                    &minus;
                  </button>
                  <span class="px-2 text-sm">{{ line.quantity }}</span>
                  <button class="px-2.5 py-1 text-medium-grey hover:text-charcoal" @click="cart.setQuantity(line.listing.id, line.quantity + 1)">
                    +
                  </button>
                </div>
                <p class="text-sm font-semibold text-charcoal">{{ formatPrice(line.listing.price * line.quantity) }}</p>
              </div>
              <button class="mt-1.5 text-xs font-medium text-danger hover:underline" @click="cart.remove(line.listing.id)">
                Remove
              </button>
            </div>
          </div>
        </div>

        <div v-if="cart.lines.length > 0" class="border-t border-light-grey px-5 py-4">
          <div class="mb-3 flex justify-between text-base font-bold text-charcoal">
            <span>Subtotal</span>
            <span>{{ formatPrice(cart.subtotal) }}</span>
          </div>
          <button class="btn-primary w-full text-sm" @click="goToCheckout">Checkout</button>
        </div>
      </aside>
    </Teleport>
  </div>
</template>
