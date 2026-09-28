<script setup lang="ts">
import { ArrowLeft, BadgeCheck, MessageCircle, ShieldCheck } from "@lucide/vue";
import LogoMark from "@/components/LogoMark.vue";

defineProps<{ title: string; subtitle?: string }>();

const year = new Date().getFullYear();

const POINTS = [
  { icon: ShieldCheck, text: "Students-only - every account uses a university email" },
  { icon: BadgeCheck, text: "Businesses are verified by an admin before they go live" },
  { icon: MessageCircle, text: "Chat, order and book services without leaving res" },
];
</script>

<template>
  <div class="grid min-h-screen bg-white lg:grid-cols-2">
    <div class="flex flex-col px-5 py-6 sm:px-10">
      <div class="flex items-center justify-between gap-3">
        <RouterLink to="/" class="flex items-center gap-2">
          <LogoMark :size="30" />
          <span class="font-display text-lg font-bold text-uni-navy">Uni<span class="text-teal-500">Link</span>Hub</span>
        </RouterLink>
        <RouterLink to="/" class="btn-ghost px-3 text-xs">
          <ArrowLeft class="h-4 w-4" /> <span class="hidden sm:inline">Back to marketplace</span><span class="sm:hidden">Back</span>
        </RouterLink>
      </div>

      <div class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10 animate-fade-up">
        <h1 class="font-display text-3xl font-bold tracking-tight text-uni-navy">{{ title }}</h1>
        <p v-if="subtitle" class="mt-2 text-sm leading-relaxed text-medium-grey">{{ subtitle }}</p>
        <div class="mt-8"><slot /></div>
      </div>

      <p class="text-center text-xs text-medium-grey">&copy; {{ year }} UniLinkHub · Connect. Buy. Sell &amp; Succeed.</p>
    </div>

    <aside class="relative hidden overflow-hidden bg-hero p-12 text-white lg:flex lg:flex-col lg:justify-between">
      <div class="bg-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]"></div>
      <div class="absolute -right-24 top-1/3 h-80 w-80 rounded-full bg-teal-400/20 blur-3xl"></div>

      <p class="eyebrow relative text-sky-blue">The campus marketplace</p>

      <div class="relative">
        <h2 class="max-w-md font-display text-4xl font-bold leading-tight text-white">
          Your res is full of talent. <span class="text-gradient">Now it's all in one place.</span>
        </h2>
        <ul class="mt-10 space-y-4">
          <li v-for="p in POINTS" :key="p.text" class="flex items-center gap-3 text-sm text-white/80">
            <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/10 ring-1 ring-white/15">
              <component :is="p.icon" class="h-4 w-4 text-sky-blue" />
            </span>
            {{ p.text }}
          </li>
        </ul>
      </div>

      <figure class="relative rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur">
        <blockquote class="text-sm leading-relaxed text-white/85">
          “I used to post my printing prices in five different WhatsApp groups. Now students just find me - and I can see exactly
          what's selling.”
        </blockquote>
        <figcaption class="mt-4 flex items-center gap-3">
          <span class="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-gold-300 to-teal-400 text-xs font-bold text-navy-900">
            TP
          </span>
          <span class="text-xs">
            <span class="block font-semibold text-white">Thabo's Prints</span>
            <span class="text-white/60">Verified seller · Printing</span>
          </span>
        </figcaption>
      </figure>
    </aside>
  </div>
</template>
