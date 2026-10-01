<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ArrowDownRight, ArrowUpRight, ChartColumn, Minus, Table2 } from "@lucide/vue";
import { api } from "@/lib/api";
import { formatPrice, formatRand } from "@/lib/format";
import type { BusinessDTO, SellerAnalyticsDTO } from "@/lib/types";

const props = defineProps<{ businesses: BusinessDTO[] }>();

// Single-series chart: the brand teal, validated for lightness/chroma/contrast on white.
const SERIES = "#2A9BB4";
const GRID = "#E3E8EF";
const RANGES = [7, 30, 90] as const;

const days = ref<(typeof RANGES)[number]>(30);
const businessId = ref("");
const data = ref<SellerAnalyticsDTO | null>(null);
const loading = ref(true);
const failed = ref(false);
const showTable = ref(false);

async function load() {
  loading.value = true;
  failed.value = false;
  try {
    const { data: result } = await api.get<SellerAnalyticsDTO>("/orders/seller/analytics", {
      params: { days: days.value, businessId: businessId.value || undefined },
    });
    data.value = result;
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
}

watch([days, businessId], load);
onMounted(load);

// ---- KPI tiles ----
function delta(current: number, previous: number) {
  if (previous === 0) return current > 0 ? { dir: "up" as const, text: "New" } : { dir: "flat" as const, text: "No change" };
  const pct = ((current - previous) / previous) * 100;
  if (Math.abs(pct) < 0.5) return { dir: "flat" as const, text: "0%" };
  return { dir: pct > 0 ? ("up" as const) : ("down" as const), text: `${pct > 0 ? "+" : ""}${pct.toFixed(0)}%` };
}

const tiles = computed(() => {
  const d = data.value;
  if (!d) return [];
  return [
    { label: "Revenue", value: formatRand(d.revenue), delta: delta(d.revenue, d.previousRevenue) },
    { label: "Orders", value: d.orders.toLocaleString("en-ZA"), delta: delta(d.orders, d.previousOrders) },
    { label: "Average order", value: formatPrice(d.averageOrderValue), delta: null },
    {
      label: "Buyers",
      value: d.uniqueBuyers.toLocaleString("en-ZA"),
      note: d.repeatBuyers > 0 ? `${d.repeatBuyers} came back for more` : d.cancelledOrders > 0 ? `${d.cancelledOrders} cancelled order${d.cancelledOrders === 1 ? "" : "s"}` : "",
      delta: null,
    },
  ];
});

// ---- Column chart (hand-rolled SVG: one series, no charting library needed) ----
const chartEl = ref<HTMLElement | null>(null);
const width = ref(600);
const HEIGHT = 220;
const M = { top: 12, right: 8, bottom: 26, left: 52 };
let observer: ResizeObserver | undefined;

// The chart element is recreated when the error state clears, so observe whichever one is current.
watch(chartEl, (el, previous) => {
  observer ??= new ResizeObserver(([entry]) => (width.value = Math.max(280, entry.contentRect.width)));
  if (previous) observer.unobserve(previous);
  if (el) observer.observe(el);
});
onBeforeUnmount(() => observer?.disconnect());

const hasSales = computed(() => (data.value?.daily ?? []).some((p) => p.revenue > 0));

function niceStep(max: number, ticks: number) {
  const raw = max / ticks;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const norm = raw / mag;
  return (norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10) * mag;
}

const chart = computed(() => {
  const points = data.value?.daily ?? [];
  const innerW = width.value - M.left - M.right;
  const innerH = HEIGHT - M.top - M.bottom;
  const peak = Math.max(0, ...points.map((p) => p.revenue));
  const step = niceStep(peak || 100, 4);
  const yMax = Math.max(step, Math.ceil(peak / step) * step);
  const slot = innerW / Math.max(points.length, 1);
  // Bars are capped at 24px and always leave at least a 2px surface gap between neighbours.
  const barW = Math.max(1, Math.min(24, slot * 0.7, slot - 2));
  const y = (v: number) => M.top + innerH - (v / yMax) * innerH;

  const bars = points.map((p, i) => {
    const x = M.left + i * slot + (slot - barW) / 2;
    const h = (p.revenue / yMax) * innerH;
    const r = Math.min(4, barW / 2, h);
    const top = M.top + innerH - h;
    const base = M.top + innerH;
    // Rounded data-end, square at the baseline.
    const path =
      h <= 0
        ? ""
        : `M${x},${base} V${top + r} Q${x},${top} ${x + r},${top} H${x + barW - r} Q${x + barW},${top} ${x + barW},${top + r} V${base} Z`;
    return { ...p, i, path, slotX: M.left + i * slot, cx: x + barW / 2, top };
  });

  const yTicks = Array.from({ length: Math.round(yMax / step) + 1 }, (_, k) => ({ value: k * step, y: y(k * step) }));
  const labelEvery = Math.ceil(points.length / Math.max(2, Math.floor(innerW / 64)));
  const xLabels = bars.filter((b) => b.i % labelEvery === 0).map((b) => ({ x: b.cx, text: shortDate(b.date) }));

  return { bars, yTicks, xLabels, slot, baseY: M.top + innerH };
});

function shortDate(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("en-ZA", { day: "numeric", month: "short" });
}
function longDate(iso: string) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString("en-ZA", { weekday: "short", day: "numeric", month: "long" });
}

