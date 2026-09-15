<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useToastStore } from "@/stores/toast";
import { getRecentlyViewed } from "@/lib/recentlyViewed";
import { api, extractErrorMessage } from "@/lib/api";
import { useCategories } from "@/lib/categories";
import ListingCard from "@/components/ListingCard.vue";
import type { BookingSummaryView, BusinessDTO, BusinessStatsDTO, ListingDTO, QuestionView, ReportStatus, ReportSummaryView } from "@/lib/types";

const auth = useAuthStore();
const saved = useSavedListingsStore();
const followed = useFollowedProvidersStore();
const toast = useToastStore();
const categories = useCategories();

const businesses = ref<BusinessDTO[]>([]);
const listingsByBusiness = ref<Record<string, ListingDTO[]>>({});
const error = ref("");
const becomingSeller = ref(false);

const newBusiness = ref({ businessName: "", description: "", category: "" });
const creatingBusiness = ref(false);

const newListing = ref({
  businessId: "",
  kind: "PRODUCT" as "PRODUCT" | "SERVICE",
  name: "",
  description: "",
  category: "",
  price: 0,
  stockQuantity: 1,
  imageUrl: "",
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
      favoriteCategories.value.slice(0, 3).map((c) => api.get<ListingDTO[]>("/listings", { params: { category: c, sort: "views" } })),
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

const REASON_LABELS: Record<string, string> = {
  MISREPRESENTATION: "Misrepresentation",
  NON_DELIVERY: "Non-delivery",
  INAPPROPRIATE_CONDUCT: "Inappropriate conduct",
  SPAM: "Spam",
  OTHER: "Other",
};

const STATUS_STYLES: Record<ReportStatus, string> = {
  OPEN: "bg-warning/15 text-warning",
  UNDER_REVIEW: "bg-info/15 text-info",
  RESOLVED: "bg-success/15 text-success",
  DISMISSED: "bg-medium-grey/15 text-medium-grey",
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
    el.classList.add("ring-2", "ring-campus-teal");
    setTimeout(() => el.classList.remove("ring-2", "ring-campus-teal"), 1500);
  }
}

// ---- Seller: booking requests ----
const sellerBookings = ref<BookingSummaryView[]>([]);
const decliningBookingId = ref<string | null>(null);
const declineReason = ref("");
const bookingActing = ref<string | null>(null);

const pendingBookings = computed(() => sellerBookings.value.filter((b) => b.status === "PENDING"));

const BOOKING_STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-warning/15 text-warning",
  ACCEPTED: "bg-success/15 text-success",
  DECLINED: "bg-danger/15 text-danger",
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

async function becomeSeller() {
  becomingSeller.value = true;
  try {
    await auth.becomeSeller();
    await loadBusinesses();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    becomingSeller.value = false;
  }
}

async function createBusiness() {
  creatingBusiness.value = true;
  error.value = "";
  try {
    await api.post("/businesses", newBusiness.value);
    toast.success("Business added!", "An admin will review it within a few days.");
    newBusiness.value = { businessName: "", description: "", category: "" };
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
    };
    if (newListing.value.kind === "PRODUCT") {
      await api.post("/listings/products", {
        ...payload,
        stockQuantity: newListing.value.stockQuantity,
        imageUrl: newListing.value.imageUrl || null,
      });
    } else {
      await api.post("/listings/services", {
        ...payload,
        durationMinutes: newListing.value.durationMinutes,
        availabilitySchedule: newListing.value.availabilitySchedule || null,
      });
    }
    toast.success("Listing published!");
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
  imageUrl: "",
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
    imageUrl: listing.imageUrl ?? "",
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
      imageUrl: listing.type === "PRODUCT" ? editForm.value.imageUrl : undefined,
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
  <section class="space-y-6">
    <div class="card">
      <h1 class="font-display text-xl font-bold text-uni-navy">
        Welcome, {{ auth.user?.firstName }}
      </h1>
      <p class="text-sm text-medium-grey">{{ auth.user?.email }} · {{ auth.user?.studentNumber }}</p>
      <div class="mt-3 flex flex-wrap gap-2 border-t border-light-grey pt-3">
        <RouterLink to="/orders" class="badge bg-soft-grey text-charcoal hover:bg-light-grey">My orders</RouterLink>
        <RouterLink to="/saved-searches" class="badge bg-soft-grey text-charcoal hover:bg-light-grey">Saved searches</RouterLink>
        <RouterLink to="/messages" class="badge bg-soft-grey text-charcoal hover:bg-light-grey">Messages</RouterLink>
        <template v-if="auth.isSeller">
          <RouterLink to="/orders/selling" class="badge bg-soft-grey text-charcoal hover:bg-light-grey">Selling: orders</RouterLink>
          <RouterLink to="/promo-codes" class="badge bg-soft-grey text-charcoal hover:bg-light-grey">Promo codes</RouterLink>
        </template>
      </div>
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>

    <!-- Saved listings -->
    <div>
      <div class="mb-3 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <h2 class="font-display text-lg font-semibold text-uni-navy">Saved listings</h2>
          <span v-if="saved.listings.length > 0" class="text-xs text-medium-grey">{{ saved.listings.length }} saved</span>
        </div>
        <button
          v-if="saved.listings.length > 0"
          class="text-xs font-medium text-campus-teal"
          @click="savedSelectMode = !savedSelectMode; savedSelected.clear()"
        >
          {{ savedSelectMode ? "Cancel" : "Select" }}
        </button>
      </div>
      <div v-if="saved.listings.length === 0" class="card text-sm text-medium-grey">
        Tap the heart on any listing to save it here for later.
      </div>
      <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div v-for="listing in saved.listings" :key="listing.id" class="relative">
          <input
            v-if="savedSelectMode"
            type="checkbox"
            class="absolute right-2.5 top-2.5 z-10 h-4 w-4 accent-campus-teal"
            :checked="savedSelected.has(listing.id)"
            @change="toggleSavedSelected(listing.id)"
            @click.stop
          />
          <ListingCard :listing="listing" :class="{ '!border-campus-teal bg-campus-teal/5': savedSelected.has(listing.id) }" />
        </div>
      </div>
      <div v-if="savedSelectMode && savedSelected.size > 0" class="mt-3 flex items-center justify-between rounded-control bg-uni-navy px-4 py-3">
        <p class="text-sm font-semibold text-white">{{ savedSelected.size }} selected</p>
        <button class="inline-flex items-center gap-1.5 rounded-control bg-white/10 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50" :disabled="bulkUnsaving" @click="bulkUnsave">
          {{ bulkUnsaving ? "Removing..." : "Remove from saved" }}
        </button>
      </div>
    </div>

    <!-- Recommended for you -->
    <div v-if="recommended.length > 0">
      <div class="mb-1 flex items-center gap-2">
        <span class="text-lg">✨</span>
        <h2 class="font-display text-lg font-semibold text-uni-navy">Recommended for you</h2>
      </div>
      <p class="mb-3 text-xs text-medium-grey">Based on categories you've saved and followed: {{ favoriteCategories.join(", ") }}</p>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ListingCard v-for="listing in recommended" :key="listing.id" :listing="listing" />
      </div>
    </div>

    <!-- Providers you follow -->
    <div>
      <div class="mb-3 flex items-center gap-2">
        <h2 class="font-display text-lg font-semibold text-uni-navy">Providers you follow</h2>
        <span v-if="followed.providers.length > 0" class="text-xs text-medium-grey">{{ followed.providers.length }} followed</span>
      </div>
      <div v-if="followed.providers.length === 0" class="card text-sm text-medium-grey">
        Follow a provider from their profile to see new listings from them here.
      </div>
      <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <RouterLink
          v-for="p in followed.providers"
          :key="p.businessId"
          :to="`/providers/${p.businessId}`"
          class="card space-y-2 transition hover:shadow-md"
        >
          <div class="flex items-center justify-between">
            <h3 class="font-display text-sm font-semibold text-uni-navy">{{ p.businessName }}</h3>
            <span
              class="badge"
              :class="p.verificationStatus === 'VERIFIED' ? 'bg-success/15 text-success' : 'bg-warning/15 text-warning'"
            >
              {{ p.verificationStatus === "VERIFIED" ? "Verified" : "Pending" }}
            </span>
          </div>
          <p class="text-xs text-medium-grey">{{ p.category }} &middot; {{ p.activeListingCount }} active listings</p>
        </RouterLink>
      </div>
    </div>

    <!-- Recently viewed -->
    <div v-if="recentlyViewed.length > 0">
      <div class="mb-3 flex items-center justify-between">
        <h2 class="font-display text-lg font-semibold text-uni-navy">Recently viewed</h2>
        <RouterLink to="/recently-viewed" class="text-xs font-medium text-campus-teal underline">View all &rarr;</RouterLink>
      </div>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ListingCard v-for="listing in recentlyViewed.slice(0, 3)" :key="listing.id" :listing="listing" />
      </div>
    </div>

    <!-- My reports -->
    <div>
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Your reports</h2>
      <p v-if="reportsError" class="text-sm text-danger">{{ reportsError }}</p>
      <p v-else-if="reportsLoading" class="text-sm text-medium-grey">Loading...</p>
      <div v-else-if="myReports.length === 0" class="card text-sm text-medium-grey">
        Reports you file on listings or providers will show up here with their status.
      </div>
      <div v-else class="space-y-2">
        <div
          v-for="r in myReports"
          :key="r.id"
          class="flex items-center justify-between rounded-card border border-light-grey bg-white px-4 py-3"
        >
          <div>
            <p class="text-sm font-semibold text-uni-navy">{{ REASON_LABELS[r.reason] ?? r.reason }}</p>
            <p class="text-xs text-medium-grey">{{ r.target.label }} &middot; Filed {{ relativeTime(r.createdAt) }}</p>
          </div>
          <span class="badge" :class="STATUS_STYLES[r.status]">{{ STATUS_LABELS[r.status] }}</span>
        </div>
      </div>
    </div>

    <div class="h-px bg-light-grey"></div>

    <div v-if="!auth.isSeller" class="card space-y-3">
      <h2 class="font-display text-lg font-semibold text-uni-navy">Have something to offer?</h2>
      <p class="text-sm text-charcoal">
        Unlock seller features on this same account - no separate sign-up needed.
      </p>
      <button class="btn-primary" :disabled="becomingSeller" @click="becomeSeller">
        {{ becomingSeller ? "Unlocking..." : "Become a Seller" }}
      </button>
    </div>

    <template v-else>
      <div class="card space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-lg font-semibold text-uni-navy">Your businesses</h2>
          <div class="flex items-center gap-3">
            <select v-if="businesses.length > 1" class="input-field w-40 text-xs" @change="scrollToBusiness(($event.target as HTMLSelectElement).value)">
              <option value="" disabled selected>Jump to business...</option>
              <option v-for="b in businesses" :key="b.id" :value="b.id">{{ b.businessName }}</option>
            </select>
            <RouterLink to="/my-listings" class="text-xs font-medium text-campus-teal underline">View all listings &rarr;</RouterLink>
          </div>
        </div>

        <div v-if="businesses.length === 0" class="text-sm text-medium-grey">
          You haven't registered a business yet - add one below to start listing.
        </div>

        <ul v-else class="space-y-2">
          <li v-for="business in businesses" :id="`business-${business.id}`" :key="business.id" class="rounded-control border border-light-grey p-3 transition">
            <div class="flex items-center justify-between">
              <span class="font-semibold text-uni-navy">{{ business.businessName }}</span>
              <span
                class="badge"
                :class="{
                  'bg-success/15 text-success': business.verificationStatus === 'VERIFIED',
                  'bg-warning/15 text-warning': business.verificationStatus === 'PENDING',
                  'bg-danger/15 text-danger': business.verificationStatus === 'REJECTED',
                }"
              >
                {{ business.verificationStatus }}
              </span>
            </div>
            <p class="text-sm text-medium-grey">{{ business.category }}</p>

            <div
              v-if="business.verificationStatus === 'PENDING'"
              class="mt-3 space-y-2 rounded-control border border-academic-gold/50 bg-academic-gold/5 p-3"
            >
              <div class="flex items-center justify-between">
                <span class="text-sm font-semibold text-uni-navy">Get {{ business.businessName }} verified</span>
                <span class="text-xs font-semibold text-medium-grey">
                  {{ (business.description ? 1 : 0) + ((listingsByBusiness[business.id] ?? []).length > 0 ? 1 : 0) + (business.imageUrl ? 1 : 0) }} / 3
                </span>
              </div>
              <ul class="space-y-1 text-sm">
                <li class="flex items-center gap-2 text-charcoal">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-success text-xs text-white">✓</span>
                  Business profile complete
                </li>
                <li class="flex items-center gap-2" :class="(listingsByBusiness[business.id] ?? []).length > 0 ? 'text-charcoal' : 'text-medium-grey'">
                  <span
                    class="flex h-5 w-5 items-center justify-center rounded-full text-xs"
                    :class="(listingsByBusiness[business.id] ?? []).length > 0 ? 'bg-success text-white' : 'border-2 border-light-grey'"
                  >{{ (listingsByBusiness[business.id] ?? []).length > 0 ? "✓" : "" }}</span>
                  At least one listing added
                </li>
                <li class="flex items-center gap-2" :class="business.imageUrl ? 'text-charcoal' : 'text-medium-grey'">
                  <span
                    class="flex h-5 w-5 items-center justify-center rounded-full text-xs"
                    :class="business.imageUrl ? 'bg-success text-white' : 'border-2 border-light-grey'"
                  >{{ business.imageUrl ? "✓" : "" }}</span>
                  Add a business logo <span class="text-xs">(optional but recommended)</span>
                </li>
              </ul>
              <p class="text-xs text-medium-grey">An admin reviews new businesses within a few days - you can keep editing while you wait.</p>
            </div>

            <div v-if="statsByBusiness[business.id]" class="mt-3 grid grid-cols-4 gap-2 rounded-control bg-soft-grey p-3 text-center">
              <div>
                <p class="font-display text-lg font-bold text-uni-navy">{{ statsByBusiness[business.id].totalListings }}</p>
                <p class="text-[11px] text-medium-grey">Listings</p>
              </div>
              <div>
                <p class="font-display text-lg font-bold text-uni-navy">{{ statsByBusiness[business.id].totalViews }}</p>
                <p class="text-[11px] text-medium-grey">Views</p>
              </div>
              <div>
                <p class="font-display text-lg font-bold text-uni-navy">{{ statsByBusiness[business.id].totalSaves }}</p>
                <p class="text-[11px] text-medium-grey">Saves</p>
              </div>
              <div>
                <p class="font-display text-lg font-bold text-uni-navy">{{ statsByBusiness[business.id].followerCount }}</p>
                <p class="text-[11px] text-medium-grey">Followers</p>
              </div>
            </div>

            <ul class="mt-2 space-y-2">
              <li v-for="listing in listingsByBusiness[business.id] ?? []" :key="listing.id">
                <!-- Normal row -->
                <div v-if="editingListingId !== listing.id" class="flex items-center justify-between rounded-control bg-soft-grey px-3 py-2 text-sm">
                  <span :class="{ 'text-medium-grey line-through': listing.status === 'INACTIVE' }">
                    {{ listing.name }} · {{ LISTING_STATUS_LABELS[listing.status] ?? listing.status }} · {{ listing.viewCount }} views
                    <span
                      v-if="listing.type === 'PRODUCT' && listing.status === 'ACTIVE' && listing.lowStockThreshold != null && (listing.stockQuantity ?? 0) <= listing.lowStockThreshold"
                      class="badge bg-warning/15 text-warning ml-1"
                    >
                      ⚠️ Low stock - {{ listing.stockQuantity }} left
                    </span>
                  </span>
                  <div class="flex items-center gap-3">
                    <button class="text-xs font-medium text-campus-teal underline" @click="startEdit(listing)">Edit</button>
                    <button
                      v-if="listing.status === 'ACTIVE'"
                      class="text-xs font-medium text-danger underline"
                      @click="deactivateListing(listing.id)"
                    >
                      Deactivate
                    </button>
                    <button
                      v-else-if="listing.status === 'SOLD_OUT'"
                      class="text-xs font-medium text-campus-teal underline"
                      @click="startEdit(listing)"
                    >
                      Restock
                    </button>
                    <button v-else class="text-xs font-medium text-success underline" @click="reactivateListing(listing.id)">
                      Reactivate
                    </button>
                  </div>
                </div>

                <!-- Edit form -->
                <div v-else class="space-y-3 rounded-control border border-campus-teal bg-white p-3">
                  <div class="flex items-center justify-between text-sm font-medium text-uni-navy">
                    <span>Editing: {{ listing.name }}</span>
                    <span class="badge bg-sky-blue/20 text-uni-navy">{{ listing.type }}</span>
                  </div>
                  <div class="grid gap-2 sm:grid-cols-2">
                    <input v-model="editForm.name" class="input-field sm:col-span-2" placeholder="Title" />
                    <select v-model="editForm.category" class="input-field">
                      <option v-if="editForm.category && !categories.includes(editForm.category)" :value="editForm.category">
                        {{ editForm.category }}
                      </option>
                      <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
                    </select>
                    <select v-model="editForm.status" class="input-field">
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                    </select>
                    <textarea v-model="editForm.description" class="input-field sm:col-span-2" rows="2"></textarea>
                    <input v-model.number="editForm.price" type="number" min="0" step="0.01" class="input-field" placeholder="Price (ZAR)" />
                    <template v-if="listing.type === 'PRODUCT'">
                      <input
                        v-model.number="editForm.stockQuantity"
                        type="number"
                        min="0"
                        class="input-field"
                        placeholder="Stock quantity"
                      />
                      <input
                        v-model="editForm.imageUrl"
                        class="input-field sm:col-span-2"
                        placeholder="Image URL (optional)"
                      />
                      <div class="sm:col-span-2">
                        <label class="mb-1 block text-xs font-medium text-medium-grey">Low-stock alert threshold (optional)</label>
                        <input
                          v-model.number="editForm.lowStockThreshold"
                          type="number"
                          min="0"
                          class="input-field"
                          placeholder="e.g. 20"
                        />
                      </div>
                    </template>
                    <template v-else>
                      <input
                        v-model.number="editForm.durationMinutes"
                        type="number"
                        min="0"
                        class="input-field"
                        placeholder="Duration (minutes)"
                      />
                      <input
                        v-model="editForm.availabilitySchedule"
                        class="input-field sm:col-span-2"
                        placeholder="Availability (e.g. Weekdays 2-6pm)"
                      />
                    </template>
                  </div>
                  <div class="flex justify-end gap-2">
                    <button class="btn-secondary text-sm" @click="cancelEdit">Cancel</button>
                    <button class="btn-primary text-sm" :disabled="savingEdit" @click="saveEdit(listing)">
                      {{ savingEdit ? "Saving..." : "Save changes" }}
                    </button>
                  </div>
                </div>
              </li>
            </ul>
          </li>
        </ul>

        <form class="grid gap-2 border-t border-light-grey pt-3 sm:grid-cols-3" @submit.prevent="createBusiness">
          <input v-model="newBusiness.businessName" required placeholder="Business name" class="input-field" />
          <select v-model="newBusiness.category" required class="input-field">
            <option value="" disabled>Select category</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="newBusiness.description" required placeholder="Short description" class="input-field" />
          <button type="submit" class="btn-secondary sm:col-span-3" :disabled="creatingBusiness">
            {{ creatingBusiness ? "Adding..." : "Add business" }}
          </button>
        </form>
      </div>

      <div v-if="sellerBookings.length > 0" class="card space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-lg font-semibold text-uni-navy">Booking requests</h2>
          <span v-if="pendingBookings.length > 0" class="badge bg-warning/15 text-warning">{{ pendingBookings.length }} pending</span>
        </div>

        <div class="space-y-2">
          <div
            v-for="b in sellerBookings"
            :key="b.id"
            class="rounded-control border p-3"
            :class="b.status === 'PENDING' ? 'border-warning/40 bg-warning/5' : 'border-light-grey opacity-70'"
          >
            <div class="flex items-center justify-between">
              <span class="text-sm font-semibold text-uni-navy">{{ b.listingName }}</span>
              <span class="badge" :class="BOOKING_STATUS_STYLES[b.status]">{{ b.status.charAt(0) + b.status.slice(1).toLowerCase() }}</span>
            </div>
            <p class="mt-0.5 text-xs text-medium-grey">{{ b.buyerName }} &middot; {{ formatDateTime(b.preferredAt) }}</p>
            <p v-if="b.note" class="mt-1 text-xs text-charcoal">"{{ b.note }}"</p>

            <div v-if="b.status === 'PENDING' && decliningBookingId !== b.id" class="mt-2 flex gap-2">
              <button
                class="inline-flex items-center justify-center rounded-control bg-success px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                :disabled="bookingActing === b.id"
                @click="acceptBooking(b.id)"
              >
                Accept
              </button>
              <button
                class="inline-flex items-center justify-center rounded-control border border-danger bg-white px-3 py-1.5 text-xs font-semibold text-danger disabled:opacity-50"
                :disabled="bookingActing === b.id"
                @click="startDecline(b.id)"
              >
                Decline
              </button>
            </div>

            <div v-if="decliningBookingId === b.id" class="mt-2 space-y-2">
              <textarea v-model="declineReason" rows="2" placeholder="Reason (optional, shown to the student)" class="input-field resize-y"></textarea>
              <div class="flex justify-end gap-2">
                <button class="btn-secondary text-xs" @click="decliningBookingId = null">Cancel</button>
                <button
                  class="inline-flex items-center justify-center rounded-control bg-danger px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                  :disabled="bookingActing === b.id"
                  @click="confirmDecline(b.id)"
                >
                  Decline booking
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="pendingQuestions.length > 0" class="card space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-lg font-semibold text-uni-navy">Questions awaiting your reply</h2>
          <span class="badge bg-warning/15 text-warning">{{ pendingQuestions.length }} pending</span>
        </div>
        <div class="space-y-3">
          <div v-for="q in pendingQuestions" :key="q.id" class="rounded-control border border-light-grey p-3">
            <p class="text-xs text-medium-grey">{{ q.askerName }} asked on <span class="font-medium text-charcoal">{{ q.listingName }}</span></p>
            <p class="mt-1 text-sm font-medium text-charcoal">{{ q.questionText }}</p>
            <div class="mt-2 flex gap-2">
              <input v-model="answerDrafts[q.id]" class="input-field" placeholder="Type your answer..." @keyup.enter="answerQuestion(q.id)" />
              <button class="btn-primary shrink-0 text-sm" :disabled="answeringId === q.id || !answerDrafts[q.id]?.trim()" @click="answerQuestion(q.id)">
                Reply
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="businesses.length > 0" class="card space-y-3">
        <h2 class="font-display text-lg font-semibold text-uni-navy">New listing</h2>
        <form class="grid gap-2 sm:grid-cols-2" @submit.prevent="createListing">
          <select v-model="newListing.businessId" required class="input-field sm:col-span-2">
            <option value="" disabled>Select business</option>
            <option v-for="business in businesses" :key="business.id" :value="business.id">
              {{ business.businessName }}
            </option>
          </select>
          <select v-model="newListing.kind" class="input-field">
            <option value="PRODUCT">Product</option>
            <option value="SERVICE">Service</option>
          </select>
          <select v-model="newListing.category" required class="input-field">
            <option value="" disabled>Select category</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="newListing.name" required placeholder="Title" class="input-field sm:col-span-2" />
          <textarea
            v-model="newListing.description"
            required
            placeholder="Description"
            class="input-field sm:col-span-2"
            rows="2"
          ></textarea>
          <input
            v-model.number="newListing.price"
            type="number"
            min="0"
            step="0.01"
            required
            placeholder="Price (ZAR)"
            class="input-field"
          />

          <input
            v-if="newListing.kind === 'PRODUCT'"
            v-model.number="newListing.stockQuantity"
            type="number"
            min="0"
            placeholder="Stock quantity"
            class="input-field"
          />
          <input
            v-if="newListing.kind === 'PRODUCT'"
            v-model="newListing.imageUrl"
            placeholder="Image URL (optional)"
            class="input-field sm:col-span-2"
          />
          <input
            v-if="newListing.kind === 'SERVICE'"
            v-model.number="newListing.durationMinutes"
            type="number"
            min="0"
            placeholder="Duration (minutes)"
            class="input-field"
          />

          <input
            v-if="newListing.kind === 'SERVICE'"
            v-model="newListing.availabilitySchedule"
            placeholder="Availability (e.g. Weekdays 2-6pm)"
            class="input-field sm:col-span-2"
          />

          <button type="submit" class="btn-primary sm:col-span-2" :disabled="creatingListing">
            {{ creatingListing ? "Publishing..." : "Publish listing" }}
          </button>
        </form>
      </div>
    </template>
  </section>
</template>
