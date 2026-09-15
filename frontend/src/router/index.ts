import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useMessagesStore } from "@/stores/messages";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "browse", component: () => import("@/views/BrowseView.vue") },
    { path: "/listings/:id", name: "listing-detail", component: () => import("@/views/ListingDetailView.vue") },
    { path: "/providers", name: "provider-directory", component: () => import("@/views/ProviderDirectoryView.vue") },
    { path: "/providers/:businessId", name: "provider-profile", component: () => import("@/views/ProviderProfileView.vue") },
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue") },
    { path: "/register", name: "register", component: () => import("@/views/RegisterView.vue") },
    { path: "/forgot-password", name: "forgot-password", component: () => import("@/views/ForgotPasswordView.vue") },
    {
      path: "/dashboard",
      name: "dashboard",
      component: () => import("@/views/DashboardView.vue"),
      meta: { requiresAuth: true },
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
      meta: { requiresAuth: true },
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
      meta: { requiresAuth: true },
    },
    {
      path: "/listings/:id/insights",
      name: "listing-insights",
      component: () => import("@/views/ListingInsightsView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/messages",
      name: "messages",
      component: () => import("@/views/MessagesView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/messages/:id",
      name: "conversation",
      component: () => import("@/views/ConversationView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/checkout",
      name: "checkout",
      component: () => import("@/views/CheckoutView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/orders",
      name: "my-orders",
      component: () => import("@/views/MyOrdersView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/orders/selling",
      name: "seller-orders",
      component: () => import("@/views/SellerOrdersView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/promo-codes",
      name: "promo-codes",
      component: () => import("@/views/PromoCodesView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/saved-searches",
      name: "saved-searches",
      component: () => import("@/views/SavedSearchesView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/admin",
      name: "admin-dashboard",
      component: () => import("@/views/admin/AdminDashboardView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    { path: "/:pathMatch(.*)*", name: "not-found", component: () => import("@/views/NotFoundView.vue") },
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

  const saved = useSavedListingsStore();
  if (auth.isAuthenticated && !saved.initialized) {
    saved.fetchSaved().catch(() => {});
  }

  const followed = useFollowedProvidersStore();
  if (auth.isAuthenticated && !followed.initialized) {
    followed.fetchFollowed().catch(() => {});
  }

  const messages = useMessagesStore();
  if (auth.isAuthenticated && !messages.initialized) {
    messages.fetchUnreadCount();
    messages.initialized = true;
  }

  return true;
});

export default router;
