<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useToastStore } from "@/stores/toast";
import { getRecentlyViewed } from "@/lib/recentlyViewed";
import { api, extractErrorMessage } from "@/lib/api";
import { useCategories } from "@/lib/categories";
import {
  ArrowRight,
  BadgeCheck,
  Bookmark,
  CalendarDays,
  Check,
  CircleQuestionMark,
  Flag,
  Heart,
  History,
  MessageCircle,
  Package,
  Pencil,
  Plus,
  Receipt,
  Sparkles,
  Store,
  TicketPercent,
  Users,
} from "@lucide/vue";
import ListingCard from "@/components/ListingCard.vue";
import MultiImageUpload from "@/components/ui/MultiImageUpload.vue";
import { useCampuses } from "@/lib/campuses";
import FollowingFeed from "@/components/posts/FollowingFeed.vue";
import RestrictedItemsNotice from "@/components/RestrictedItemsNotice.vue";
import SellerRulesModal from "@/components/SellerRulesModal.vue";
import SellerChecklist from "@/components/SellerChecklist.vue";
import { REASON_LABELS } from "@/lib/reports";
import SalesAnalytics from "@/components/SalesAnalytics.vue";
import EmptyState from "@/components/ui/EmptyState.vue";
import { useMessagesStore } from "@/stores/messages";
import { categoryMeta } from "@/lib/categoryMeta";
import type { BookingSummaryView, BusinessDTO, BusinessStatsDTO, ListingDTO, QuestionView, ReportStatus, ReportSummaryView } from "@/lib/types";

const auth = useAuthStore();
const saved = useSavedListingsStore();
const followed = useFollowedProvidersStore();
const toast = useToastStore();
const categories = useCategories();
const messagesStore = useMessagesStore();

const tab = ref<"buying" | "selling">(auth.isSeller ? "selling" : "buying");

const buyerActions = [
  { to: "/orders", label: "My orders", icon: Receipt, tone: "bg-sky-50 text-sky-600" },
  { to: "/bookings", label: "Bookings", icon: CalendarDays, tone: "bg-violet-50 text-violet-600" },
  { to: "/messages", label: "Messages", icon: MessageCircle, tone: "bg-teal-50 text-teal-600" },
  { to: "/saved-searches", label: "Saved searches", icon: Bookmark, tone: "bg-gold-50 text-gold-700" },
];
const sellerActions = [
  { to: "/orders/selling", label: "Orders to fulfil", icon: Package, tone: "bg-orange-50 text-orange-600" },
  { to: "/my-listings", label: "My listings", icon: Store, tone: "bg-navy-50 text-navy-600" },
  { to: "/questions", label: "Listing Q&A", icon: CircleQuestionMark, tone: "bg-pink-50 text-pink-600" },
  { to: "/promo-codes", label: "Promo codes", icon: TicketPercent, tone: "bg-emerald-50 text-emerald-600" },
];

const VERIFICATION_STYLES: Record<string, string> = {
  VERIFIED: "bg-emerald-50 text-emerald-700",
  PENDING: "bg-amber-50 text-warning",
  REJECTED: "bg-red-50 text-danger",
};

const businesses = ref<BusinessDTO[]>([]);
const listingsByBusiness = ref<Record<string, ListingDTO[]>>({});
const error = ref("");
const becomingSeller = ref(false);

const newBusiness = ref({ businessName: "", description: "", category: "", campus: "", pickupLocation: "" });
const campuses = useCampuses();
const creatingBusiness = ref(false);

const newListing = ref({
  businessId: "",
  kind: "PRODUCT" as "PRODUCT" | "SERVICE",
  name: "",
  description: "",
  category: "",
  price: 0,
  stockQuantity: 1,
  imageUrls: [] as string[],
  durationMinutes: 30,
  availabilitySchedule: "",
});
const creatingListing = ref(false);

// ---- Buyer: recently viewed ----
const recentlyViewed = ref<ListingDTO[]>([]);

// ---- Buyer: bulk unsave ----
const savedSelectMode = ref(false);
const savedSelected = ref(new Set<string>());
const bulkUnsaving = ref(false);

function toggleSavedSelected(id: string) {
  if (savedSelected.value.has(id)) {
    savedSelected.value.delete(id);
  } else {
    savedSelected.value.add(id);
  }
}

async function bulkUnsave() {
  bulkUnsaving.value = true;
  try {
    for (const listing of saved.listings.filter((l) => savedSelected.value.has(l.id))) {
      await saved.toggleSave(listing);
    }
    savedSelected.value.clear();
    savedSelectMode.value = false;
  } finally {
    bulkUnsaving.value = false;
  }
}

// ---- Buyer: recommended for you ----
const recommended = ref<ListingDTO[]>([]);
const favoriteCategories = ref<string[]>([]);

async function loadRecommended() {
  const categoriesOfInterest = new Set<string>();
  for (const l of saved.listings) categoriesOfInterest.add(l.category);
  for (const p of followed.providers) categoriesOfInterest.add(p.category);
  favoriteCategories.value = [...categoriesOfInterest];
  if (favoriteCategories.value.length === 0) return;

  try {
    const savedIds = new Set(saved.listings.map((l) => l.id));
    const results = await Promise.all(
      favoriteCategories.value.slice(0, 3).map((c) => api.get<ListingDTO[]>("/listings", { params: { category: c, sort: "views", limit: 8 } })),
    );
    const seen = new Set<string>();
    const combined: ListingDTO[] = [];
    for (const { data } of results) {
      for (const listing of data) {
        if (listing.status === "ACTIVE" && !savedIds.has(listing.id) && !seen.has(listing.id)) {
          seen.add(listing.id);
          combined.push(listing);
        }
      }
    }
    recommended.value = combined.slice(0, 6);
  } catch {
    // Recommendations are a nice-to-have; ignore failures here.
  }
}

