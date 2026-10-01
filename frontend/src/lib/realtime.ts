import { ref } from "vue";
import { getToken } from "@/lib/api";
import type { MessageDTO, NotificationCategory } from "@/lib/types";

/**
 * Live updates from GET /api/stream (Server-Sent Events).
 *
 * The browser's EventSource can't send an Authorization header, so this reads the stream with
 * fetch and parses the SSE frames itself. It reconnects with backoff when the connection drops;
 * the 30s badge polling elsewhere only runs while this is disconnected.
 */

export interface LiveMessage {
  conversationId: string;
  message: MessageDTO;
}

export interface LiveNotification {
  category: NotificationCategory;
  message: string;
}

type Handlers = {
  message: (e: LiveMessage) => void;
  notification: (e: LiveNotification) => void;
};

export const realtimeConnected = ref(false);

const listeners: { [K in keyof Handlers]: Set<Handlers[K]> } = {
  message: new Set(),
  notification: new Set(),
};

/** Subscribe to a live event. Returns an unsubscribe function. */
export function onRealtime<K extends keyof Handlers>(event: K, handler: Handlers[K]): () => void {
  listeners[event].add(handler);
  return () => listeners[event].delete(handler);
}

let controller: AbortController | null = null;
let retryTimer: ReturnType<typeof setTimeout> | undefined;
let attempt = 0;

export function startRealtime() {
  if (controller) return;
  attempt = 0;
  connect();
}

export function stopRealtime() {
  clearTimeout(retryTimer);
  controller?.abort();
  controller = null;
  realtimeConnected.value = false;
}

function scheduleReconnect() {
  realtimeConnected.value = false;
  if (!controller) return; // stopped on purpose
  const delay = Math.min(30_000, 1_000 * 2 ** attempt) + Math.random() * 1_000;
  attempt++;
  retryTimer = setTimeout(connect, delay);
}

async function connect() {
  const token = getToken();
  if (!token) {
    stopRealtime();
    return;
  }
  const mine = new AbortController();
  controller?.abort();
  controller = mine;
  try {
    const res = await fetch("/api/stream", {
      headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
      signal: mine.signal,
    });
    if (res.status === 401 || res.status === 403) {
      // Session ended or account blocked - the next normal API call explains why and logs out.
      stopRealtime();
      return;
    }
    if (!res.ok || !res.body) throw new Error(`Stream unavailable (${res.status})`);
    await read(res.body);
  } catch {
    // Network drop, server restart, or abort - handled below.
  }
  if (controller === mine) scheduleReconnect();
}

async function read(body: ReadableStream<Uint8Array>) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  for (;;) {
    const { done, value } = await reader.read();
    if (done) return;
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n?/g, "\n");
    let end: number;
    while ((end = buffer.indexOf("\n\n")) >= 0) {
      dispatch(buffer.slice(0, end));
      buffer = buffer.slice(end + 2);
    }
  }
}

function dispatch(frame: string) {
  let event = "message";
  const data: string[] = [];
  for (const line of frame.split("\n")) {
    if (line.startsWith(":")) continue; // heartbeat comment
    const colon = line.indexOf(":");
    const field = colon < 0 ? line : line.slice(0, colon);
    const value = colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "");
    if (field === "event") event = value;
    else if (field === "data") data.push(value);
  }
  if (data.length === 0) return;
  let payload: unknown;
  try {
    payload = JSON.parse(data.join("\n"));
  } catch {
    return;
  }
  if (event === "ready") {
    attempt = 0;
    realtimeConnected.value = true;
  } else if (event === "message" || event === "notification") {
    for (const handler of listeners[event]) (handler as (p: unknown) => void)(payload);
  }
}
