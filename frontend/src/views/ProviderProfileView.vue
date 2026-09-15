<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useToastStore } from "@/stores/toast";
import ListingCard from "@/components/ListingCard.vue";
import type { BusinessContactDTO, BusinessReviewsDTO, ListingDTO, ProviderProfileDTO } from "@/lib/types";

const route = useRoute();
const auth = useAuthStore();
const followed = useFollowedProvidersStore();
const toast = useToastStore();

const profile = ref<ProviderProfileDTO | null>(null);
const listings = ref<ListingDTO[]>([]);
const similarBusinesses = ref<ProviderProfileDTO[]>([]);
const error = ref("");
const reportOpen = ref(false);
const reportReason = ref("MISREPRESENTATION");
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
    <RouterLink to="/" class="text-sm text-medium-grey hover:text-campus-teal">&larr; Back to browse</RouterLink>

    <div class="card space-y-5">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="flex items-start gap-4">
          <div
            v-if="profile.imageUrl"
            class="h-16 w-16 shrink-0 rounded-full border border-light-grey bg-cover bg-center"
            :style="{ backgroundImage: `url(${profile.imageUrl})` }"
          ></div>
          <div v-else class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-sky-blue/20 font-display text-xl font-bold text-uni-navy">
            {{ profile.businessName.charAt(0) }}
          </div>

          <div>
            <div class="mb-2 flex flex-wrap items-center gap-2">
              <h1 class="font-display text-2xl font-bold text-uni-navy sm:text-[26px]">{{ profile.businessName }}</h1>
              <span
                v-if="profile.verificationStatus === 'VERIFIED'"
                class="badge bg-success/15 text-success"
              >
                Verified
              </span>
              <span v-else class="badge bg-warning/15 text-warning">Pending verification</span>
              <span class="badge bg-academic-gold/20 text-uni-navy">{{ profile.category }}</span>
            </div>
            <p class="mb-3.5 text-sm text-medium-grey">Run by {{ profile.ownerFullName }}</p>
            <p class="max-w-xl text-sm leading-relaxed text-charcoal">{{ profile.description }}</p>
          </div>
        </div>

        <div class="flex shrink-0 flex-wrap gap-2">
          <button
            v-if="auth.isAuthenticated"
            class="inline-flex items-center gap-1.5 whitespace-nowrap rounded-control border px-4 py-2 text-sm font-semibold"
            :class="followed.isFollowing(profile.businessId)
              ? 'border-campus-teal bg-campus-teal/10 text-campus-teal'
              : 'border-uni-navy bg-white text-uni-navy hover:bg-soft-grey'"
            @click="followed.toggleFollow(profile)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" :fill="followed.isFollowing(profile.businessId) ? '#2A9BB4' : 'none'" stroke="currentColor" stroke-width="2">
              <path d="M12 21s-8-4.5-8-10.5A4.5 4.5 0 0 1 12 6a4.5 4.5 0 0 1 8 4.5C20 16.5 12 21 12 21Z" />
            </svg>
            {{ followed.isFollowing(profile.businessId) ? "Following" : "Follow" }}
          </button>
          <button
            v-if="auth.isAuthenticated"
            class="inline-flex items-center justify-center whitespace-nowrap rounded-control border border-uni-navy bg-white px-4 py-2 text-sm font-semibold text-uni-navy hover:bg-soft-grey"
            @click="openContact"
          >
            Contact
          </button>
          <button
            v-if="auth.isAuthenticated && !reportOpen"
            class="inline-flex items-center justify-center whitespace-nowrap rounded-control border border-danger bg-white px-4 py-2 text-sm font-semibold text-danger"
            @click="reportOpen = true"
          >
            Report this provider
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-7 border-t border-light-grey pt-4">
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ profile.activeListingCount }}</p>
          <p class="text-xs text-medium-grey">Active listings</p>
        </div>
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ profile.totalViews }}</p>
          <p class="text-xs text-medium-grey">Total views</p>
        </div>
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ formatDate(profile.memberSince) }}</p>
          <p class="text-xs text-medium-grey">Member since</p>
        </div>
      </div>
    </div>

    <div v-if="reportOpen" class="card space-y-3">
      <form class="space-y-3" @submit.prevent="submitReport">
        <select v-model="reportReason" class="input-field">
          <option value="MISREPRESENTATION">Misrepresentation</option>
          <option value="NON_DELIVERY">Non-delivery</option>
          <option value="INAPPROPRIATE_CONDUCT">Inappropriate conduct</option>
          <option value="SPAM">Spam</option>
          <option value="OTHER">Other</option>
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
      <RouterLink to="/login" class="text-campus-teal underline">Log in</RouterLink> to report a provider.
    </p>

    <div>
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">Listings from {{ profile.businessName }}</h2>

      <p v-if="listings.length === 0" class="card text-sm text-medium-grey">No active listings right now.</p>
      <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <ListingCard v-for="listing in listings" :key="listing.id" :listing="listing" />
      </div>
    </div>

    <div v-if="reviewsData" class="card space-y-4">
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
        <button v-if="auth.isAuthenticated" class="btn-secondary text-sm" @click="openReviewForm">
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
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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

    <div v-if="contactOpen" class="fixed inset-0 z-20 flex items-center justify-center bg-charcoal/40 p-6" @click.self="contactOpen = false">
      <div class="w-full max-w-sm rounded-modal border border-light-grey bg-white p-5 shadow-lg">
        <div class="mb-3 flex items-center justify-between">
          <h2 class="font-display text-base font-bold text-uni-navy">Contact {{ profile.businessName }}</h2>
          <button class="text-medium-grey" @click="contactOpen = false">&times;</button>
        </div>
        <p class="mb-4 text-xs text-medium-grey">Shown because you're signed in - please keep it campus-appropriate.</p>

        <p v-if="contactError" class="text-sm text-danger">{{ contactError }}</p>
        <template v-else-if="contact">
          <div class="space-y-2">
            <div class="flex items-center justify-between rounded-control border border-light-grey bg-soft-grey px-3 py-2.5">
              <div class="flex items-center gap-2">
                <span>✉️</span>
                <span class="text-sm text-charcoal">{{ contact.email }}</span>
              </div>
              <button class="text-xs font-semibold text-campus-teal" @click="copyContact(contact.email)">Copy</button>
            </div>
            <div v-if="contact.phoneNumber" class="flex items-center justify-between rounded-control border border-light-grey bg-soft-grey px-3 py-2.5">
              <div class="flex items-center gap-2">
                <span>📞</span>
                <span class="text-sm text-charcoal">{{ contact.phoneNumber }}</span>
              </div>
              <button class="text-xs font-semibold text-campus-teal" @click="copyContact(contact.phoneNumber)">Copy</button>
            </div>
          </div>
          <p class="mt-4 rounded-control bg-sky-blue/10 px-3 py-2 text-xs text-uni-navy">
            💬 In-app messaging isn't available yet - for now, reach out directly.
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