// ---- Buyer: my reports ----
const myReports = ref<ReportSummaryView[]>([]);
const reportsLoading = ref(false);
const reportsError = ref("");


const STATUS_STYLES: Record<ReportStatus, string> = {
  OPEN: "bg-amber-50 text-warning",
  UNDER_REVIEW: "bg-blue-50 text-info",
  RESOLVED: "bg-emerald-50 text-emerald-700",
  DISMISSED: "bg-slate-100 text-medium-grey",
};

const STATUS_LABELS: Record<ReportStatus, string> = {
  OPEN: "Open",
  UNDER_REVIEW: "Under review",
  RESOLVED: "Resolved",
  DISMISSED: "Dismissed",
};

const LISTING_STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  SOLD_OUT: "Sold out",
};

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

async function loadReports() {
  reportsLoading.value = true;
  reportsError.value = "";
  try {
    const { data } = await api.get<ReportSummaryView[]>("/reports/mine");
    myReports.value = data;
  } catch (err) {
    reportsError.value = extractErrorMessage(err);
  } finally {
    reportsLoading.value = false;
  }
}

// ---- Seller ----
const statsByBusiness = ref<Record<string, BusinessStatsDTO>>({});

function scrollToBusiness(businessId: string) {
  const el = document.getElementById(`business-${businessId}`);
  if (el) {
    el.scrollIntoView({ behavior: "smooth", block: "start" });
    el.classList.add("ring-2", "ring-teal-500");
    setTimeout(() => el.classList.remove("ring-2", "ring-teal-500"), 1500);
  }
}

// ---- Seller: booking requests ----
const sellerBookings = ref<BookingSummaryView[]>([]);
const decliningBookingId = ref<string | null>(null);
const declineReason = ref("");
const bookingActing = ref<string | null>(null);

const pendingBookings = computed(() => sellerBookings.value.filter((b) => b.status === "PENDING"));

const BOOKING_STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-amber-50 text-warning",
  ACCEPTED: "bg-emerald-50 text-emerald-700",
  DECLINED: "bg-red-50 text-danger",
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-ZA", { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
}

