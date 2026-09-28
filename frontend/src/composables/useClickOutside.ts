import { onBeforeUnmount, onMounted, type Ref } from "vue";

/** Calls `handler` on any pointerdown outside `target` - used to close dropdown panels. */
export function useClickOutside(target: Ref<HTMLElement | null>, handler: () => void) {
  function onPointerDown(event: PointerEvent) {
    const el = target.value;
    if (el && !el.contains(event.target as Node)) handler();
  }
  function onKeydown(event: KeyboardEvent) {
    if (event.key === "Escape") handler();
  }
  onMounted(() => {
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeydown);
  });
  onBeforeUnmount(() => {
    document.removeEventListener("pointerdown", onPointerDown);
    document.removeEventListener("keydown", onKeydown);
  });
}
