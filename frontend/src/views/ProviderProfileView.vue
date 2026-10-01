<script setup lang="ts">
import { ArrowLeft, BadgeCheck, Flag, Mail, MapPin, MessageCircle, Phone, UserCheck, UserPlus } from "@lucide/vue";
import SellerTrustBadges from "@/components/SellerTrustBadges.vue";
import { categoryMeta } from "@/lib/categoryMeta";
import { REPORT_REASONS } from "@/lib/reports";
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useToastStore } from "@/stores/toast";
import ListingCard from "@/components/ListingCard.vue";
import BusinessPostsFeed from "@/components/posts/BusinessPostsFeed.vue";
import type { BusinessContactDTO, BusinessReviewsDTO, ListingDTO, ProviderProfileDTO, ReportReason, SellerTrust } from "@/lib/types";

const route = useRoute();
const auth = useAuthStore();
const followed = useFollowedProvidersStore();
const toast = useToastStore();

const profile = ref<ProviderProfileDTO | null>(null);
const listings = ref<ListingDTO[]>([]);
const similarBusinesses = ref<ProviderProfileDTO[]>([]);
const trust = ref<SellerTrust | null>(null);

// ---- Page tabs ----
type PageTab = "posts" | "listings" | "reviews";
const PAGE_TABS: { key: PageTab; label: string }[] = [
  { key: "posts", label: "Posts" },
  { key: "listings", label: "Listings" },
  { key: "reviews", label: "Reviews" },
];
// A shared post link opens straight on Posts; otherwise Posts is still the default, like a Facebook page.
const pageTab = ref<PageTab>("posts");
const postCount = ref<number | null>(null);

function tabCount(tab: PageTab): number | null {
  if (tab === "posts") return postCount.value;
  if (tab === "listings") return listings.value.length;
  return reviewsData.value?.total ?? null;
}
const error = ref("");
const reportOpen = ref(false);
const reportReason = ref<ReportReason>("PROHIBITED_ITEM");
const reportDetails = ref("");
const reportStatus = ref("");

const contactOpen = ref(false);
const contact = ref<BusinessContactDTO | null>(null);
const contactError = ref("");

async function openContact() {
  contactOpen.value = true;
  contactError.value = "";
  if (contact.value) return;
  try {
    const { data } = await api.get<BusinessContactDTO>(`/businesses/${route.params.businessId}/contact`);
    contact.value = data;
  } catch (err) {
    contactError.value = extractErrorMessage(err);
  }
}

async function copyContact(text: string) {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    // Clipboard access can be blocked - the value is still visible to copy manually.
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-ZA", { month: "short", year: "numeric" });
}

// ---- Reviews ----
const reviewsData = ref<BusinessReviewsDTO | null>(null);
const reviewFormOpen = ref(false);
const reviewRating = ref(5);
const reviewComment = ref("");
const reviewSubmitting = ref(false);
const reviewError = ref("");

const myReview = computed(() =>
  reviewsData.value?.reviews.find((r) => r.reviewerId === auth.user?.id) ?? null,
);

function starDistributionPercent(count: number): number {
  const total = reviewsData.value?.total ?? 0;
  if (total === 0) return 0;
  return Math.round((count / total) * 100);
}

async function loadReviews(businessId: string) {
  try {
    const { data } = await api.get<BusinessReviewsDTO>(`/businesses/${businessId}/reviews`);
    reviewsData.value = data;
  } catch {
    // Reviews are secondary to the profile itself; ignore failures here.
  }
}

function openReviewForm() {
  reviewFormOpen.value = true;
  reviewError.value = "";
  if (myReview.value) {
    reviewRating.value = myReview.value.rating;
    reviewComment.value = myReview.value.comment ?? "";
  } else {
    reviewRating.value = 5;
    reviewComment.value = "";
  }
}

