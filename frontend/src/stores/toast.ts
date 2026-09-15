import { defineStore } from "pinia";

export type ToastType = "success" | "error" | "info";

export interface ToastItem {
  id: number;
  type: ToastType;
  title: string;
  message?: string;
  duration: number;
}

let nextId = 1;
const timers = new Map<number, ReturnType<typeof setTimeout>>();
// Tracks how much time is left on each toast's auto-dismiss clock so a pause (hover) followed by
// a resume continues the countdown instead of restarting it - keeping the JS dismiss timer in
// sync with the CSS progress bar, which itself just freezes and continues via animation-play-state.
const remaining = new Map<number, number>();
const armedAt = new Map<number, number>();

export const useToastStore = defineStore("toast", {
  state: () => ({
    items: [] as ToastItem[],
  }),
  actions: {
    push(type: ToastType, title: string, message?: string, duration = 4200) {
      const id = nextId++;
      this.items.push({ id, type, title, message, duration });
      this.arm(id, duration);
      return id;
    },
    success(title: string, message?: string) {
      return this.push("success", title, message);
    },
    error(title: string, message?: string) {
      return this.push("error", title, message, 6000);
    },
    info(title: string, message?: string) {
      return this.push("info", title, message);
    },
    arm(id: number, ms: number) {
      this.clearTimer(id);
      if (ms > 0) {
        armedAt.set(id, Date.now());
        remaining.set(id, ms);
        timers.set(
          id,
          setTimeout(() => this.dismiss(id), ms),
        );
      }
    },
    pause(id: number) {
      const startedAt = armedAt.get(id);
      const left = remaining.get(id);
      if (startedAt != null && left != null) {
        remaining.set(id, Math.max(left - (Date.now() - startedAt), 0));
      }
      this.clearTimer(id);
    },
    resume(id: number) {
      const left = remaining.get(id);
      if (left != null) {
        this.arm(id, left);
      }
    },
    clearTimer(id: number) {
      const handle = timers.get(id);
      if (handle) {
        clearTimeout(handle);
        timers.delete(id);
      }
    },
    dismiss(id: number) {
      this.clearTimer(id);
      remaining.delete(id);
      armedAt.delete(id);
      this.items = this.items.filter((t) => t.id !== id);
    },
  },
});