async function loadSellerBookings() {
  if (!auth.isSeller) return;
  try {
    const { data } = await api.get<BookingSummaryView[]>("/bookings/seller");
    sellerBookings.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function acceptBooking(id: string) {
  bookingActing.value = id;
  try {
    await api.post(`/bookings/${id}/accept`);
    toast.success("Booking accepted!");
    await loadSellerBookings();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    bookingActing.value = null;
  }
}

function startDecline(id: string) {
  decliningBookingId.value = id;
  declineReason.value = "";
}

async function confirmDecline(id: string) {
  bookingActing.value = id;
  try {
    await api.post(`/bookings/${id}/decline`, { reason: declineReason.value });
    decliningBookingId.value = null;
    toast.info("Booking declined");
    await loadSellerBookings();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    bookingActing.value = null;
  }
}

// ---- Seller: pending questions ----
const pendingQuestions = ref<QuestionView[]>([]);
const answerDrafts = ref<Record<string, string>>({});
const answeringId = ref<string | null>(null);

async function loadPendingQuestions() {
  if (!auth.isSeller) return;
  try {
    const { data } = await api.get<QuestionView[]>("/questions/pending");
    pendingQuestions.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function answerQuestion(id: string) {
  const answerText = answerDrafts.value[id];
  if (!answerText?.trim()) return;
  answeringId.value = id;
  try {
    await api.post(`/questions/${id}/answer`, { answerText });
    delete answerDrafts.value[id];
    toast.success("Answer posted!");
    await loadPendingQuestions();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    answeringId.value = null;
  }
}

async function loadBusinesses() {
  if (!auth.isSeller) return;
  try {
    const { data } = await api.get<BusinessDTO[]>("/businesses/mine");
    businesses.value = data;
    for (const business of data) {
      const [{ data: listings }, { data: stats }] = await Promise.all([
        api.get<ListingDTO[]>(`/listings/business/${business.id}`),
        api.get<BusinessStatsDTO>(`/businesses/${business.id}/stats`),
      ]);
      listingsByBusiness.value[business.id] = listings;
      statsByBusiness.value[business.id] = stats;
    }
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

// Becoming a seller (or a seller from before the rules existed confirming them) goes through the
// marketplace rules modal - the backend refuses to unlock selling until they've been accepted.
const rulesOpen = ref(false);

function becomeSeller() {
  rulesOpen.value = true;
}

/** Checklist shortcuts: bring a form into view and put the cursor in its first field. */
function jumpTo(id: string) {
  const el = document.getElementById(id);
  if (!el) return;
  el.scrollIntoView({ behavior: "smooth", block: "start" });
  el.querySelector<HTMLElement>("input, select, textarea")?.focus({ preventScroll: true });
}

async function onRulesAccepted() {
  rulesOpen.value = false;
  tab.value = "selling";
  await loadBusinesses();
}

async function createBusiness() {
  creatingBusiness.value = true;
  error.value = "";
  try {
    await api.post("/businesses", newBusiness.value);
    toast.success("Business added!", "An admin will review it within a few days.");
    newBusiness.value = { businessName: "", description: "", category: "", campus: "", pickupLocation: "" };
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    creatingBusiness.value = false;
  }
}

async function createListing() {
  creatingListing.value = true;
  error.value = "";
  try {
    const payload = {
      businessId: newListing.value.businessId,
      name: newListing.value.name,
      description: newListing.value.description,
      category: newListing.value.category,
      price: newListing.value.price,
      imageUrls: newListing.value.imageUrls,
    };
    if (newListing.value.kind === "PRODUCT") {
      await api.post("/listings/products", {
        ...payload,
        stockQuantity: newListing.value.stockQuantity,
      });
    } else {
      await api.post("/listings/services", {
        ...payload,
        durationMinutes: newListing.value.durationMinutes,
        availabilitySchedule: newListing.value.availabilitySchedule || null,
      });
    }
    toast.success("Listing published!");
    newListing.value.imageUrls = [];
    newListing.value.name = "";
    newListing.value.description = "";
    newListing.value.category = "";
    newListing.value.price = 0;
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    creatingListing.value = false;
  }
}

async function deactivateListing(id: string) {
  try {
    await api.post(`/listings/${id}/deactivate`);
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function reactivateListing(id: string) {
  try {
    await api.post(`/listings/${id}/reactivate`);
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

// ---- Seller: edit listing ----
const editingListingId = ref<string | null>(null);
const editForm = ref({
  name: "",
  category: "",
  description: "",
  price: 0,
  status: "ACTIVE" as "ACTIVE" | "INACTIVE",
  stockQuantity: 0,
  imageUrls: [] as string[],
  lowStockThreshold: null as number | null,
  durationMinutes: 0,
  availabilitySchedule: "",
});
const savingEdit = ref(false);

function startEdit(listing: ListingDTO) {
  editingListingId.value = listing.id;
  editForm.value = {
    name: listing.name,
    category: listing.category,
    description: listing.description,
    price: listing.price,
    status: listing.status === "INACTIVE" ? "INACTIVE" : "ACTIVE",
    stockQuantity: listing.stockQuantity ?? 0,
    imageUrls: listing.imageUrls?.length ? [...listing.imageUrls] : listing.imageUrl ? [listing.imageUrl] : [],
    lowStockThreshold: listing.lowStockThreshold,
    durationMinutes: listing.durationMinutes ?? 0,
    availabilitySchedule: listing.availabilitySchedule ?? "",
  };
}

function cancelEdit() {
  editingListingId.value = null;
}

async function saveEdit(listing: ListingDTO) {
  savingEdit.value = true;
  error.value = "";
  try {
    await api.patch(`/listings/${listing.id}`, {
      name: editForm.value.name,
      category: editForm.value.category,
      description: editForm.value.description,
      price: editForm.value.price,
      status: editForm.value.status,
      stockQuantity: listing.type === "PRODUCT" ? editForm.value.stockQuantity : undefined,
      imageUrls: editForm.value.imageUrls,
      lowStockThreshold: listing.type === "PRODUCT" ? editForm.value.lowStockThreshold : undefined,
      durationMinutes: listing.type === "SERVICE" ? editForm.value.durationMinutes : undefined,
      availabilitySchedule: listing.type === "SERVICE" ? editForm.value.availabilitySchedule : undefined,
    });
    editingListingId.value = null;
    toast.success("Changes saved!");
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    savingEdit.value = false;
  }
}

onMounted(async () => {
  recentlyViewed.value = getRecentlyViewed();
  await Promise.all([loadBusinesses(), loadReports(), saved.fetchSaved(), followed.fetchFollowed(), loadSellerBookings(), loadPendingQuestions()]);
  await loadRecommended();
});
</script>

<template>
  <section class="space-y-8">
    <!-- Welcome header -->
    <div class="relative -mx-4 -mt-6 overflow-hidden bg-hero px-5 py-8 text-white sm:mx-0 sm:mt-0 sm:rounded-[24px] sm:px-8">
      <div class="bg-grid pointer-events-none absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_right,black,transparent_70%)]"></div>
      <div class="relative flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-4">
          <span class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-blue to-teal-500 font-display text-lg font-bold text-navy-950 shadow-lift">
            {{ (auth.user?.firstName?.[0] ?? "") + (auth.user?.lastName?.[0] ?? "") }}
          </span>
          <div class="min-w-0">
            <h1 class="font-display text-2xl font-bold text-white sm:text-3xl">Welcome, {{ auth.user?.firstName }}</h1>
            <p class="truncate text-sm text-white/65">{{ auth.user?.email }} · {{ auth.user?.studentNumber }}</p>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <span v-if="auth.isSeller" class="badge border border-white/15 bg-white/10 py-1 text-sky-blue"><Store class="h-3.5 w-3.5" /> Seller</span>
          <span class="badge border border-white/15 bg-white/10 py-1 text-white/80"><Heart class="h-3.5 w-3.5" /> {{ saved.listings.length }} saved</span>
          <span class="badge border border-white/15 bg-white/10 py-1 text-white/80"><Users class="h-3.5 w-3.5" /> {{ followed.providers.length }} following</span>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <RouterLink v-for="a in tab === 'selling' ? sellerActions : buyerActions" :key="a.to" :to="a.to" class="group card card-interactive flex items-center gap-3 p-4">
        <span class="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl" :class="a.tone">
          <component :is="a.icon" class="h-5 w-5" />
          <span v-if="a.to === '/messages' && messagesStore.unreadCount > 0" class="count-dot -right-1.5 -top-1.5">{{ messagesStore.unreadCount }}</span>
        </span>
        <span class="text-sm font-semibold text-uni-navy">{{ a.label }}</span>
      </RouterLink>
    </div>

    <!-- Tabs -->
    <div v-if="auth.isSeller" class="flex gap-1 rounded-control bg-white p-1 shadow-xs ring-1 ring-light-grey sm:w-fit" role="tablist">
      <button
        v-for="t in (['selling', 'buying'] as const)"
        :key="t"
        role="tab"
        :aria-selected="tab === t"
        class="flex-1 rounded-lg px-5 py-2 text-sm font-semibold transition sm:flex-none"
        :class="tab === t ? 'bg-uni-navy text-white shadow-card' : 'text-medium-grey hover:text-uni-navy'"
        @click="tab = t"
      >
        {{ t === "selling" ? "Selling" : "Buying" }}
        <span v-if="t === 'selling' && pendingBookings.length + pendingQuestions.length > 0" class="ml-1.5 rounded-full bg-warning px-1.5 text-[10px] text-white">
          {{ pendingBookings.length + pendingQuestions.length }}
        </span>
      </button>
    </div>

    <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>

    <!-- ================= BUYING ================= -->
    <div v-show="tab === 'buying'" class="space-y-10">
      <FollowingFeed />

      <!-- Saved listings -->
      <div>
        <div class="mb-4 flex items-center justify-between">
          <h2 class="section-title flex items-center gap-2">
            Saved listings <span v-if="saved.listings.length > 0" class="badge bg-navy-50 text-navy-600">{{ saved.listings.length }}</span>
          </h2>
          <button
            v-if="saved.listings.length > 0"
            class="btn-ghost px-3 py-1.5 text-xs"
            @click="savedSelectMode = !savedSelectMode; savedSelected.clear()"
          >
            {{ savedSelectMode ? "Cancel" : "Select" }}
          </button>
        </div>
        <EmptyState v-if="saved.listings.length === 0" :icon="Heart" title="Nothing saved yet" description="Tap the heart on any listing to keep it here for later.">
          <RouterLink to="/" class="btn-secondary">Explore listings</RouterLink>
        </EmptyState>
        <div v-else class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3 xl:grid-cols-4 3xl:grid-cols-5">
          <div v-for="listing in saved.listings" :key="listing.id" class="relative">
            <button
              v-if="savedSelectMode"
              class="absolute inset-0 z-10 rounded-card transition"
              :class="savedSelected.has(listing.id) ? 'bg-teal-500/10 ring-2 ring-teal-500' : 'hover:bg-navy-900/5'"
              :aria-pressed="savedSelected.has(listing.id)"
              :aria-label="`Select ${listing.name}`"
              @click="toggleSavedSelected(listing.id)"
            >
              <span
                class="absolute right-3 top-3 flex h-6 w-6 items-center justify-center rounded-full border-2"
                :class="savedSelected.has(listing.id) ? 'border-teal-500 bg-teal-500 text-white' : 'border-white bg-white/80'"
              >
                <Check v-if="savedSelected.has(listing.id)" class="h-3.5 w-3.5" />
              </span>
            </button>
            <ListingCard :listing="listing" />
          </div>
        </div>
        <div v-if="savedSelectMode && savedSelected.size > 0" class="sticky bottom-24 z-20 mt-4 flex items-center justify-between rounded-full bg-navy-900 py-2 pl-5 pr-2 shadow-pop md:bottom-6">
          <p class="text-sm font-semibold text-white">{{ savedSelected.size }} selected</p>
          <button class="rounded-full bg-white/10 px-4 py-2 text-sm font-semibold text-white hover:bg-white/20 disabled:opacity-50" :disabled="bulkUnsaving" @click="bulkUnsave">
            {{ bulkUnsaving ? "Removing…" : "Remove from saved" }}
          </button>
        </div>
      </div>

      <!-- Recommended -->
      <div v-if="recommended.length > 0">
        <h2 class="section-title flex items-center gap-2"><Sparkles class="h-5 w-5 text-gold-500" /> Recommended for you</h2>
        <p class="mb-4 mt-1 text-xs text-medium-grey">Based on what you've saved and followed: {{ favoriteCategories.join(", ") }}</p>
        <div class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3 xl:grid-cols-4 3xl:grid-cols-5">
          <ListingCard v-for="listing in recommended" :key="listing.id" :listing="listing" />
        </div>
      </div>

      <!-- Following -->
      <div>
        <h2 class="section-title mb-4 flex items-center gap-2">
          Providers you follow <span v-if="followed.providers.length > 0" class="badge bg-navy-50 text-navy-600">{{ followed.providers.length }}</span>
        </h2>
        <EmptyState v-if="followed.providers.length === 0" :icon="Users" title="You're not following anyone yet" description="Follow a provider from their profile to keep up with their new listings.">
          <RouterLink to="/providers" class="btn-secondary">Browse providers</RouterLink>
        </EmptyState>
        <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <RouterLink v-for="p in followed.providers" :key="p.businessId" :to="`/providers/${p.businessId}`" class="group card card-interactive flex items-center gap-3.5 p-4">
            <img v-if="p.imageUrl" :src="p.imageUrl" alt="" class="h-11 w-11 shrink-0 rounded-xl object-cover ring-1 ring-light-grey" />
            <span v-else class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl font-display font-bold" :class="categoryMeta(p.category).tile">{{ p.businessName.charAt(0) }}</span>
            <div class="min-w-0 flex-1">
              <p class="flex items-center gap-1.5 truncate font-display text-sm font-semibold text-uni-navy">
                {{ p.businessName }} <BadgeCheck v-if="p.verificationStatus === 'VERIFIED'" class="h-4 w-4 shrink-0 text-emerald-500" />
              </p>
              <p class="text-xs text-medium-grey">{{ p.category }} · {{ p.activeListingCount }} active listings</p>
            </div>
            <ArrowRight class="h-4 w-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-teal-600" />
          </RouterLink>
        </div>
      </div>

      <!-- Recently viewed -->
      <div v-if="recentlyViewed.length > 0">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="section-title flex items-center gap-2"><History class="h-5 w-5 text-medium-grey" /> Recently viewed</h2>
          <RouterLink to="/recently-viewed" class="link text-xs">View all</RouterLink>
        </div>
        <div class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3 xl:grid-cols-4 3xl:grid-cols-5">
          <ListingCard v-for="listing in recentlyViewed.slice(0, 4)" :key="listing.id" :listing="listing" />
        </div>
      </div>

      <!-- Reports -->
      <div>
        <h2 class="section-title mb-4 flex items-center gap-2"><Flag class="h-5 w-5 text-medium-grey" /> Your reports</h2>
        <p v-if="reportsError" class="text-sm text-danger">{{ reportsError }}</p>
        <div v-else-if="reportsLoading" class="skeleton h-16 rounded-card"></div>
        <p v-else-if="myReports.length === 0" class="rounded-card border border-dashed border-light-grey px-5 py-4 text-sm text-medium-grey">
          Reports you file on listings or providers will show up here with their status.
        </p>
        <ul v-else class="divide-y divide-light-grey overflow-hidden rounded-card border border-light-grey bg-white">
          <li v-for="r in myReports" :key="r.id" class="flex items-center justify-between gap-3 px-5 py-3.5">
            <div class="min-w-0">
              <p class="text-sm font-semibold text-charcoal">{{ REASON_LABELS[r.reason] ?? r.reason }}</p>
              <p class="truncate text-xs text-medium-grey">{{ r.target.label }} · Filed {{ relativeTime(r.createdAt) }}</p>
            </div>
            <span class="badge shrink-0" :class="STATUS_STYLES[r.status]">{{ STATUS_LABELS[r.status] }}</span>
          </li>
        </ul>
      </div>

      <!-- Become a seller -->
      <div v-if="!auth.isSeller" class="relative overflow-hidden rounded-[24px] bg-gradient-to-br from-gold-50 via-white to-teal-50 p-6 ring-1 ring-light-grey sm:p-8">
        <div class="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-teal-200/40 blur-3xl"></div>
        <div class="relative flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="eyebrow text-gold-700">Have something to offer?</p>
            <h2 class="mt-1 font-display text-xl font-bold text-uni-navy">Unlock seller tools on this same account</h2>
            <p class="mt-1 max-w-lg text-sm text-medium-grey">List products and services, take orders and bookings, run promo codes and track your sales - no separate sign-up.</p>
          </div>
          <button class="btn-primary shrink-0 px-6 py-3" :disabled="becomingSeller" @click="becomeSeller">
            <Store class="h-4 w-4" /> {{ becomingSeller ? "Unlocking…" : "Become a seller" }}
          </button>
        </div>
      </div>
    </div>

    <!-- ================= SELLING ================= -->
    <div v-if="auth.isSeller" v-show="tab === 'selling'" class="space-y-8">
      <div v-if="!auth.user?.sellerRulesAcceptedAt" class="flex flex-col gap-3 rounded-card border border-amber-200 bg-amber-50 p-5 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-sm text-amber-900">
          <span class="font-semibold">Please review the marketplace rules.</span> Every seller now agrees to them before adding a business -
          they cover what can't be sold and what happens if someone does.
        </p>
        <button class="btn-primary shrink-0" @click="becomeSeller">Review &amp; accept</button>
      </div>
      <SellerChecklist
        :businesses="businesses"
        :listings-by-business="listingsByBusiness"
        @accept-rules="becomeSeller"
        @add-business="jumpTo('add-business-form')"
        @add-listing="jumpTo('new-listing-form')"
      />
      <SalesAnalytics v-if="businesses.length > 0" :businesses="businesses" />

      <div v-if="sellerBookings.length > 0 || pendingQuestions.length > 0" class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <!-- Booking requests -->
        <div v-if="sellerBookings.length > 0" class="card space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="section-title">Booking requests</h2>
            <span v-if="pendingBookings.length > 0" class="badge bg-amber-50 text-warning">{{ pendingBookings.length }} pending</span>
          </div>
          <ul class="space-y-2.5">
            <li
              v-for="b in sellerBookings"
              :key="b.id"
              class="rounded-control border p-3.5"
              :class="b.status === 'PENDING' ? 'border-amber-200 bg-amber-50/40' : 'border-light-grey opacity-70'"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="truncate text-sm font-semibold text-uni-navy">{{ b.listingName }}</span>
                <span class="badge" :class="BOOKING_STATUS_STYLES[b.status]">{{ b.status.charAt(0) + b.status.slice(1).toLowerCase() }}</span>
              </div>
              <p class="mt-0.5 text-xs text-medium-grey">{{ b.buyerName }} · {{ formatDateTime(b.preferredAt) }}</p>
              <p v-if="b.note" class="mt-1.5 text-xs italic text-charcoal">“{{ b.note }}”</p>

              <div v-if="b.status === 'PENDING' && decliningBookingId !== b.id" class="mt-3 flex gap-2">
                <button class="btn bg-success px-3 py-1.5 text-xs text-white hover:bg-green-700" :disabled="bookingActing === b.id" @click="acceptBooking(b.id)">Accept</button>
                <button class="btn-secondary px-3 py-1.5 text-xs text-danger" :disabled="bookingActing === b.id" @click="startDecline(b.id)">Decline</button>
              </div>
              <div v-if="decliningBookingId === b.id" class="mt-3 space-y-2">
                <textarea v-model="declineReason" rows="2" placeholder="Reason (optional, shown to the student)" class="input-field resize-y"></textarea>
                <div class="flex justify-end gap-2">
                  <button class="btn-secondary px-3 py-1.5 text-xs" @click="decliningBookingId = null">Cancel</button>
                  <button class="btn-danger px-3 py-1.5 text-xs" :disabled="bookingActing === b.id" @click="confirmDecline(b.id)">Decline booking</button>
                </div>
              </div>
            </li>
          </ul>
        </div>

        <!-- Pending questions -->
        <div v-if="pendingQuestions.length > 0" class="card space-y-4">
          <div class="flex items-center justify-between gap-2">
            <h2 class="section-title">Questions awaiting reply</h2>
            <RouterLink to="/questions" class="link shrink-0 text-xs">View all</RouterLink>
          </div>
          <ul class="space-y-3">
            <li v-for="q in pendingQuestions" :key="q.id" class="rounded-control border border-light-grey p-3.5">
              <p class="text-xs text-medium-grey">{{ q.askerName }} on <span class="font-medium text-charcoal">{{ q.listingName }}</span></p>
              <p class="mt-1 text-sm font-medium text-charcoal">{{ q.questionText }}</p>
              <div class="mt-2.5 flex gap-2">
                <input v-model="answerDrafts[q.id]" class="input-field py-2" placeholder="Type your answer…" @keyup.enter="answerQuestion(q.id)" />
                <button class="btn-primary shrink-0 px-3.5 py-2" :disabled="answeringId === q.id || !answerDrafts[q.id]?.trim()" @click="answerQuestion(q.id)">Reply</button>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <!-- Businesses -->
      <div class="space-y-4">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 class="section-title">Your businesses</h2>
          <div class="flex flex-wrap items-center gap-3">
            <select v-if="businesses.length > 1" class="input-field w-full py-2 text-xs sm:w-48" @change="scrollToBusiness(($event.target as HTMLSelectElement).value)">
              <option value="" disabled selected>Jump to business…</option>
              <option v-for="b in businesses" :key="b.id" :value="b.id">{{ b.businessName }}</option>
            </select>
            <RouterLink to="/my-listings" class="link text-xs">Manage all listings</RouterLink>
          </div>
        </div>

        <EmptyState v-if="businesses.length === 0" :icon="Store" title="Register your first business" description="Add a business below - once an admin verifies it, your listings get the verified badge." />

        <div v-for="business in businesses" :id="`business-${business.id}`" :key="business.id" class="card space-y-4 transition">
          <div class="flex items-start justify-between gap-3">
            <div class="flex min-w-0 items-center gap-3">
              <img v-if="business.imageUrl" :src="business.imageUrl" alt="" class="h-11 w-11 shrink-0 rounded-xl object-cover ring-1 ring-light-grey" />
              <span v-else class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl font-display font-bold" :class="categoryMeta(business.category).tile">{{ business.businessName.charAt(0) }}</span>
              <div class="min-w-0">
                <RouterLink :to="`/providers/${business.id}`" class="block truncate font-display font-semibold text-uni-navy hover:text-teal-700">{{ business.businessName }}</RouterLink>
                <p class="text-xs text-medium-grey">{{ business.category }}</p>
              </div>
            </div>
            <span class="badge shrink-0" :class="VERIFICATION_STYLES[business.verificationStatus]">
              {{ business.verificationStatus.charAt(0) + business.verificationStatus.slice(1).toLowerCase() }}
            </span>
          </div>

          <div v-if="business.verificationStatus === 'PENDING'" class="rounded-control border border-gold-200 bg-gold-50/60 p-4">
            <div class="flex items-center justify-between">
              <span class="text-sm font-semibold text-uni-navy">Get verified</span>
              <span class="text-xs font-semibold text-medium-grey">
                {{ (business.description ? 1 : 0) + ((listingsByBusiness[business.id] ?? []).length > 0 ? 1 : 0) + (business.imageUrl ? 1 : 0) }} / 3
              </span>
            </div>
            <ul class="mt-2.5 space-y-1.5 text-sm">
              <li
                v-for="item in [
                  { done: !!business.description, label: 'Business profile complete' },
                  { done: (listingsByBusiness[business.id] ?? []).length > 0, label: 'At least one listing added' },
                  { done: !!business.imageUrl, label: 'Business logo added (recommended)' },
                ]"
                :key="item.label"
                class="flex items-center gap-2"
                :class="item.done ? 'text-charcoal' : 'text-medium-grey'"
              >
                <span class="flex h-5 w-5 items-center justify-center rounded-full" :class="item.done ? 'bg-success text-white' : 'border-2 border-light-grey'">
                  <Check v-if="item.done" class="h-3 w-3" />
                </span>
                {{ item.label }}
              </li>
            </ul>
            <p class="mt-2.5 text-xs text-medium-grey">An admin reviews new businesses within a few days - you can keep editing while you wait.</p>
          </div>

          <dl v-if="statsByBusiness[business.id]" class="grid grid-cols-4 divide-x divide-light-grey rounded-control bg-soft-grey py-3 text-center">
            <div v-for="stat in [
              { label: 'Listings', value: statsByBusiness[business.id].totalListings },
              { label: 'Views', value: statsByBusiness[business.id].totalViews },
              { label: 'Saves', value: statsByBusiness[business.id].totalSaves },
              { label: 'Followers', value: statsByBusiness[business.id].followerCount },
            ]" :key="stat.label">
              <dd class="text-lg font-semibold text-charcoal">{{ stat.value }}</dd>
              <dt class="text-[11px] text-medium-grey">{{ stat.label }}</dt>
            </div>
          </dl>

          <ul v-if="(listingsByBusiness[business.id] ?? []).length > 0" class="divide-y divide-light-grey rounded-control border border-light-grey">
            <li v-for="listing in listingsByBusiness[business.id] ?? []" :key="listing.id">
              <div v-if="editingListingId !== listing.id" class="flex flex-wrap items-center justify-between gap-2 px-3.5 py-2.5 text-sm">
                <div class="min-w-0">
                  <RouterLink :to="`/listings/${listing.id}`" class="font-medium hover:text-teal-700" :class="listing.status === 'INACTIVE' ? 'text-medium-grey line-through' : 'text-charcoal'">
                    {{ listing.name }}
                  </RouterLink>
                  <p class="flex flex-wrap items-center gap-2 text-xs text-medium-grey">
                    {{ LISTING_STATUS_LABELS[listing.status] ?? listing.status }} · {{ listing.viewCount }} views
                    <span v-if="listing.takenDownAt" class="badge bg-red-50 text-danger" :title="listing.takedownReason ?? ''">
                      Removed by an admin: {{ listing.takedownReason }}
                    </span>
                    <span
                      v-if="listing.type === 'PRODUCT' && listing.status === 'ACTIVE' && listing.lowStockThreshold != null && (listing.stockQuantity ?? 0) <= listing.lowStockThreshold"
                      class="badge bg-amber-50 text-warning"
                    >
                      Low stock · {{ listing.stockQuantity }} left
                    </span>
                  </p>
                </div>
                <div class="flex items-center gap-1">
                  <button class="btn-ghost px-2.5 py-1.5 text-xs" @click="startEdit(listing)"><Pencil class="h-3.5 w-3.5" /> Edit</button>
                  <button v-if="listing.status === 'ACTIVE'" class="btn-ghost px-2.5 py-1.5 text-xs hover:text-danger" @click="deactivateListing(listing.id)">Deactivate</button>
                  <button v-else-if="listing.status === 'SOLD_OUT'" class="btn-ghost px-2.5 py-1.5 text-xs text-teal-700" @click="startEdit(listing)">Restock</button>
                  <button v-else-if="!listing.takenDownAt" class="btn-ghost px-2.5 py-1.5 text-xs text-success" @click="reactivateListing(listing.id)">Reactivate</button>
                </div>
              </div>

              <div v-else class="space-y-3 bg-teal-50/30 p-4">
                <div class="flex items-center justify-between text-sm font-semibold text-uni-navy">
                  <span>Editing: {{ listing.name }}</span>
                  <span class="badge bg-navy-50 text-navy-600">{{ listing.type === "PRODUCT" ? "Product" : "Service" }}</span>
                </div>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div class="sm:col-span-2"><label class="field-label">Title</label><input v-model="editForm.name" class="input-field" /></div>
                  <div>
                    <label class="field-label">Category</label>
                    <select v-model="editForm.category" class="input-field">
                      <option v-if="editForm.category && !categories.includes(editForm.category)" :value="editForm.category">{{ editForm.category }}</option>
                      <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
                    </select>
                  </div>
                  <div>
                    <label class="field-label">Status</label>
                    <select v-model="editForm.status" class="input-field">
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                    </select>
                  </div>
                  <div class="sm:col-span-2"><label class="field-label">Description</label><textarea v-model="editForm.description" class="input-field" rows="2"></textarea></div>
                  <div><label class="field-label">Price (ZAR)</label><input v-model.number="editForm.price" type="number" min="0" step="0.01" class="input-field" /></div>
                  <template v-if="listing.type === 'PRODUCT'">
                    <div><label class="field-label">Stock quantity</label><input v-model.number="editForm.stockQuantity" type="number" min="0" class="input-field" /></div>
                    <div class="sm:col-span-2">
                      <label class="field-label">Low-stock alert threshold (optional)</label>
                      <input v-model.number="editForm.lowStockThreshold" type="number" min="0" class="input-field" placeholder="e.g. 20" />
                    </div>
                  </template>
                  <template v-else>
                    <div><label class="field-label">Duration (minutes)</label><input v-model.number="editForm.durationMinutes" type="number" min="0" class="input-field" /></div>
                    <div class="sm:col-span-2"><label class="field-label">Availability</label><input v-model="editForm.availabilitySchedule" class="input-field" placeholder="e.g. Weekdays 2-6pm" /></div>
                  </template>
                </div>
                <MultiImageUpload :id="`edit-photo-${listing.id}`" v-model="editForm.imageUrls" />
                <div class="flex justify-end gap-2">
                  <button class="btn-secondary" @click="cancelEdit">Cancel</button>
                  <button class="btn-primary" :disabled="savingEdit" @click="saveEdit(listing)">{{ savingEdit ? "Saving…" : "Save changes" }}</button>
                </div>
              </div>
            </li>
          </ul>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
        <!-- New listing -->
        <div v-if="businesses.length > 0" id="new-listing-form" class="card scroll-mt-24 space-y-4">
          <h2 class="section-title flex items-center gap-2"><Plus class="h-5 w-5 text-teal-600" /> New listing</h2>
          <RestrictedItemsNotice />
          <form class="grid grid-cols-1 gap-3 sm:grid-cols-2" @submit.prevent="createListing">
            <div class="sm:col-span-2">
              <label class="field-label">Business</label>
              <select v-model="newListing.businessId" required class="input-field">
                <option value="" disabled>Select business</option>
                <option v-for="business in businesses" :key="business.id" :value="business.id">{{ business.businessName }}</option>
              </select>
            </div>
            <div class="sm:col-span-2">
              <label class="field-label">Type</label>
              <div class="grid grid-cols-2 gap-1 rounded-control bg-soft-grey p-1">
                <button
                  v-for="k in (['PRODUCT', 'SERVICE'] as const)"
                  :key="k"
                  type="button"
                  class="rounded-lg py-2 text-xs font-semibold transition"
                  :class="newListing.kind === k ? 'bg-white text-uni-navy shadow-card' : 'text-medium-grey hover:text-uni-navy'"
                  @click="newListing.kind = k"
                >
                  {{ k === "PRODUCT" ? "Product" : "Service" }}
                </button>
              </div>
            </div>
            <div class="sm:col-span-2"><label class="field-label">Title</label><input v-model="newListing.name" required class="input-field" placeholder="e.g. A3 colour poster printing" /></div>
            <div class="sm:col-span-2"><label class="field-label">Description</label><textarea v-model="newListing.description" required class="input-field" rows="3" placeholder="What's included, turnaround time, pickup spot…"></textarea></div>
            <div>
              <label class="field-label">Category</label>
              <select v-model="newListing.category" required class="input-field">
                <option value="" disabled>Select category</option>
                <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div><label class="field-label">Price (ZAR)</label><input v-model.number="newListing.price" type="number" min="0" step="0.01" required class="input-field" /></div>
            <template v-if="newListing.kind === 'PRODUCT'">
              <div><label class="field-label">Stock quantity</label><input v-model.number="newListing.stockQuantity" type="number" min="0" class="input-field" /></div>
            </template>
            <template v-else>
              <div><label class="field-label">Duration (minutes)</label><input v-model.number="newListing.durationMinutes" type="number" min="0" class="input-field" /></div>
              <div><label class="field-label">Availability</label><input v-model="newListing.availabilitySchedule" class="input-field" placeholder="e.g. Weekdays 2-6pm" /></div>
            </template>
            <div class="sm:col-span-2">
              <MultiImageUpload id="new-listing-photo" v-model="newListing.imageUrls" label="Photos (optional)" />
            </div>
            <button type="submit" class="btn-primary sm:col-span-2" :disabled="creatingListing">{{ creatingListing ? "Publishing…" : "Publish listing" }}</button>
          </form>
        </div>

        <!-- Add business -->
        <div id="add-business-form" class="card h-fit scroll-mt-24 space-y-4">
          <h2 class="section-title flex items-center gap-2"><Store class="h-5 w-5 text-teal-600" /> Add a business</h2>
          <form class="space-y-3" @submit.prevent="createBusiness">
            <div><label class="field-label">Business name</label><input v-model="newBusiness.businessName" required class="input-field" /></div>
            <div>
              <label class="field-label">Category</label>
              <select v-model="newBusiness.category" required class="input-field">
                <option value="" disabled>Select category</option>
                <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div><label class="field-label">Short description</label><input v-model="newBusiness.description" required class="input-field" /></div>
            <div>
              <label class="field-label">Campus buyers collect from</label>
              <select v-model="newBusiness.campus" required class="input-field">
                <option value="" disabled>Select campus</option>
                <option v-for="c in campuses" :key="c.key" :value="c.key">{{ c.label }}</option>
              </select>
            </div>
            <div>
              <label class="field-label">Pickup spot <span class="font-normal text-medium-grey">(optional)</span></label>
              <input v-model="newBusiness.pickupLocation" maxlength="120" class="input-field" placeholder="e.g. Catsville res, Block C" />
            </div>
            <button type="submit" class="btn-secondary w-full" :disabled="creatingBusiness">{{ creatingBusiness ? "Adding…" : "Add business" }}</button>
          </form>
        </div>
      </div>
    </div>
    <SellerRulesModal v-if="rulesOpen" @close="rulesOpen = false" @accepted="onRulesAccepted" />
  </section>
</template>
