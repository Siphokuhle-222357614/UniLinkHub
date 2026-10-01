import { ref } from "vue";
import { api } from "@/lib/api";
import type { CampusOption } from "@/lib/types";

const campuses = ref<CampusOption[]>([]);
let loaded = false;

/** CPUT's campuses, from the backend (GET /api/campuses) so the list is defined in one place. */
export function useCampuses() {
  if (!loaded) {
    loaded = true;
    api
      .get<CampusOption[]>("/campuses")
      .then(({ data }) => (campuses.value = data))
      .catch(() => (loaded = false));
  }
  return campuses;
}

export function campusLabel(key: string | null | undefined): string | null {
  if (!key) return null;
  return campuses.value.find((c) => c.key === key)?.label ?? key.charAt(0) + key.slice(1).toLowerCase().replace("_", " ");
}
