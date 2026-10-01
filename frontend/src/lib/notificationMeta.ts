import type { Component } from "vue";
import { BadgeCheck, Bell, CalendarDays, CircleQuestionMark, Megaphone, MessageCircle, Package, Newspaper, Receipt, Search, ShieldAlert, Star } from "@lucide/vue";

export interface NotificationMeta {
  icon: Component;
  /** Tailwind classes for the round icon badge (background + icon colour). */
  tone: string;
}

const META: Record<string, NotificationMeta> = {
  BUSINESS: { icon: BadgeCheck, tone: "bg-emerald-50 text-emerald-600" },
  BOOKING: { icon: CalendarDays, tone: "bg-violet-50 text-violet-600" },
  STOCK: { icon: Package, tone: "bg-orange-50 text-orange-600" },
  REVIEW: { icon: Star, tone: "bg-gold-50 text-gold-600" },
  ANNOUNCEMENT: { icon: Megaphone, tone: "bg-navy-50 text-navy-600" },
  MESSAGE: { icon: MessageCircle, tone: "bg-teal-50 text-teal-600" },
  ORDER: { icon: Receipt, tone: "bg-sky-50 text-sky-600" },
  QUESTION: { icon: CircleQuestionMark, tone: "bg-pink-50 text-pink-600" },
  SAVED_SEARCH: { icon: Search, tone: "bg-navy-50 text-navy-600" },
  MODERATION: { icon: ShieldAlert, tone: "bg-red-50 text-danger" },
  POST: { icon: Newspaper, tone: "bg-teal-50 text-teal-600" },
};

const FALLBACK: NotificationMeta = { icon: Bell, tone: "bg-slate-100 text-slate-600" };

export function notificationMeta(category: string): NotificationMeta {
  return META[category] ?? FALLBACK;
}
