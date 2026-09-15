<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useCartStore } from "@/stores/cart";
import { recordView } from "@/lib/recentlyViewed";
import ListingCard from "@/components/ListingCard.vue";
import type { BusinessDTO, ListingDTO, PromoCodeDTO, QuestionView } from "@/lib/types";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const saved = useSavedListingsStore();
const cart = useCartStore();

const listing = ref<ListingDTO | null>(null);
const business = ref<BusinessDTO | null>(null);
const moreFromSeller = ref<ListingDTO[]>([]);
const activePromo = ref<PromoCodeDTO | null>(null);
const error = ref("");
const reportOpen = ref(false);
const reportReason = ref("MISREPRESENTATION");
const reportDetails = ref("");
const reportStatus = ref("");

const shareOpen = ref(false);
const linkCopied = ref(false);
const shareUrl = `${window.location.origin}/listings/${route.params.id}`;

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareUrl);
    linkCopied.value = true;
    setTimeout(() => (linkCopied.value = false), 2000);
  } catch {
    // Clipboard access can be blocked (permissions, insecure context) - the link is still shown to copy manually.
  }
}

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

function relativeDays(iso: string): string {
  const days = Math.round((Date.now() - new Date(iso).getTime()) / 86400000);
  if (days < 1) return "Listed today";
  if (days === 1) return "Listed 1 day ago";
  return `Listed ${days} days ago`;
}

async function load() {
  try {
    const { data } = await api.get<ListingDTO>(`/listings/${route.params.id}`);
    listing.value = data;
    recordView(data);
    const { data: businessData } = await api.get<BusinessDTO>(`/businesses/${data.businessId}`);
    business.value = businessData;

    const { data: businessListings } = await api.get<ListingDTO[]>(`/listings/business/${data.businessId}`);
    moreFromSeller.value = businessListings
      .filter((l) => l.id !== data.id && l.status === "ACTIVE")
      .slice(0, 4);

    try {
      const { data: promo } = await api.get<PromoCodeDTO | null>(`/listings/${data.id}/promo`);
      activePromo.value = promo;
    } catch {
      // No active promo code is the common case; ignore failures here.
    }

    loadQuestions();
  } catch (err) {
    error.value = extractErrorMessage(err);
  }
}

// ---- Message the seller ----
const messageOpen = ref(false);
const messageBody = ref("");
const messageSending = ref(false);
const messageStatus = ref("");

async function sendMessage() {
  if (!listing.value || !messageBody.value.trim()) return;
  messageSending.value = true;
  messageStatus.value = "";
  try {
    const { data } = await api.post<{ id: string }>("/conversations", {
      listingId: listing.value.id,
      body: messageBody.value,
    });
    messageOpen.value = false;
    router.push(`/messages/${data.id}`);
  } catch (err) {
    messageStatus.value = extractErrorMessage(err);
  } finally {
    messageSending.value = false;
  }
}

// ---- Add to cart (products) ----
const addedToCart = ref(false);

function addToCart() {
  if (!listing.value) return;
  cart.add(listing.value, 1);
  addedToCart.value = true;
  setTimeout(() => (addedToCart.value = false), 2500);
}

// ---- Listing Q&A ----
const questions = ref<QuestionView[]>([]);
const questionDraft = ref("");
const questionSubmitting = ref(false);

async function loadQuestions() {
  if (!listing.value) return;
  try {
    const { data } = await api.get<QuestionView[]>(`/listings/${listing.value.id}/questions`);
    questions.value = data;
  } catch {
    // Q&A is a nice-to-have; ignore failures here.
  }
}

async function submitQuestion() {
  if (!listing.value || !questionDraft.value.trim()) return;
  questionSubmitting.value = true;
  try {
    await api.post(`/listings/${listing.value.id}/questions`, { questionText: questionDraft.value });
    questionDraft.value = "";
    await loadQuestions();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    questionSubmitting.value = false;
  }
}

// ---- Request a booking (services) ----
const bookingOpen = ref(false);
const bookingDate = ref("");
const bookingTime = ref("12:00");
const bookingNote = ref("");
const bookingSubmitting = ref(false);
const bookingStatus = ref("");

async function submitBooking() {
  if (!listing.value || !bookingDate.value) return;
  bookingSubmitting.value = true;
  bookingStatus.value = "";
  try {
    await api.post("/bookings", {
      listingId: listing.value.id,
      preferredAt: `${bookingDate.value}T${bookingTime.value}:00`,
      note: bookingNote.value || null,
    });
    bookingStatus.value = "Request sent - you'll get a notification once the seller responds.";
    bookingOpen.value = false;
    bookingNote.value = "";
  } catch (err) {
    bookingStatus.value = extractErrorMessage(err);
  } finally {
    bookingSubmitting.value = false;
  }
}

