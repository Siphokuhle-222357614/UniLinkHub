import { defineStore } from "pinia";
import type { ListingDTO } from "@/lib/types";

export interface CartLine {
  listing: ListingDTO;
  quantity: number;
}

const STORAGE_KEY = "unilinkhub.cart";

function loadFromStorage(): CartLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CartLine[]) : [];
  } catch {
    return [];
  }
}

function persist(lines: CartLine[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lines));
  } catch {
    // Best-effort only - a full/blocked storage just means the cart won't survive a refresh.
  }
}

export const useCartStore = defineStore("cart", {
  state: () => ({
    lines: loadFromStorage() as CartLine[],
    open: false,
  }),
  getters: {
    count: (state) => state.lines.reduce((sum, l) => sum + l.quantity, 0),
    subtotal: (state) => state.lines.reduce((sum, l) => sum + l.listing.price * l.quantity, 0),
  },
  actions: {
    add(listing: ListingDTO, quantity = 1) {
      const existing = this.lines.find((l) => l.listing.id === listing.id);
      if (existing) {
        existing.quantity += quantity;
      } else {
        this.lines.push({ listing, quantity });
      }
      persist(this.lines);
      this.open = true;
    },
    setQuantity(listingId: string, quantity: number) {
      if (quantity <= 0) {
        this.remove(listingId);
        return;
      }
      const existing = this.lines.find((l) => l.listing.id === listingId);
      if (existing) {
        existing.quantity = quantity;
        persist(this.lines);
      }
    },
    remove(listingId: string) {
      this.lines = this.lines.filter((l) => l.listing.id !== listingId);
      persist(this.lines);
    },
    clear() {
      this.lines = [];
      persist(this.lines);
    },
  },
});