async function submitReview() {
  if (!profile.value) return;
  reviewSubmitting.value = true;
  reviewError.value = "";
  try {
    await api.post(`/businesses/${profile.value.businessId}/reviews`, {
      rating: reviewRating.value,
      comment: reviewComment.value || null,
    });
    reviewFormOpen.value = false;
    toast.success("Review submitted!");
    await loadReviews(profile.value.businessId);
  } catch (err) {
    reviewError.value = extractErrorMessage(err);
  } finally {
    reviewSubmitting.value = false;
  }
}

async function flagReview(reviewId: string) {
  try {
    await api.post(`/reviews/${reviewId}/flag`);
  } catch {
    // Best-effort - no need to surface a failure for flagging.
  }
}

async function load() {
  const businessId = route.params.businessId as string;
  try {
    const [profileRes, listingsRes] = await Promise.all([
      api.get<ProviderProfileDTO>(`/businesses/${businessId}/profile`),
      api.get<ListingDTO[]>(`/listings/business/${businessId}`),
    ]);
    profile.value = profileRes.data;
    listings.value = listingsRes.data.filter((l) => l.status === "ACTIVE");

    api.get<SellerTrust>(`/businesses/${businessId}/trust`).then(({ data }) => (trust.value = data)).catch(() => {});
    const { data: similar } = await api.get<ProviderProfileDTO[]>(`/businesses/${businessId}/similar`);
    similarBusinesses.value = similar;

    await loadReviews(businessId);
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

async function submitReport() {
  if (!profile.value) return;
  reportStatus.value = "";
  try {
    await api.post("/reports", {
      targetType: "USER",
      targetId: profile.value.ownerId,
      reason: reportReason.value,
      details: reportDetails.value,
    });
    reportStatus.value = "Thanks - our team will review this shortly.";
    reportOpen.value = false;
    reportDetails.value = "";
  } catch (err) {
    reportStatus.value = extractErrorMessage(err);
  }
}

onMounted(load);
</script>

<template>
  <section v-if="profile" class="space-y-5">
    <RouterLink to="/providers" class="inline-flex items-center gap-1.5 text-sm font-medium text-medium-grey hover:text-uni-navy"><ArrowLeft class="h-4 w-4" /> All providers</RouterLink>

    <div class="card overflow-hidden p-0">
      <div class="relative h-28 bg-hero sm:h-36">
        <div class="bg-grid absolute inset-0 [mask-image:radial-gradient(ellipse_at_top_right,black,transparent_70%)]"></div>
      </div>
      <div class="space-y-5 px-5 pb-6 sm:px-7">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="flex flex-col items-start gap-4 sm:flex-row">
          <img
            v-if="profile.imageUrl"
            :src="profile.imageUrl"
            alt=""
            class="relative z-10 -mt-10 h-20 w-20 shrink-0 rounded-2xl bg-white object-cover shadow-lift ring-4 ring-white sm:-mt-12 sm:h-24 sm:w-24"
          />
          <div
            v-else
            class="relative z-10 -mt-10 flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl font-display text-2xl font-bold shadow-lift ring-4 ring-white sm:-mt-12 sm:h-24 sm:w-24"
            :class="categoryMeta(profile.category).tile"
          >
            {{ profile.businessName.charAt(0) }}
          </div>

          <div class="sm:pt-4">
            <div class="mb-2 flex flex-wrap items-center gap-2">
              <h1 class="font-display text-2xl font-bold text-uni-navy sm:text-[26px]">{{ profile.businessName }}</h1>
              <span
                v-if="profile.verificationStatus === 'VERIFIED'"
                class="badge bg-emerald-50 text-emerald-700"
              >
                <BadgeCheck class="h-3.5 w-3.5" /> Verified
              </span>
              <span v-else class="badge bg-amber-50 text-warning">Pending verification</span>
              <span class="badge" :class="categoryMeta(profile.category).tile">{{ profile.category }}</span>
            </div>
            <p class="text-sm text-medium-grey">Run by {{ profile.ownerFullName }}</p>
            <p v-if="profile.campusLabel" class="mt-1 flex items-center gap-1.5 text-sm text-charcoal">
              <MapPin class="h-4 w-4 shrink-0 text-teal-600" />
              Collect at {{ profile.pickupLocation ? `${profile.pickupLocation}, ` : "" }}{{ profile.campusLabel }} campus
            </p>
            <SellerTrustBadges v-if="trust" :trust="trust" class="mb-3.5 mt-3" />
            <div v-else class="mb-3.5"></div>
            <p class="max-w-xl text-sm leading-relaxed text-charcoal">{{ profile.description }}</p>
          </div>
        </div>

        <div class="flex shrink-0 flex-wrap gap-2 sm:pt-4">
          <button
            v-if="auth.isAuthenticated && !auth.isAdmin && profile.ownerId !== auth.user?.id"
            :class="followed.isFollowing(profile.businessId) ? 'btn border border-teal-500 bg-teal-50 text-teal-700' : 'btn-primary'"
            @click="followed.toggleFollow(profile)"
          >
            <component :is="followed.isFollowing(profile.businessId) ? UserCheck : UserPlus" class="h-4 w-4" />
            {{ followed.isFollowing(profile.businessId) ? "Following" : "Follow" }}
          </button>
          <button
            v-if="auth.isAuthenticated && profile.ownerId !== auth.user?.id"
            class="btn-secondary"
            @click="openContact"
          >
            <MessageCircle class="h-4 w-4" />
            Contact
          </button>
          <button
            v-if="auth.isAuthenticated && !auth.isAdmin && !reportOpen"
            class="btn-ghost px-3 hover:text-danger"
            aria-label="Report this provider"
            title="Report this provider"
            @click="reportOpen = true"
          >
            <Flag class="h-4 w-4" />
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-7 border-t border-light-grey pt-4">
        <div>
          <p class="text-xl font-semibold text-charcoal">{{ profile.activeListingCount }}</p>
          <p class="text-xs text-medium-grey">Active listings</p>
        </div>
        <div>
          <p class="text-xl font-semibold text-charcoal">{{ profile.totalViews }}</p>
          <p class="text-xs text-medium-grey">Total views</p>
        </div>
        <div>
          <p class="text-xl font-semibold text-charcoal">{{ formatDate(profile.memberSince) }}</p>
          <p class="text-xs text-medium-grey">Member since</p>
          </div>
    </div>
      </div>
    </div>

    <div v-if="reportOpen" class="card space-y-3">
      <form class="space-y-3" @submit.prevent="submitReport">
        <select v-model="reportReason" class="input-field">
          <option v-for="r in REPORT_REASONS" :key="r.value" :value="r.value">{{ r.label }}</option>
        </select>
        <textarea
          v-model="reportDetails"
          class="input-field"
          rows="3"
          placeholder="Tell us what happened (optional)"
        ></textarea>
        <div class="flex gap-2">
          <button type="submit" class="btn-primary text-sm">Submit report</button>
          <button type="button" class="btn-secondary text-sm" @click="reportOpen = false">Cancel</button>
        </div>
      </form>
    </div>
    <p v-if="reportStatus" class="text-sm text-medium-grey">{{ reportStatus }}</p>
    <p v-else-if="!auth.isAuthenticated" class="text-sm text-medium-grey">
      <RouterLink to="/login" class="text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600">Log in</RouterLink> to report a provider.
    </p>

    <!-- Facebook-style page tabs: posts first, because that's what's new. -->
    <div class="sticky top-16 z-20 -mx-4 border-b border-light-grey bg-soft-grey/90 px-4 backdrop-blur sm:mx-0 sm:rounded-control sm:border sm:bg-white sm:px-1.5 sm:py-1.5" role="tablist" aria-label="Business page sections">
      <div class="flex gap-1">
        <button
          v-for="t in PAGE_TABS"
          :key="t.key"
          role="tab"
          :aria-selected="pageTab === t.key"
          class="flex-1 border-b-2 px-3 py-3 text-sm font-semibold transition sm:flex-none sm:rounded-lg sm:border-b-0 sm:px-5 sm:py-2"
          :class="pageTab === t.key ? 'border-teal-500 text-uni-navy sm:bg-navy-50' : 'border-transparent text-medium-grey hover:text-uni-navy'"
          @click="pageTab = t.key"
        >
          {{ t.label }}
          <span v-if="tabCount(t.key) !== null" class="ml-1 text-xs font-normal text-medium-grey">{{ tabCount(t.key) }}</span>
        </button>
      </div>
    </div>

    <BusinessPostsFeed
      v-show="pageTab === 'posts'"
      :business-id="profile.businessId"
      :business-name="profile.businessName"
      :logo-url="profile.imageUrl"
      :owner-id="profile.ownerId"
      :verified="profile.verificationStatus === 'VERIFIED'"
      :listings="listings"
      @count="postCount = $event"
    />

    <div v-show="pageTab === 'listings'">
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Listings from {{ profile.businessName }}</h2>

      <p v-if="listings.length === 0" class="card text-sm text-medium-grey">No active listings right now.</p>
      <div v-else class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">
        <ListingCard v-for="listing in listings" :key="listing.id" :listing="listing" />
      </div>
    </div>

    <div v-if="reviewsData" v-show="pageTab === 'reviews'" class="card space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <div class="flex items-center gap-2">
            <span class="font-display text-2xl font-bold text-uni-navy">{{ reviewsData.average || "-" }}</span>
            <span class="text-xl text-academic-gold" v-if="reviewsData.total > 0">★★★★★</span>
          </div>
          <p class="text-xs text-medium-grey">
            {{ reviewsData.total === 0 ? "No reviews yet" : `Based on ${reviewsData.total} review${reviewsData.total === 1 ? "" : "s"}` }}
          </p>
        </div>
        <button v-if="auth.isAuthenticated && !auth.isAdmin && profile.ownerId !== auth.user?.id" class="btn-secondary text-sm" @click="openReviewForm">
          {{ myReview ? "Edit your review" : "Leave a review" }}
        </button>
      </div>

      <div v-if="reviewsData.total > 0" class="space-y-1.5">
        <div v-for="star in [5, 4, 3, 2, 1]" :key="star" class="flex items-center gap-2 text-xs">
          <span class="w-4 text-medium-grey">{{ star }}</span>
          <div class="h-2 flex-1 overflow-hidden rounded-full bg-soft-grey">
            <div class="h-full rounded-full bg-academic-gold" :style="{ width: `${starDistributionPercent(reviewsData.distribution[star] ?? 0)}%` }"></div>
          </div>
          <span class="w-6 text-right text-medium-grey">{{ reviewsData.distribution[star] ?? 0 }}</span>
        </div>
      </div>

      <div v-if="reviewFormOpen" class="space-y-2 rounded-control border border-campus-teal bg-white p-3">
        <p class="text-xs font-semibold text-medium-grey">Your rating</p>
        <div class="flex gap-1 text-2xl text-academic-gold">
          <button v-for="n in 5" :key="n" type="button" @click="reviewRating = n">
            <span :class="n <= reviewRating ? '' : 'text-light-grey'">★</span>
          </button>
        </div>
        <textarea v-model="reviewComment" class="input-field" rows="3" placeholder="How was your experience?"></textarea>
        <p v-if="reviewError" class="text-sm text-danger">{{ reviewError }}</p>
        <div class="flex justify-end gap-2">
          <button class="btn-secondary text-sm" @click="reviewFormOpen = false">Cancel</button>
          <button class="btn-primary text-sm" :disabled="reviewSubmitting" @click="submitReview">
            {{ reviewSubmitting ? "Posting..." : "Post review" }}
          </button>
        </div>
      </div>

      <div v-if="reviewsData.reviews.length > 0" class="space-y-3 border-t border-light-grey pt-3">
        <div v-for="r in reviewsData.reviews" :key="r.id" class="border-t border-light-grey pt-3 first:border-t-0 first:pt-0">
          <div class="flex items-center justify-between">
            <span class="text-sm font-semibold text-uni-navy">{{ r.reviewerName }}</span>
            <span class="text-academic-gold">
              <template v-for="n in 5" :key="n"><span :class="n <= r.rating ? '' : 'text-light-grey'">★</span></template>
            </span>
          </div>
          <p v-if="r.comment" class="mt-1 text-sm text-charcoal">{{ r.comment }}</p>
          <div class="mt-1 flex items-center justify-between">
            <p class="text-xs text-medium-grey">{{ formatDate(r.createdAt) }}</p>
            <button v-if="auth.isAuthenticated && r.reviewerId !== auth.user?.id" class="text-xs text-medium-grey hover:text-danger" @click="flagReview(r.id)">
              Flag
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="similarBusinesses.length > 0">
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Similar businesses in {{ profile.category }}</h2>
      <div class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">
        <RouterLink
          v-for="b in similarBusinesses"
          :key="b.businessId"
          :to="`/providers/${b.businessId}`"
          class="card space-y-2 transition hover:shadow-md"
        >
          <div class="flex items-center justify-between">
            <h3 class="font-display text-sm font-semibold text-uni-navy">{{ b.businessName }}</h3>
            <span
              class="badge"
              :class="b.verificationStatus === 'VERIFIED' ? 'bg-success/15 text-success' : 'bg-warning/15 text-warning'"
            >
              {{ b.verificationStatus === "VERIFIED" ? "Verified" : "Pending" }}
            </span>
          </div>
          <p class="text-xs text-medium-grey">{{ b.activeListingCount }} active listing{{ b.activeListingCount === 1 ? "" : "s" }}</p>
        </RouterLink>
      </div>
    </div>

    <div v-if="contactOpen" class="modal-backdrop" @click.self="contactOpen = false">
      <div v-dialog="() => (contactOpen = false)" class="modal-panel max-w-sm" role="dialog" aria-modal="true" :aria-label="`Contact ${profile.businessName}`">
        <div class="mb-3 flex items-center justify-between">
          <h2 class="font-display text-base font-bold text-uni-navy">Contact {{ profile.businessName }}</h2>
          <button class="text-medium-grey" aria-label="Close" @click="contactOpen = false">&times;</button>
        </div>
        <p class="mb-4 text-xs text-medium-grey">Shown because you're signed in - please keep it campus-appropriate.</p>

        <p v-if="contactError" class="text-sm text-danger">{{ contactError }}</p>
        <template v-else-if="contact">
          <div class="space-y-2">
            <div class="flex items-center justify-between rounded-control border border-light-grey bg-soft-grey px-3 py-2.5">
              <div class="flex items-center gap-2">
                <Mail class="h-4 w-4 text-medium-grey" />
                <span class="text-sm text-charcoal">{{ contact.email }}</span>
              </div>
              <button class="text-xs font-semibold text-campus-teal" @click="copyContact(contact.email)">Copy</button>
            </div>
            <div v-if="contact.phoneNumber" class="flex items-center justify-between rounded-control border border-light-grey bg-soft-grey px-3 py-2.5">
              <div class="flex items-center gap-2">
                <Phone class="h-4 w-4 text-medium-grey" />
                <span class="text-sm text-charcoal">{{ contact.phoneNumber }}</span>
              </div>
              <button class="text-xs font-semibold text-campus-teal" @click="copyContact(contact.phoneNumber)">Copy</button>
            </div>
          </div>
          <p class="mt-4 flex items-center gap-2 rounded-control bg-teal-50 px-3 py-2 text-xs text-navy-800">
            <MessageCircle class="h-4 w-4 shrink-0 text-teal-600" /> You can also message them in-app from any of their listings.
          </p>
        </template>
        <p v-else class="text-sm text-medium-grey">Loading...</p>

        <button class="btn-primary mt-4 w-full text-sm" @click="contactOpen = false">Close</button>
      </div>
    </div>
  </section>

  <p v-else-if="error" class="text-sm text-danger">{{ error }}</p>
  <p v-else class="text-sm text-medium-grey">Loading...</p>
</template>
