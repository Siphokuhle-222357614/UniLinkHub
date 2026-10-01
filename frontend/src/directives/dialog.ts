import type { Directive } from "vue";

/**
 * v-dialog="close" makes an element behave like a proper modal for keyboard and screen-reader users:
 *  - focus moves into it (the first [autofocus] field, else the first control, else the panel itself),
 *  - Tab / Shift+Tab cycle inside it instead of wandering into the page behind,
 *  - Escape calls `close` (only the topmost dialog reacts when several are open),
 *  - the page behind stops scrolling,
 *  - focus returns to whatever opened it once it closes.
 */

const FOCUSABLE = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled]):not([type=hidden])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
].join(",");

interface Entry {
  el: HTMLElement;
  close: (() => void) | undefined;
  returnTo: HTMLElement | null;
}

const stack: Entry[] = [];

function focusables(el: HTMLElement): HTMLElement[] {
  return Array.from(el.querySelectorAll<HTMLElement>(FOCUSABLE)).filter((n) => n.offsetParent !== null || n === document.activeElement);
}

function onKeydown(e: KeyboardEvent) {
  const top = stack.at(-1);
  if (!top) return;
  if (e.key === "Escape") {
    e.preventDefault();
    e.stopPropagation();
    top.close?.();
    return;
  }
  if (e.key !== "Tab") return;
  const items = focusables(top.el);
  if (items.length === 0) {
    e.preventDefault();
    top.el.focus();
    return;
  }
  const first = items[0];
  const last = items[items.length - 1];
  const active = document.activeElement as HTMLElement | null;
  const inside = !!active && top.el.contains(active);
  if (e.shiftKey && (active === first || !inside)) {
    e.preventDefault();
    last.focus();
  } else if (!e.shiftKey && (active === last || !inside)) {
    e.preventDefault();
    first.focus();
  }
}

function lockScroll() {
  if (stack.length === 1) {
    // Keep the layout from jumping when the scrollbar disappears.
    const gap = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.overflow = "hidden";
    if (gap > 0) document.body.style.paddingRight = `${gap}px`;
    document.addEventListener("keydown", onKeydown, true);
  }
}

function unlockScroll() {
  if (stack.length === 0) {
    document.body.style.overflow = "";
    document.body.style.paddingRight = "";
    document.removeEventListener("keydown", onKeydown, true);
  }
}

export const vDialog: Directive<HTMLElement, (() => void) | undefined> = {
  mounted(el, binding) {
    const entry: Entry = { el, close: binding.value, returnTo: document.activeElement as HTMLElement | null };
    stack.push(entry);
    lockScroll();
    if (!el.hasAttribute("tabindex")) el.setAttribute("tabindex", "-1");
    requestAnimationFrame(() => {
      if (el.contains(document.activeElement)) return;
      const target = el.querySelector<HTMLElement>("[autofocus]") ?? focusables(el)[0] ?? el;
      target.focus({ preventScroll: true });
    });
  },
  updated(el, binding) {
    const entry = stack.find((s) => s.el === el);
    if (entry) entry.close = binding.value;
  },
  unmounted(el) {
    const i = stack.findIndex((s) => s.el === el);
    if (i < 0) return;
    const [entry] = stack.splice(i, 1);
    unlockScroll();
    if (entry.returnTo?.isConnected) entry.returnTo.focus({ preventScroll: true });
  },
};

declare module "vue" {
  interface GlobalDirectives {
    vDialog: typeof vDialog;
  }
}
