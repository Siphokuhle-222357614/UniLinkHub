import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useMessagesStore } from "@/stores/messages";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    if (saved) return saved;
    if (to.hash) return { el: to.hash, behavior: "smooth" };
    // Filter changes on the same page (e.g. /?category=Food) shouldn't jump back to the top.
    if (to.path === from.path) return false;
    return { top: 0 };
  },
  routes: [
    { path: "/", name: "browse", component: () => import("@/views/BrowseView.vue") },
    { path: "/listings/:id", name: "listing-detail", component: () => import("@/views/ListingDetailView.vue") },
    { path: "/providers", name: "provider-directory", component: () => import("@/views/ProviderDirectoryView.vue") },
    { path: "/providers/:businessId", name: "provider-profile", component: () => import("@/views/ProviderProfileView.vue") },
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue"), meta: { layout: "bare" } },
    { path: "/register", name: "register", component: () => import("@/views/RegisterView.vue"), meta: { layout: "bare" } },
    { path: "/forgot-password", name: "forgot-password", component: () => import("@/views/ForgotPasswordView.vue"), meta: { layout: "bare" } },
    { path: "/verify-email", name: "verify-email", component: () => import("@/views/VerifyEmailView.vue"), meta: { layout: "bare" } },
    {
      path: "/confirm-email-change",
      name: "confirm-email-change",
      component: () => import("@/views/ConfirmEmailChangeView.vue"),
      meta: { layout: "bare" },
    },
    { path: "/marketplace-rules", name: "marketplace-rules", component: () => import("@/views/MarketplaceRulesView.vue") },
    {
      path: "/dashboard",
      name: "dashboard",
      component: () => import("@/views/DashboardView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/account",
      name: "account-settings",
      component: () => import("@/views/AccountSettingsView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/my-listings",
      name: "my-listings",
      component: () => import("@/views/MyListingsView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/recently-viewed",
      name: "recently-viewed",
      component: () => import("@/views/RecentlyViewedView.vue"),
    },
    {
      path: "/compare",
      name: "compare-listings",
      component: () => import("@/views/CompareListingsView.vue"),
    },
    {
      path: "/notifications",
      name: "notifications",
      component: () => import("@/views/NotificationsView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/bookings",
      name: "my-bookings",
      component: () => import("@/views/MyBookingsView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/listings/:id/insights",
      name: "listing-insights",
      component: () => import("@/views/ListingInsightsView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/messages",
      name: "messages",
      component: () => import("@/views/MessagesView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/messages/:id",
      name: "conversation",
      component: () => import("@/views/ConversationView.vue"),
      meta: { requiresAuth: true, studentOnly: true, hideFooter: true },
    },
    {
      path: "/checkout",
      name: "checkout",
      component: () => import("@/views/CheckoutView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/orders",
      name: "my-orders",
      component: () => import("@/views/MyOrdersView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/orders/selling",
      name: "seller-orders",
      component: () => import("@/views/SellerOrdersView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/questions",
      name: "seller-questions",
      component: () => import("@/views/SellerQuestionsView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/promo-codes",
      name: "promo-codes",
      component: () => import("@/views/PromoCodesView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/saved-searches",
      name: "saved-searches",
      component: () => import("@/views/SavedSearchesView.vue"),
      meta: { requiresAuth: true, studentOnly: true },
    },
    {
      path: "/admin",
      name: "admin-dashboard",
      component: () => import("@/views/admin/AdminDashboardView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true, layout: "bare" },
    },
   // { path: "/:pathMatch(.*)*", name: "not-found", component: () => import("@/views/NotFoundView.vue") },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!auth.initialized) {
    await auth.fetchCurrentUser();
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: "browse" };
  }
  // Admin accounts run the marketplace rather than take part in it, so buyer/seller pages send
  // them to their console instead.
  if (to.meta.studentOnly && auth.isAdmin) {
    return { name: "admin-dashboard" };
  }

  const saved = useSavedListingsStore();
  if (auth.isAuthenticated && !auth.isAdmin && !saved.initialized) {
    saved.fetchSaved().catch(() => {});
  }

  const followed = useFollowedProvidersStore();
  if (auth.isAuthenticated && !auth.isAdmin && !followed.initialized) {
    followed.fetchFollowed().catch(() => {});
  }

  const messages = useMessagesStore();
  if (auth.isAuthenticated && !auth.isAdmin && !messages.initialized) {
    messages.fetchUnreadCount();
    messages.initialized = true;
  }

  return true;
});

export default router;
