import { onBeforeUnmount, watch } from "vue";
import { useRoute } from "vue-router";
import { onRealtime, startRealtime, stopRealtime } from "@/lib/realtime";
import { useAuthStore } from "@/stores/auth";
import { useMessagesStore } from "@/stores/messages";
import { useNotificationsStore } from "@/stores/notifications";
import { useToastStore } from "@/stores/toast";

/** Keeps the live connection open while someone is logged in and feeds its events into the stores. Call once, in App.vue. */
export function useRealtimeSync() {
  const auth = useAuthStore();
  const messages = useMessagesStore();
  const notifications = useNotificationsStore();
  const toast = useToastStore();
  const route = useRoute();

  watch(
    () => auth.isAuthenticated,
    (loggedIn) => (loggedIn ? startRealtime() : stopRealtime()),
    { immediate: true },
  );

  const offNotification = onRealtime("notification", (n) => {
    notifications.fetchUnreadCount();
    if (notifications.initialized) notifications.fetchAll();
    // Chat messages get their own toast below.
    if (n.category !== "MESSAGE") toast.info(n.message);
  });

  const offMessage = onRealtime("message", ({ conversationId, message }) => {
    const viewing = route.name === "conversation" && route.params.id === conversationId;
    if (!viewing) {
      messages.fetchUnreadCount();
      const preview = message.body.length > 80 ? `${message.body.slice(0, 80)}…` : message.body;
      toast.info("New message", preview);
    }
    if (messages.initialized) messages.fetchAll();
  });

  onBeforeUnmount(() => {
    offNotification();
    offMessage();
    stopRealtime();
  });
}
