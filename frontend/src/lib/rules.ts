import { ref } from "vue";
import { api } from "@/lib/api";
import type { RestrictedCategory } from "@/lib/types";

const categories = ref<RestrictedCategory[]>([]);
let loaded = false;

/** The restricted-items list, straight from the backend so it always matches what's enforced. */
export function useRestrictedCategories() {
  if (!loaded) {
    loaded = true;
    api
      .get<RestrictedCategory[]>("/rules/restricted-items")
      .then(({ data }) => (categories.value = data))
      .catch(() => (loaded = false));
  }
  return categories;
}

export const CONSEQUENCES = [
  "The listing is taken down and the seller is notified why.",
  "The seller's account can be suspended - they lose access to buying and selling.",
  "Serious cases are reported to CPUT residence management, and illegal items to the police.",
];
