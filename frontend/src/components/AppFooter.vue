<script setup lang="ts">
import LogoMark from "@/components/LogoMark.vue";
import { useCategories } from "@/lib/categories";

const categories = useCategories();
const year = new Date().getFullYear();
</script>

<template>
  <footer class="mt-16 border-t border-light-grey bg-white">
    <div class="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
      <div>
        <RouterLink to="/" class="flex items-center gap-2">
          <LogoMark :size="30" />
          <span class="font-display text-lg font-bold text-uni-navy">Uni<span class="text-teal-500">Link</span>Hub</span>
        </RouterLink>
        <p class="mt-3 max-w-xs text-sm leading-relaxed text-medium-grey">
          The trusted marketplace for student entrepreneurs in university residences. Connect. Buy. Sell &amp; Succeed.
        </p>
      </div>

      <div>
        <p class="footer-heading">Marketplace</p>
        <ul class="space-y-2">
          <li><RouterLink to="/" class="footer-link">Explore listings</RouterLink></li>
          <li><RouterLink to="/providers" class="footer-link">Provider directory</RouterLink></li>
          <li><RouterLink to="/compare" class="footer-link">Compare listings</RouterLink></li>
          <li><RouterLink to="/recently-viewed" class="footer-link">Recently viewed</RouterLink></li>
        </ul>
      </div>

      <div>
        <p class="footer-heading">Categories</p>
        <ul class="space-y-2">
          <li v-for="c in categories.slice(0, 5)" :key="c">
            <RouterLink :to="{ path: '/', query: { category: c } }" class="footer-link">{{ c }}</RouterLink>
          </li>
        </ul>
      </div>

      <div>
        <p class="footer-heading">Sell on UniLinkHub</p>
        <ul class="space-y-2">
          <li><RouterLink to="/register" class="footer-link">Create an account</RouterLink></li>
          <li><RouterLink to="/dashboard" class="footer-link">Seller dashboard</RouterLink></li>
          <li><RouterLink to="/promo-codes" class="footer-link">Promo codes</RouterLink></li>
        </ul>
      </div>
    </div>
    <div class="border-t border-light-grey">
      <div class="mx-auto flex max-w-7xl flex-col items-center justify-between gap-2 px-4 py-5 text-xs text-medium-grey sm:flex-row sm:px-6">
        <p>&copy; {{ year }} UniLinkHub. Built by students, for students.</p>
        <p>Payments and pickup are arranged directly between buyer and seller.</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer-heading {
  @apply mb-3 text-xs font-semibold uppercase tracking-wider text-uni-navy;
}
.footer-link {
  @apply text-sm text-medium-grey transition hover:text-teal-600;
}
</style>
