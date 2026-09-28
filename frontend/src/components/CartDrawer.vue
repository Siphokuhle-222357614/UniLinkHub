<script setup lang="ts">
import { useRouter } from "vue-router";
import { Minus, Plus, ShoppingBag, X } from "@lucide/vue";
import { useCartStore } from "@/stores/cart";
import { categoryMeta } from "@/lib/categoryMeta";
import { formatPrice } from "@/lib/format";

const router = useRouter();
const cart = useCartStore();

function goToCheckout() {
  cart.open = false;
  router.push("/checkout");
}

function browse() {
  cart.open = false;
  router.push("/");
}
</script>

<template>
  <div>
    <button class="btn-icon" aria-label="Cart" @click="cart.open = !cart.open">
      <ShoppingBag class="h-5 w-5" />
      <span v-if="cart.count > 0" class="count-dot">{{ cart.count > 9 ? "9+" : cart.count }}</span>
    </button>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="cart.open" class="fixed inset-0 z-40 bg-navy-950/40 backdrop-blur-sm" @click="cart.open = false"></div>
      </Transition>
      <Transition name="drawer">
        <aside v-if="cart.open" class="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col bg-white shadow-pop">
          <div class="flex items-center justify-between border-b border-light-grey px-6 py-5">
            <div>
              <h2 class="font-display text-lg font-bold text-uni-navy">Your cart</h2>
              <p class="text-xs text-medium-grey">{{ cart.count }} item{{ cart.count === 1 ? "" : "s" }}</p>
            </div>
            <button class="btn-icon h-9 w-9" aria-label="Close" @click="cart.open = false">
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-4">
            <div v-if="cart.lines.length === 0" class="flex h-full flex-col items-center justify-center text-center">
              <div class="flex h-16 w-16 items-center justify-center rounded-full bg-navy-50">
                <ShoppingBag class="h-7 w-7 text-navy-400" />
              </div>
              <p class="mt-4 font-display font-semibold text-uni-navy">Your cart is empty</p>
              <p class="mt-1 max-w-[16rem] text-sm text-medium-grey">Add products from any listing page and they'll show up here.</p>
              <button class="btn-secondary mt-5" @click="browse">Start exploring</button>
            </div>

            <ul v-else class="divide-y divide-light-grey">
              <li v-for="line in cart.lines" :key="line.listing.id" class="flex gap-4 py-4">
                <img v-if="line.listing.imageUrl" :src="line.listing.imageUrl" alt="" class="h-20 w-20 shrink-0 rounded-control object-cover" />
                <div
                  v-else
                  class="flex h-20 w-20 shrink-0 items-center justify-center rounded-control"
                  :style="{ background: categoryMeta(line.listing.category).gradient }"
                >
                  <component :is="categoryMeta(line.listing.category).icon" class="h-6 w-6 text-navy-400" />
                </div>
                <div class="flex min-w-0 flex-1 flex-col">
                  <div class="flex items-start justify-between gap-2">
                    <p class="line-clamp-2 text-sm font-semibold text-charcoal">{{ line.listing.name }}</p>
                    <p class="shrink-0 text-sm font-semibold text-uni-navy">{{ formatPrice(line.listing.price * line.quantity) }}</p>
                  </div>
                  <p class="text-xs text-medium-grey">{{ formatPrice(line.listing.price) }} each</p>
                  <div class="mt-auto flex items-center justify-between pt-2">
                    <div class="flex items-center rounded-full border border-light-grey">
                      <button class="qty-btn" aria-label="Decrease quantity" @click="cart.setQuantity(line.listing.id, line.quantity - 1)">
                        <Minus class="h-3.5 w-3.5" />
                      </button>
                      <span class="w-7 text-center text-sm font-semibold">{{ line.quantity }}</span>
                      <button class="qty-btn" aria-label="Increase quantity" @click="cart.setQuantity(line.listing.id, line.quantity + 1)">
                        <Plus class="h-3.5 w-3.5" />
                      </button>
                    </div>
                    <button class="text-xs font-semibold text-medium-grey hover:text-danger" @click="cart.remove(line.listing.id)">Remove</button>
                  </div>
                </div>
              </li>
            </ul>
          </div>

          <div v-if="cart.lines.length > 0" class="border-t border-light-grey bg-soft-grey/60 px-6 py-5 pb-safe">
            <div class="mb-1 flex justify-between text-sm text-medium-grey">
              <span>Subtotal</span>
              <span class="font-display text-lg font-bold text-uni-navy">{{ formatPrice(cart.subtotal) }}</span>
            </div>
            <p class="mb-4 text-xs text-medium-grey">Promo codes and fulfilment are chosen at checkout.</p>
            <button class="btn-primary w-full py-3" @click="goToCheckout">Checkout</button>
          </div>
        </aside>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.qty-btn {
  @apply flex h-8 w-8 items-center justify-center rounded-full text-medium-grey transition hover:bg-navy-50 hover:text-uni-navy;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}
</style>
