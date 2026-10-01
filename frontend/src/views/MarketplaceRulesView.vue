<script setup lang="ts">
import { Ban, CircleCheck, Flag, Gavel, HeartHandshake, ShieldAlert } from "@lucide/vue";
import { CONSEQUENCES, useRestrictedCategories } from "@/lib/rules";

const categories = useRestrictedCategories();

const WELCOME = [
  "Food you've made yourself (cakes, lunches, snacks)",
  "Printing, design and photography",
  "Tutoring and study help (not doing the work for someone)",
  "Hair, nails and beauty services",
  "Second-hand textbooks, tech and clothing that's yours to sell",
  "Event tickets and services",
];
</script>

<template>
  <section class="mx-auto max-w-4xl space-y-10">
    <div class="relative -mx-4 -mt-6 overflow-hidden bg-hero px-5 py-10 text-white sm:mx-0 sm:mt-0 sm:rounded-[24px] sm:px-10 sm:py-12">
      <div class="bg-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_right,black,transparent_70%)]"></div>
      <div class="relative max-w-2xl">
        <p class="eyebrow text-sky-blue">Keeping UniLinkHub safe</p>
        <h1 class="mt-2 font-display text-3xl font-bold tracking-tight text-white sm:text-4xl">Marketplace rules</h1>
        <p class="mt-3 text-sm leading-relaxed text-white/75 sm:text-base">
          UniLinkHub is for legal, honest student businesses in CPUT residences. These rules apply to every listing, every business
          and every student who sells here.
        </p>
      </div>
    </div>

    <div>
      <h2 class="section-title flex items-center gap-2"><ShieldAlert class="h-5 w-5 text-danger" /> What can't be sold</h2>
      <p class="mt-1 text-sm text-medium-grey">Listings that include these are blocked automatically - and anything that slips through is removed by an admin.</p>
      <div class="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div v-for="c in categories" :key="c.key" class="card flex gap-4">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-50 text-danger"><Ban class="h-5 w-5" /></span>
          <div>
            <h3 class="font-display text-base font-semibold text-uni-navy">{{ c.label }}</h3>
            <p class="mt-1 text-sm leading-relaxed text-charcoal">{{ c.description }}</p>
            <p class="mt-2 text-xs text-medium-grey">For example: {{ c.examples.join(", ") }}</p>
          </div>
        </div>
        <template v-if="categories.length === 0">
          <div v-for="n in 4" :key="n" class="skeleton h-28 rounded-card"></div>
        </template>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <div class="card border-amber-200 bg-amber-50/60">
        <h2 class="section-title flex items-center gap-2 text-amber-900"><Gavel class="h-5 w-5" /> If someone breaks the rules</h2>
        <ul class="mt-3 space-y-2.5">
          <li v-for="(c, i) in CONSEQUENCES" :key="c" class="flex gap-3 text-sm leading-relaxed text-amber-900">
            <span class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-amber-100 text-xs font-bold">{{ i + 1 }}</span>
            {{ c }}
          </li>
        </ul>
        <p class="mt-4 text-xs leading-relaxed text-amber-800">
          Every seller agrees to these rules before they can list anything, and we keep a record of when they agreed.
        </p>
      </div>

      <div class="card">
        <h2 class="section-title flex items-center gap-2"><Flag class="h-5 w-5 text-teal-600" /> Seen something that breaks the rules?</h2>
        <ol class="mt-3 space-y-2.5 text-sm leading-relaxed text-charcoal">
          <li class="flex gap-3"><span class="step">1</span> <span>Open the listing or the seller's profile.</span></li>
          <li class="flex gap-3"><span class="step">2</span> <span>Choose <span class="font-semibold">Report</span>, then <span class="font-semibold">"Restricted or illegal item"</span>.</span></li>
          <li class="flex gap-3"><span class="step">3</span> <span>An admin reviews it. Reports are private - the seller isn't told who reported them.</span></li>
        </ol>
        <p class="mt-4 text-xs text-medium-grey">If anyone is in danger, contact campus security or the police straight away.</p>
      </div>
    </div>

    <div class="card">
      <h2 class="section-title flex items-center gap-2"><HeartHandshake class="h-5 w-5 text-emerald-600" /> What's welcome</h2>
      <ul class="mt-4 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
        <li v-for="w in WELCOME" :key="w" class="flex gap-2.5 text-sm text-charcoal">
          <CircleCheck class="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" /> {{ w }}
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.step {
  @apply flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-teal-50 text-xs font-bold text-teal-700;
}
</style>