const hovered = ref<number | null>(null);
const hoveredBar = computed(() => (hovered.value === null ? null : chart.value.bars[hovered.value]));
const tooltipLeft = computed(() => {
  if (!hoveredBar.value) return 0;
  // Keep the 168px tooltip inside the chart.
  return Math.min(Math.max(hoveredBar.value.cx - 84, 0), width.value - 168);
});

const topMax = computed(() => Math.max(1, ...(data.value?.topListings ?? []).map((t) => t.unitsSold)));
const salesDays = computed(() => (data.value?.daily ?? []).filter((p) => p.orders > 0).slice().reverse());
</script>

<template>
  <div class="card space-y-6 p-5 sm:p-6">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="section-title">Sales performance</h2>
        <p class="text-xs text-medium-grey">Cancelled orders are excluded. Compared with the previous {{ days }} days.</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <select v-if="props.businesses.length > 1" v-model="businessId" class="input-field w-auto py-2 text-xs" aria-label="Business">
          <option value="">All businesses</option>
          <option v-for="b in props.businesses" :key="b.id" :value="b.id">{{ b.businessName }}</option>
        </select>
        <div class="grid grid-cols-3 gap-1 rounded-control bg-soft-grey p-1" role="group" aria-label="Date range">
          <button
            v-for="r in RANGES"
            :key="r"
            class="rounded-lg px-3 py-1.5 text-xs font-semibold transition"
            :class="days === r ? 'bg-white text-uni-navy shadow-card' : 'text-medium-grey hover:text-uni-navy'"
            :aria-pressed="days === r"
            @click="days = r"
          >
            {{ r }}d
          </button>
        </div>
      </div>
    </div>

    <p v-if="failed" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">
      Couldn't load your sales figures. <button class="font-semibold underline" @click="load">Try again</button>
    </p>

    <template v-else>
      <!-- KPI tiles -->
      <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <template v-if="loading && !data">
          <div v-for="n in 4" :key="n" class="skeleton h-[92px] rounded-card"></div>
        </template>
        <div v-for="t in tiles" v-else :key="t.label" class="rounded-card border border-light-grey/80 bg-soft-grey/50 p-4" :class="{ 'opacity-60': loading }">
          <p class="text-xs font-medium text-medium-grey">{{ t.label }}</p>
          <p class="mt-1 text-xl font-semibold tabular-nums tracking-tight text-charcoal xs:text-2xl">{{ t.value }}</p>
          <p
            v-if="t.delta"
            class="mt-1 inline-flex items-center gap-0.5 text-xs font-semibold"
            :class="t.delta.dir === 'up' ? 'text-success' : t.delta.dir === 'down' ? 'text-danger' : 'text-medium-grey'"
          >
            <ArrowUpRight v-if="t.delta.dir === 'up'" class="h-3.5 w-3.5" />
            <ArrowDownRight v-else-if="t.delta.dir === 'down'" class="h-3.5 w-3.5" />
            <Minus v-else class="h-3.5 w-3.5" />
            {{ t.delta.text }} <span class="font-normal text-medium-grey">vs prev.</span>
          </p>
          <p v-else-if="t.note" class="mt-1 text-xs text-medium-grey">{{ t.note }}</p>
        </div>
      </div>

      <!-- Daily revenue -->
      <div>
        <div class="mb-2 flex items-center justify-between">
          <p class="text-sm font-semibold text-charcoal">Daily revenue</p>
          <button class="btn-ghost px-2 py-1 text-xs" :aria-pressed="showTable" @click="showTable = !showTable">
            <component :is="showTable ? ChartColumn : Table2" class="h-3.5 w-3.5" /> {{ showTable ? "Chart" : "Table" }}
          </button>
        </div>

        <div v-show="!showTable" ref="chartEl" class="relative" @mouseleave="hovered = null">
          <svg :width="width" :height="HEIGHT" class="block" role="img" :aria-label="`Daily revenue over the last ${days} days`">
            <g>
              <line v-for="t in chart.yTicks" :key="t.value" :x1="M.left" :x2="width - M.right" :y1="t.y" :y2="t.y" :stroke="GRID" stroke-width="1" />
              <text v-for="t in chart.yTicks" :key="`l${t.value}`" :x="M.left - 8" :y="t.y" dy="0.32em" text-anchor="end" class="fill-medium-grey text-[10px]">
                {{ formatRand(t.value) }}
              </text>
            </g>
            <text v-for="l in chart.xLabels" :key="l.x" :x="l.x" :y="HEIGHT - 6" text-anchor="middle" class="fill-medium-grey text-[10px]">{{ l.text }}</text>

            <line
              v-if="hoveredBar"
              :x1="hoveredBar.cx"
              :x2="hoveredBar.cx"
              :y1="M.top"
              :y2="chart.baseY"
              stroke="#CBD5E1"
              stroke-width="1"
            />
            <path
              v-for="b in chart.bars"
              :key="b.date"
              :d="b.path"
              :fill="SERIES"
              :opacity="hovered === null || hovered === b.i ? 1 : 0.45"
              class="transition-opacity"
            />
            <!-- Hit targets span the whole slot, wider than the thin bars. -->
            <rect
              v-for="b in chart.bars"
              :key="`hit${b.date}`"
              :x="b.slotX"
              :y="M.top"
              :width="chart.slot"
              :height="chart.baseY - M.top"
              fill="transparent"
              @mouseenter="hovered = b.i"
            />
          </svg>

          <div
            v-if="hoveredBar"
            class="pointer-events-none absolute top-0 w-[168px] rounded-control border border-light-grey bg-white px-3 py-2 shadow-pop"
            :style="{ left: `${tooltipLeft}px` }"
          >
            <p class="text-[11px] text-medium-grey">{{ longDate(hoveredBar.date) }}</p>
            <p class="mt-0.5 flex items-center gap-1.5 text-sm font-semibold text-charcoal">
              <span class="h-2 w-2 rounded-sm" :style="{ background: SERIES }"></span>{{ formatPrice(hoveredBar.revenue) }}
            </p>
            <p class="text-[11px] text-medium-grey">{{ hoveredBar.orders }} order{{ hoveredBar.orders === 1 ? "" : "s" }}</p>
          </div>

          <div v-if="!loading && !hasSales" class="absolute inset-0 flex items-center justify-center">
            <p class="rounded-full bg-white px-4 py-2 text-sm text-medium-grey shadow-card ring-1 ring-light-grey">
              No sales in the last {{ days }} days yet
            </p>
          </div>
        </div>

        <div v-if="showTable" class="max-h-[220px] overflow-y-auto rounded-control border border-light-grey">
          <table class="w-full text-sm">
            <thead class="sticky top-0 bg-soft-grey text-left text-xs text-medium-grey">
              <tr>
                <th class="px-3 py-2 font-semibold">Date</th>
                <th class="px-3 py-2 text-right font-semibold">Orders</th>
                <th class="px-3 py-2 text-right font-semibold">Revenue</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-light-grey">
              <tr v-if="salesDays.length === 0">
                <td colspan="3" class="px-3 py-4 text-center text-medium-grey">No sales in this period.</td>
              </tr>
              <tr v-for="p in salesDays" :key="p.date">
                <td class="px-3 py-2 text-charcoal">{{ longDate(p.date) }}</td>
                <td class="px-3 py-2 text-right tabular-nums text-charcoal">{{ p.orders }}</td>
                <td class="px-3 py-2 text-right tabular-nums text-charcoal">{{ formatPrice(p.revenue) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Top listings -->
      <div v-if="data && data.topListings.length > 0">
        <p class="mb-3 text-sm font-semibold text-charcoal">Best sellers <span class="font-normal text-medium-grey">· units sold</span></p>
        <ul class="space-y-3">
          <li v-for="t in data.topListings" :key="t.listingId">
            <RouterLink :to="`/listings/${t.listingId}`" class="group block">
              <div class="mb-1 flex items-baseline justify-between gap-3 text-sm">
                <span class="truncate font-medium text-charcoal group-hover:text-teal-700">{{ t.name }}</span>
                <span class="shrink-0 tabular-nums text-medium-grey">
                  <span class="font-semibold text-charcoal">{{ t.unitsSold }}</span> · {{ formatPrice(t.revenue) }}
                </span>
              </div>
              <div class="h-2 overflow-hidden rounded-full bg-teal-50">
                <div class="h-full rounded-full transition-all duration-500" :style="{ width: `${(t.unitsSold / topMax) * 100}%`, background: SERIES }"></div>
              </div>
            </RouterLink>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>
