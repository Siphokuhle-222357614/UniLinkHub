import { defineStore } from "pinia";
import { api } from "@/lib/api";
import type { ConversationSummaryView } from "@/lib/types";

export const useMessagesStore = defineStore("messages", {
  state: () => ({
    conversations: [] as ConversationSummaryView[],
    unreadCount: 0,
    initialized: false,
  }),
  actions: {
    async fetchAll() {
      try {
        const { data } = await api.get<ConversationSummaryView[]>("/conversations");
        this.conversations = data;
      } finally {
        this.initialized = true;
      }
    },
    async fetchUnreadCount() {
      try {
        const { data } = await api.get<{ count: number }>("/conversations/unread-count");
        this.unreadCount = data.count;
      } catch {
        // The nav badge is a nice-to-have; ignore failures here.
      }
    },
    reset() {
      this.conversations = [];
      this.unreadCount = 0;
      this.initialized = false;
    },
  },
});
