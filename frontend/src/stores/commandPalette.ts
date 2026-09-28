import { defineStore } from "pinia";

export const useCommandPaletteStore = defineStore("commandPalette", {
  state: () => ({ open: false }),
  actions: {
    show() {
      this.open = true;
    },
    hide() {
      this.open = false;
    },
    toggle() {
      this.open = !this.open;
    },
  },
});