// ---- Notify me when back in stock (sold-out products) ----
const notifySubscribed = ref(false);
const notifyLoading = ref(false);

async function toggleNotifyMe() {
  if (!listing.value) return;
  notifyLoading.value = true;
  try {
    const { data } = await api.post<{ subscribed: boolean }>(`/listings/${listing.value.id}/notify-me`);
    notifySubscribed.value = data.subscribed;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    notifyLoading.value = false;
  }
}

async function submitReport() {
  if (!listing.value) return;
  reportStatus.value = "";
  try {
    await api.post("/reports", {
      targetType: "LISTING",
      targetId: listing.value.id,
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
  <section v-if="listing" class="mx-auto max-w-2xl space-y-4">
    <div class="card overflow-hidden p-0">
      <img v-if="listing.imageUrl" :src="listing.imageUrl" alt="" class="h-56 w-full object-cover" />
      <div class="space-y-3 p-4">
      <div class="flex items-start justify-between gap-2">
        <h1 class="font-display text-2xl font-bold text-uni-navy">{{ listing.name }}</h1>
        <div class="flex shrink-0 items-center gap-2">
          <span v-if="listing.status === 'SOLD_OUT'" class="badge bg-medium-grey/15 text-medium-grey">Sold out</span>
          <span v-else-if="listing.status === 'INACTIVE'" class="badge bg-danger/15 text-danger">Inactive</span>
          <span class="badge bg-sky-blue/20 text-uni-navy">{{ listing.type }}</span>
          <button
            v-if="auth.isAuthenticated"
            class="flex h-8 w-8 items-center justify-center rounded-full border border-light-grey"
            :aria-label="saved.isSaved(listing.id) ? 'Unsave listing' : 'Save listing'"
            @click="saved.toggleSave(listing)"
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              :fill="saved.isSaved(listing.id) ? '#DC2626' : 'none'"
              stroke="#DC2626"
              stroke-width="2"
            >
              <path
                d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z"
              />
            </svg>
          </button>
          <div class="relative">
            <button
              class="flex h-8 w-8 items-center justify-center rounded-full border border-light-grey"
              aria-label="Share listing"
              @click="shareOpen = !shareOpen"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#163D72" stroke-width="2">
                <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
                <path d="M8.6 13.5 15.4 17.5M15.4 6.5 8.6 10.5" />
              </svg>
            </button>

            <div v-if="shareOpen" class="absolute right-0 top-[calc(100%+8px)] z-10 w-72 rounded-card border border-light-grey bg-white p-3 shadow-md">
              <p class="mb-2 text-xs font-semibold text-medium-grey">Share this listing</p>
              <div class="flex items-center gap-2 rounded-control border border-light-grey bg-soft-grey px-2.5 py-2">
                <span class="flex-1 truncate text-xs text-charcoal">{{ shareUrl }}</span>
                <button class="btn-primary px-2.5 py-1 text-xs" @click="copyShareLink">Copy</button>
              </div>
              <p v-if="linkCopied" class="mt-1.5 text-xs font-medium text-success">Link copied to clipboard!</p>
            </div>
          </div>
        </div>
      </div>
      <p class="text-sm text-medium-grey">{{ listing.category }}</p>
      <RouterLink
        v-if="business"
        :to="`/providers/${business.id}`"
        class="inline-flex w-fit items-center gap-1.5 text-sm text-charcoal hover:text-campus-teal"
      >
        Sold by <span class="font-medium underline">{{ business.businessName }}</span>
        <span v-if="business.verificationStatus === 'VERIFIED'" class="badge bg-success/15 text-success">Verified</span>
      </RouterLink>
      <p class="whitespace-pre-line text-charcoal">{{ listing.description }}</p>

      <div v-if="activePromo" class="flex items-center gap-2">
        <span class="badge bg-danger text-white">{{ activePromo.discountType === "PERCENT" ? `${activePromo.discountValue}% OFF` : `R${activePromo.discountValue} OFF` }} &middot; {{ activePromo.code }}</span>
      </div>
      <div class="flex items-center gap-2">
        <span v-if="activePromo" class="text-lg text-medium-grey line-through">{{ formatPrice(listing.price) }}</span>
        <p
          class="font-display text-2xl font-semibold"
          :class="activePromo ? 'text-success' : 'text-campus-teal'"
        >
          {{ formatPrice(activePromo ? (activePromo.discountType === "PERCENT" ? listing.price * (1 - activePromo.discountValue / 100) : Math.max(listing.price - activePromo.discountValue, 0)) : listing.price) }}
        </p>
      </div>

      <dl v-if="listing.type === 'PRODUCT'" class="text-sm text-medium-grey">
        <dt class="inline font-medium">In stock:</dt>
        <dd class="inline"> {{ listing.stockQuantity ?? "N/A" }}</dd>
      </dl>
      <dl v-else class="text-sm text-medium-grey">
        <dt class="inline font-medium">Availability:</dt>
        <dd class="inline"> {{ listing.availabilitySchedule ?? "Contact seller" }}</dd>
      </dl>

      <button
        v-if="listing.type === 'SERVICE' && listing.status === 'ACTIVE' && auth.isAuthenticated"
        class="btn-primary w-full text-sm"
        @click="bookingOpen = true; bookingStatus = ''"
      >
        Request a booking
      </button>
      <RouterLink v-else-if="listing.type === 'SERVICE' && listing.status === 'ACTIVE'" to="/login" class="btn-secondary block w-full text-center text-sm">
        Log in to request a booking
      </RouterLink>
      <p v-if="bookingStatus" class="text-sm text-success">{{ bookingStatus }}</p>

      <button
        v-if="listing.type === 'PRODUCT' && listing.status === 'ACTIVE' && auth.isAuthenticated"
        class="btn-primary w-full text-sm"
        @click="addToCart"
      >
        {{ addedToCart ? "Added to cart!" : "Add to cart" }}
      </button>
      <RouterLink v-else-if="listing.type === 'PRODUCT' && listing.status === 'ACTIVE'" to="/login" class="btn-secondary block w-full text-center text-sm">
        Log in to add to cart
      </RouterLink>

      <button
        v-if="auth.isAuthenticated && business && business.ownerId !== auth.user?.id"
        class="btn-secondary flex w-full items-center justify-center gap-2 text-sm"
        @click="messageOpen = true; messageStatus = ''; messageBody = ''"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" />
        </svg>
        Message {{ business.businessName }}
      </button>

      <template v-if="listing.type === 'PRODUCT' && listing.status === 'SOLD_OUT' && auth.isAuthenticated">
        <button
          class="inline-flex w-full items-center justify-center gap-2 rounded-control border-2 px-4 py-2.5 text-sm font-semibold disabled:opacity-60"
          :class="notifySubscribed ? 'border-campus-teal bg-campus-teal/10 text-campus-teal' : 'border-light-grey text-charcoal'"
          :disabled="notifyLoading"
          @click="toggleNotifyMe"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
          {{ notifySubscribed ? "We'll notify you when back in stock" : "Notify me when back in stock" }}
        </button>
      </template>

      <div class="flex flex-wrap items-center gap-4 border-t border-light-grey pt-3 text-xs text-medium-grey">
        <span class="inline-flex items-center gap-1.5">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#8A94A6" stroke-width="2">
            <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z" /><circle cx="12" cy="12" r="3" />
          </svg>
          {{ listing.viewCount }} views
        </span>
        <span class="inline-flex items-center gap-1.5">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="#DC2626">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
          </svg>
          {{ listing.savedCount }} student{{ listing.savedCount === 1 ? "" : "s" }} saved this
        </span>
        <span>{{ relativeDays(listing.createdAt) }}</span>
      </div>
      </div>
    </div>

    <!-- More from this seller -->
    <div v-if="business && moreFromSeller.length > 0">
      <h2 class="mb-3 font-display text-lg font-semibold text-uni-navy">More from {{ business.businessName }}</h2>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <ListingCard v-for="l in moreFromSeller" :key="l.id" :listing="l" />
      </div>
    </div>

    <!-- Questions & answers -->
    <div class="card space-y-3">
      <div>
        <h2 class="font-display text-lg font-semibold text-uni-navy">Questions &amp; answers</h2>
        <p class="text-xs text-medium-grey">Ask the seller anything - your question and their answer are visible to everyone.</p>
      </div>

      <div v-if="auth.isAuthenticated" class="flex gap-2">
        <input v-model="questionDraft" class="input-field" placeholder="Ask a question about this listing..." @keyup.enter="submitQuestion" />
        <button class="btn-secondary shrink-0 text-sm" :disabled="questionSubmitting || !questionDraft.trim()" @click="submitQuestion">
          {{ questionSubmitting ? "Posting..." : "Post" }}
        </button>
      </div>
      <p v-else class="text-sm text-medium-grey">
        <RouterLink to="/login" class="text-campus-teal underline">Log in</RouterLink> to ask a question.
      </p>

      <p v-if="questions.length === 0" class="text-sm text-medium-grey">No questions yet - be the first to ask.</p>
      <div v-else class="space-y-3 border-t border-light-grey pt-3">
        <div v-for="q in questions" :key="q.id">
          <p class="text-sm text-charcoal"><span class="font-semibold">{{ q.askerName }}</span> asked</p>
          <p class="text-sm text-charcoal">{{ q.questionText }}</p>
          <div v-if="q.answerText" class="mt-2 flex gap-2 rounded-control bg-campus-teal/5 p-3">
            <div class="flex-1">
              <p class="text-xs font-semibold text-uni-navy">
                {{ business?.businessName }} <span class="ml-1 rounded-full bg-uni-navy/10 px-1.5 py-0.5 text-[10px] font-semibold">Seller</span>
              </p>
              <p class="mt-0.5 text-sm text-charcoal">{{ q.answerText }}</p>
            </div>
          </div>
          <span v-else class="badge mt-1.5 bg-warning/15 text-warning">Awaiting seller reply</span>
        </div>
      </div>
    </div>

    <div v-if="auth.isAuthenticated" class="card">
      <button v-if="!reportOpen" class="btn-secondary text-sm" @click="reportOpen = true">
        Report this listing
      </button>
      <form v-else class="space-y-3" @submit.prevent="submitReport">
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
      <p v-if="reportStatus" class="mt-2 text-sm text-medium-grey">{{ reportStatus }}</p>
    </div>
    <p v-else class="text-sm text-medium-grey">
      <RouterLink to="/login" class="text-campus-teal underline">Log in</RouterLink> to report a listing.
    </p>

    <div v-if="bookingOpen" class="fixed inset-0 z-20 flex items-center justify-center bg-charcoal/40 p-6" @click.self="bookingOpen = false">
      <div class="w-full max-w-sm rounded-modal border border-light-grey bg-white p-5 shadow-lg">
        <div class="mb-1 flex items-center justify-between">
          <h2 class="font-display text-base font-bold text-uni-navy">Request a booking</h2>
          <button class="text-medium-grey" @click="bookingOpen = false">&times;</button>
        </div>
        <p class="mb-4 text-xs text-medium-grey">
          {{ business?.businessName }} will accept or decline - you'll get a notification either way.
        </p>

        <label class="mb-1 block text-xs font-medium text-medium-grey">Preferred date</label>
        <input v-model="bookingDate" type="date" class="input-field" :min="new Date().toISOString().slice(0, 10)" />

        <label class="mb-1 mt-3 block text-xs font-medium text-medium-grey">Preferred time</label>
        <input v-model="bookingTime" type="time" class="input-field" />

        <label class="mb-1 mt-3 block text-xs font-medium text-medium-grey">Note (optional)</label>
        <textarea v-model="bookingNote" class="input-field" rows="3" placeholder="Anything the seller should know..."></textarea>

        <div class="mt-4 flex gap-2">
          <button class="btn-secondary flex-1 text-sm" @click="bookingOpen = false">Cancel</button>
          <button class="btn-primary flex-1 text-sm" :disabled="bookingSubmitting || !bookingDate" @click="submitBooking">
            {{ bookingSubmitting ? "Sending..." : "Send request" }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="messageOpen" class="fixed inset-0 z-20 flex items-center justify-center bg-charcoal/40 p-6" @click.self="messageOpen = false">
      <div class="w-full max-w-sm rounded-modal border border-light-grey bg-white p-5 shadow-lg">
        <div class="mb-1 flex items-center justify-between">
          <h2 class="font-display text-base font-bold text-uni-navy">Message {{ business?.businessName }}</h2>
          <button class="text-medium-grey" @click="messageOpen = false">&times;</button>
        </div>
        <p class="mb-3 text-xs text-medium-grey">About: {{ listing?.name }}</p>

        <textarea v-model="messageBody" class="input-field resize-none" rows="4" placeholder="Hi! I'd like to ask about..."></textarea>
        <p v-if="messageStatus" class="mt-2 text-sm text-danger">{{ messageStatus }}</p>

        <div class="mt-4 flex gap-2">
          <button class="btn-secondary flex-1 text-sm" @click="messageOpen = false">Cancel</button>
          <button class="btn-primary flex-1 text-sm" :disabled="messageSending || !messageBody.trim()" @click="sendMessage">
            {{ messageSending ? "Sending..." : "Send message" }}
          </button>
        </div>
      </div>
    </div>
  </section>

  <p v-else-if="error" class="text-sm text-danger">{{ error }}</p>
  <p v-else class="text-sm text-medium-grey">Loading...</p>
</template>
