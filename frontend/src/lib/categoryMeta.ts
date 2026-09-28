import type { Component } from "vue";
import { CalendarDays, GraduationCap, Laptop, Package, PenTool, Printer, Scissors, UtensilsCrossed } from "@lucide/vue";

export interface CategoryMeta {
  icon: Component;
  /** Tailwind classes for the small icon tile (background + icon colour). */
  tile: string;
  /** CSS gradient used for image-less listing placeholders. */
  gradient: string;
}

// Mirrors the fixed taxonomy served by GET /api/categories (CategoryController). Anything the
// backend adds later falls back to FALLBACK rather than breaking.
const META: Record<string, CategoryMeta> = {
  Printing: { icon: Printer, tile: "bg-navy-50 text-navy-600", gradient: "linear-gradient(135deg,#D9E4F3,#EEF3FA)" },
  Tutoring: { icon: GraduationCap, tile: "bg-gold-50 text-gold-700", gradient: "linear-gradient(135deg,#F3EAD6,#FAF6EE)" },
  Food: { icon: UtensilsCrossed, tile: "bg-orange-50 text-orange-600", gradient: "linear-gradient(135deg,#FFE7D1,#FFF5EC)" },
  Design: { icon: PenTool, tile: "bg-violet-50 text-violet-600", gradient: "linear-gradient(135deg,#E9E1FB,#F6F2FE)" },
  "Hair & Beauty": { icon: Scissors, tile: "bg-pink-50 text-pink-600", gradient: "linear-gradient(135deg,#FBDDEA,#FEF1F6)" },
  Tech: { icon: Laptop, tile: "bg-teal-50 text-teal-600", gradient: "linear-gradient(135deg,#D2EEF5,#EDF8FB)" },
  Events: { icon: CalendarDays, tile: "bg-emerald-50 text-emerald-600", gradient: "linear-gradient(135deg,#D3F3E4,#EEFBF4)" },
};

const FALLBACK: CategoryMeta = {
  icon: Package,
  tile: "bg-slate-100 text-slate-600",
  gradient: "linear-gradient(135deg,#E3E8EF,#F4F6FA)",
};

export function categoryMeta(category: string | null | undefined): CategoryMeta {
  return (category && META[category]) || FALLBACK;
}
