<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useCartStore } from "@/stores/cart";
import { useToastStore } from "@/stores/toast";
import { recordView } from "@/lib/recentlyViewed";
import {
  BadgeCheck,
  Bell,
  CalendarClock,
  Check,
  ChevronRight,
  Clock3,
  Copy,
  Eye,
  Flag,
  Heart,
  MessageCircle,
  Package,
  Share2,
  ShoppingBag,
  TicketPercent,
  X,
} from "@lucide/vue";
import ListingCard from "@/components/ListingCard.vue";
import { categoryMeta } from "@/lib/categoryMeta";
import { formatPrice, initials, relativeTime } from "@/lib/format";
import type { BusinessDTO, ListingDTO, PromoCodeDTO, QuestionView } from "@/lib/types";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const saved = useSavedListingsStore();
const cart = useCartStore();
const toast = useToastStore();

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
    toast.success("Message sent!");
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
  toast.success("Added to cart", listing.value.name);
  setTimeout(() => (addedToCart.value = false), 2500);
}

// ---- Listing Q&A ----
const QUESTION_MAX_LENGTH = 1000;
const questions = ref<QuestionView[]>([]);
const questionDraft = ref("");
const questionSubmitting = ref(false);
const flaggingQuestionId = ref<string | null>(null);
const flaggedQuestionIds = ref<Set<string>>(new Set());

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
    await api.post(`/listings/${listing.value.id}/questions`, {
      questionText: questionDraft.value.slice(0, QUESTION_MAX_LENGTH),
    });
    questionDraft.value = "";
    toast.success("Question posted!");
    await loadQuestions();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    questionSubmitting.value = false;
  }
}

async function flagQuestion(id: string) {
  flaggingQuestionId.value = id;
  try {
    await api.post(`/questions/${id}/flag`);
    flaggedQuestionIds.value.add(id);
    toast.info("Question flagged", "Thanks - our team will take a look.");
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    flaggingQuestionId.value = null;
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
    toast.success("Booking requested!", "You'll get a notification once the seller responds.");
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

const imageFailed = ref(false);
const meta = computed(() => categoryMeta(listing.value?.category));

const discountedPrice = computed(() => {
  const l = listing.value;
  const promo = activePromo.value;
  if (!l || !promo) return null;
  return promo.discountType === "PERCENT"
    ? l.price * (1 - promo.discountValue / 100)
    : Math.max(l.price - promo.discountValue, 0);
});

const isOwnListing = computed(() => !!business.value && business.value.ownerId === auth.user?.id);

const promoCopied = ref(false);
async function copyPromo() {
  if (!activePromo.value) return;
  try {
    await navigator.clipboard.writeText(activePromo.value.code);
    promoCopied.value = true;
    setTimeout(() => (promoCopied.value = false), 2000);
  } catch {
    // Clipboard blocked - the code is still visible to type in at checkout.
  }
}

onMounted(load);
</script>

<template>
  <section v-if="listing" class="space-y-12">
    <nav class="flex items-center gap-1.5 text-xs text-medium-grey" aria-label="Breadcrumb">
      <RouterLink to="/" class="hover:text-uni-navy">Explore</RouterLink>
      <ChevronRight class="h-3.5 w-3.5" />
      <RouterLink :to="{ path: '/', query: { category: listing.category } }" class="hover:text-uni-navy">{{ listing.category }}</RouterLink>
      <ChevronRight class="h-3.5 w-3.5" />
      <span class="truncate font-medium text-charcoal">{{ listing.name }}</span>
    </nav>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)] lg:gap-10">
      <!-- Media -->
      <div class="relative overflow-hidden rounded-modal bg-soft-grey shadow-card ring-1 ring-light-grey/80 lg:col-start-1">
        <img
          v-if="listing.imageUrl && !imageFailed"
          :src="listing.imageUrl"
          :alt="listing.name"
          class="aspect-[4/3] w-full object-cover"
          @error="imageFailed = true"
        />
        <div v-else class="flex aspect-[4/3] w-full items-center justify-center" :style="{ background: meta.gradient }">
          <component :is="meta.icon" class="h-24 w-24 text-navy-900/15" :stroke-width="1.25" />
        </div>
        <div class="absolute left-4 top-4 flex gap-2">
          <span class="badge bg-white/90 py-1 text-navy-700 shadow-xs backdrop-blur">
            <component :is="listing.type === 'PRODUCT' ? Package : CalendarClock" class="h-3.5 w-3.5" />
            {{ listing.type === "PRODUCT" ? "Product" : "Service" }}
          </span>
          <span v-if="listing.status === 'SOLD_OUT'" class="badge bg-navy-900/85 py-1 text-white">Sold out</span>
          <span v-else-if="listing.status === 'INACTIVE'" class="badge bg-danger py-1 text-white">Inactive</span>
        </div>
      </div>

      <!-- Purchase panel (second on mobile, sticky right column on desktop) -->
      <div class="lg:col-start-2 lg:row-span-2 lg:row-start-1">
        <div class="space-y-4 lg:sticky lg:top-24">
          <div class="card space-y-5 p-5 sm:p-6">
            <div>
              <p class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-medium-grey">
                <span class="flex h-5 w-5 items-center justify-center rounded-md" :class="meta.tile"><component :is="meta.icon" class="h-3 w-3" /></span>
                {{ listing.category }}
              </p>
              <h1 class="mt-2 font-display text-2xl font-bold leading-tight text-uni-navy sm:text-3xl">{{ listing.name }}</h1>
              <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-medium-grey">
                <span class="inline-flex items-center gap-1"><Eye class="h-3.5 w-3.5" /> {{ listing.viewCount }} views</span>
                <span class="inline-flex items-center gap-1"><Heart class="h-3.5 w-3.5" /> {{ listing.savedCount }} saved</span>
                <span class="inline-flex items-center gap-1"><Clock3 class="h-3.5 w-3.5" /> {{ relativeDays(listing.createdAt) }}</span>
              </div>
            </div>

            <div class="rounded-card bg-soft-grey/80 p-4">
              <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <p class="font-display text-3xl font-bold" :class="discountedPrice !== null ? 'text-success' : 'text-uni-navy'">
                  {{ formatPrice(discountedPrice ?? listing.price) }}
                </p>
                <p v-if="discountedPrice !== null" class="text-base text-medium-grey line-through">{{ formatPrice(listing.price) }}</p>
              </div>
              <button
                v-if="activePromo"
                class="mt-3 flex w-full items-center gap-2 rounded-control border border-dashed border-emerald-300 bg-emerald-50 px-3 py-2 text-left text-xs text-emerald-800 transition hover:bg-emerald-100"
                @click="copyPromo"
              >
                <TicketPercent class="h-4 w-4 shrink-0" />
                <span class="flex-1">
                  <span class="font-bold">{{ activePromo.discountType === "PERCENT" ? `${activePromo.discountValue}% off` : `R${activePromo.discountValue} off` }}</span>
                  with code <span class="font-mono font-bold">{{ activePromo.code }}</span>
                </span>
                <component :is="promoCopied ? Check : Copy" class="h-3.5 w-3.5 shrink-0" />
              </button>
              <p class="mt-3 text-sm text-charcoal">
                <template v-if="listing.type === 'PRODUCT'">
                  <span class="font-semibold">{{ listing.stockQuantity ?? "N/A" }}</span> in stock
                </template>
                <template v-else>
                  <span v-if="listing.durationMinutes" class="font-semibold">{{ listing.durationMinutes }} min session · </span>
                  {{ listing.availabilitySchedule ?? "Contact seller for availability" }}
                </template>
              </p>
            </div>

            <div class="space-y-2.5">
              <template v-if="listing.status === 'ACTIVE' && !isOwnListing">
                <template v-if="listing.type === 'SERVICE'">
                  <button v-if="auth.isAuthenticated" class="btn-primary w-full py-3" @click="bookingOpen = true; bookingStatus = ''">
                    <CalendarClock class="h-4 w-4" /> Request a booking
                  </button>
                  <RouterLink v-else :to="{ path: '/login', query: { redirect: $route.fullPath } }" class="btn-primary w-full py-3">Log in to book</RouterLink>
                </template>
                <template v-else>
                  <button v-if="auth.isAuthenticated" class="btn-primary w-full py-3" @click="addToCart">
                    <component :is="addedToCart ? Check : ShoppingBag" class="h-4 w-4" /> {{ addedToCart ? "Added to cart" : "Add to cart" }}
                  </button>
                  <RouterLink v-else :to="{ path: '/login', query: { redirect: $route.fullPath } }" class="btn-primary w-full py-3">Log in to buy</RouterLink>
                </template>
              </template>
              <p v-if="isOwnListing" class="rounded-control bg-navy-50 px-3 py-2.5 text-center text-sm text-navy-700">This is your listing.</p>
              <p v-if="bookingStatus" class="text-sm text-success">{{ bookingStatus }}</p>

              <button
                v-if="listing.type === 'PRODUCT' && listing.status === 'SOLD_OUT' && auth.isAuthenticated"
                class="btn w-full border py-3"
                :class="notifySubscribed ? 'border-teal-500 bg-teal-50 text-teal-700' : 'border-light-grey bg-white text-charcoal hover:bg-soft-grey'"
                :disabled="notifyLoading"
                @click="toggleNotifyMe"
              >
                <Bell class="h-4 w-4" /> {{ notifySubscribed ? "We'll notify you when it's back" : "Notify me when back in stock" }}
              </button>

              <div class="flex gap-2">
                <button
                  v-if="auth.isAuthenticated && business && !isOwnListing"
                  class="btn-secondary flex-1"
                  @click="messageOpen = true; messageStatus = ''; messageBody = ''"
                >
                  <MessageCircle class="h-4 w-4" /> <span class="max-[359px]:hidden">Message seller</span><span class="min-[360px]:hidden">Message</span>
                </button>
                <button
                  v-if="auth.isAuthenticated"
                  class="btn-secondary px-3"
                  :aria-label="saved.isSaved(listing.id) ? 'Unsave listing' : 'Save listing'"
                  :aria-pressed="saved.isSaved(listing.id)"
                  @click="saved.toggleSave(listing)"
                >
                  <Heart class="h-4 w-4" :class="saved.isSaved(listing.id) ? 'fill-danger text-danger' : ''" />
                </button>
                <div class="relative">
                  <button class="btn-secondary px-3" aria-label="Share listing" @click="shareOpen = !shareOpen">
                    <Share2 class="h-4 w-4" />
                  </button>
                  <div v-if="shareOpen" class="absolute right-0 top-[calc(100%+8px)] z-20 w-72 animate-scale-in rounded-card border border-light-grey bg-white p-3 shadow-pop">
                    <p class="mb-2 text-xs font-semibold text-medium-grey">Share this listing</p>
                    <div class="flex items-center gap-2 rounded-control border border-light-grey bg-soft-grey px-2.5 py-1.5">
                      <span class="flex-1 truncate text-xs text-charcoal">{{ shareUrl }}</span>
                      <button class="btn-primary px-2.5 py-1 text-xs" @click="copyShareLink">{{ linkCopied ? "Copied" : "Copy" }}</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <RouterLink v-if="business" :to="`/providers/${business.id}`" class="group card card-interactive flex items-center gap-3.5 p-4">
            <img v-if="business.imageUrl" :src="business.imageUrl" alt="" class="h-12 w-12 shrink-0 rounded-xl object-cover ring-1 ring-light-grey" />
            <span v-else class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl font-display font-bold" :class="categoryMeta(business.category).tile">
              {{ business.businessName.charAt(0) }}
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-[11px] font-semibold uppercase tracking-wider text-medium-grey">Sold by</p>
              <p class="flex items-center gap-1.5 truncate font-display text-sm font-semibold text-uni-navy">
                {{ business.businessName }}
                <BadgeCheck v-if="business.verificationStatus === 'VERIFIED'" class="h-4 w-4 shrink-0 text-emerald-500" />
              </p>
            </div>
            <ChevronRight class="h-4 w-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-teal-600" />
          </RouterLink>

          <div v-if="auth.isAuthenticated" class="px-1">
            <button v-if="!reportOpen" class="inline-flex items-center gap-1.5 text-xs font-medium text-medium-grey hover:text-danger" @click="reportOpen = true">
              <Flag class="h-3.5 w-3.5" /> Report this listing
            </button>
            <form v-else class="card space-y-3 p-4" @submit.prevent="submitReport">
              <p class="text-sm font-semibold text-uni-navy">Report this listing</p>
              <select v-model="reportReason" class="input-field">
                <option value="MISREPRESENTATION">Misrepresentation</option>
                <option value="NON_DELIVERY">Non-delivery</option>
                <option value="INAPPROPRIATE_CONDUCT">Inappropriate conduct</option>
                <option value="SPAM">Spam</option>
                <option value="OTHER">Other</option>
              </select>
              <textarea v-model="reportDetails" class="input-field" rows="3" placeholder="Tell us what happened (optional)"></textarea>
              <div class="flex gap-2">
                <button type="button" class="btn-secondary flex-1" @click="reportOpen = false">Cancel</button>
                <button type="submit" class="btn-danger flex-1">Submit report</button>
              </div>
            </form>
            <p v-if="reportStatus" class="mt-2 text-xs text-medium-grey">{{ reportStatus }}</p>
          </div>
        </div>
      </div>

      <!-- Description + Q&A -->
      <div class="space-y-6 lg:col-start-1">
        <div class="card p-6">
          <h2 class="section-title">About this {{ listing.type === "PRODUCT" ? "product" : "service" }}</h2>
          <p class="mt-3 whitespace-pre-line leading-relaxed text-charcoal">{{ listing.description }}</p>
        </div>

        <div class="card space-y-5 p-6">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h2 class="section-title">Questions &amp; answers</h2>
              <p class="mt-0.5 text-xs text-medium-grey">Questions and the seller's answers are visible to everyone.</p>
            </div>
            <span v-if="questions.length > 0" class="badge shrink-0 bg-soft-grey text-medium-grey">{{ questions.length }}</span>
          </div>

          <div v-if="auth.isAuthenticated" class="rounded-card border border-light-grey p-3 focus-within:border-teal-500 focus-within:ring-4 focus-within:ring-teal-500/15">
            <textarea
              v-model="questionDraft"
              rows="2"
              :maxlength="QUESTION_MAX_LENGTH"
              class="w-full resize-none bg-transparent text-sm text-charcoal placeholder:text-slate-400 focus:outline-none"
              placeholder="Ask a question about this listing…"
            ></textarea>
            <div class="flex items-center justify-between">
              <span class="text-[11px] text-medium-grey">{{ questionDraft.length }} / {{ QUESTION_MAX_LENGTH }}</span>
              <button class="btn-primary px-3.5 py-1.5 text-xs" :disabled="questionSubmitting || !questionDraft.trim()" @click="submitQuestion">
                {{ questionSubmitting ? "Posting…" : "Post question" }}
              </button>
            </div>
          </div>
          <p v-else class="text-sm text-medium-grey">
            <RouterLink :to="{ path: '/login', query: { redirect: $route.fullPath } }" class="link">Log in</RouterLink> to ask a question.
          </p>

          <p v-if="questions.length === 0" class="text-sm text-medium-grey">No questions yet - be the first to ask.</p>
          <ul v-else class="space-y-5">
            <li v-for="q in questions" :key="q.id" class="flex gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-navy-100 text-[11px] font-bold text-navy-700">
                {{ initials(q.askerName) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2">
                  <p class="text-xs text-medium-grey"><span class="font-semibold text-charcoal">{{ q.askerName }}</span> · {{ relativeTime(q.createdAt) }}</p>
                  <button
                    v-if="auth.isAuthenticated && auth.user?.id !== q.askerId && !flaggedQuestionIds.has(q.id)"
                    class="shrink-0 text-[11px] font-medium text-medium-grey hover:text-danger disabled:opacity-50"
                    :disabled="flaggingQuestionId === q.id"
                    @click="flagQuestion(q.id)"
                  >
                    Flag
                  </button>
                  <span v-else-if="flaggedQuestionIds.has(q.id)" class="shrink-0 text-[11px] font-medium text-medium-grey">Flagged</span>
                </div>
                <p class="mt-0.5 text-sm font-medium text-charcoal">{{ q.questionText }}</p>
                <div v-if="q.answerText" class="mt-2.5 rounded-control border-l-2 border-teal-500 bg-teal-50/60 px-3.5 py-2.5">
                  <p class="text-xs font-semibold text-uni-navy">
                    {{ business?.businessName }} <span class="ml-1 rounded-full bg-uni-navy px-1.5 py-0.5 text-[9px] font-bold uppercase text-white">Seller</span>
                    <span class="ml-1 font-normal text-medium-grey">· {{ relativeTime(q.answeredAt!) }}</span>
                  </p>
                  <p class="mt-1 text-sm text-charcoal">{{ q.answerText }}</p>
                </div>
                <span v-else class="badge mt-2 bg-amber-50 text-warning">Awaiting seller reply</span>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- More from this seller -->
    <div v-if="business && moreFromSeller.length > 0">
      <div class="mb-5 flex items-end justify-between gap-4">
        <div>
          <p class="eyebrow">From the same seller</p>
          <h2 class="mt-1 font-display text-2xl font-bold text-uni-navy">More from {{ business.businessName }}</h2>
        </div>
        <RouterLink :to="`/providers/${business.id}`" class="link hidden text-sm sm:inline">View shop</RouterLink>
      </div>
      <div class="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
        <ListingCard v-for="l in moreFromSeller" :key="l.id" :listing="l" />
      </div>
    </div>

    <!-- Booking modal -->
    <div v-if="bookingOpen" class="modal-backdrop" @click.self="bookingOpen = false">
      <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="booking-title">
        <div class="mb-1 flex items-center justify-between">
          <h2 id="booking-title" class="font-display text-lg font-bold text-uni-navy">Request a booking</h2>
          <button class="btn-icon h-8 w-8" aria-label="Close" @click="bookingOpen = false"><X class="h-4 w-4" /></button>
        </div>
        <p class="mb-5 text-sm text-medium-grey">{{ business?.businessName }} will accept or decline - you'll get a notification either way.</p>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="field-label" for="booking-date">Preferred date</label>
            <input id="booking-date" v-model="bookingDate" type="date" class="input-field" :min="new Date().toISOString().slice(0, 10)" />
          </div>
          <div>
            <label class="field-label" for="booking-time">Preferred time</label>
            <input id="booking-time" v-model="bookingTime" type="time" class="input-field" />
          </div>
        </div>
        <label class="field-label mt-4" for="booking-note">Note (optional)</label>
        <textarea id="booking-note" v-model="bookingNote" class="input-field" rows="3" placeholder="Anything the seller should know…"></textarea>

        <div class="mt-6 flex gap-2">
          <button class="btn-secondary flex-1" @click="bookingOpen = false">Cancel</button>
          <button class="btn-primary flex-1" :disabled="bookingSubmitting || !bookingDate" @click="submitBooking">
            {{ bookingSubmitting ? "Sending…" : "Send request" }}
          </button>
        </div>
      </div>
    </div>

    <!-- Message modal -->
    <div v-if="messageOpen" class="modal-backdrop" @click.self="messageOpen = false">
      <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="message-title">
        <div class="mb-1 flex items-center justify-between">
          <h2 id="message-title" class="font-display text-lg font-bold text-uni-navy">Message {{ business?.businessName }}</h2>
          <button class="btn-icon h-8 w-8" aria-label="Close" @click="messageOpen = false"><X class="h-4 w-4" /></button>
        </div>
        <p class="mb-4 text-sm text-medium-grey">About: <span class="font-medium text-charcoal">{{ listing?.name }}</span></p>

        <textarea v-model="messageBody" class="input-field resize-none" rows="4" placeholder="Hi! I'd like to ask about…"></textarea>
        <p v-if="messageStatus" class="mt-2 text-sm text-danger">{{ messageStatus }}</p>

        <div class="mt-6 flex gap-2">
          <button class="btn-secondary flex-1" @click="messageOpen = false">Cancel</button>
          <button class="btn-primary flex-1" :disabled="messageSending || !messageBody.trim()" @click="sendMessage">
            {{ messageSending ? "Sending…" : "Send message" }}
          </button>
        </div>
      </div>
    </div>
  </section>

  <div v-else-if="error" class="mx-auto max-w-md py-16 text-center">
    <p class="font-display text-lg font-semibold text-uni-navy">We couldn't load this listing</p>
    <p class="mt-1 text-sm text-medium-grey">{{ error }}</p>
    <RouterLink to="/" class="btn-secondary mt-5">Back to marketplace</RouterLink>
  </div>

  <div v-else class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)] lg:gap-10" aria-busy="true">
    <div class="skeleton aspect-[4/3] rounded-modal"></div>
    <div class="card space-y-4 p-6">
      <div class="skeleton h-3 w-24"></div>
      <div class="skeleton h-8 w-3/4"></div>
      <div class="skeleton h-24 w-full"></div>
      <div class="skeleton h-12 w-full"></div>
    </div>
  </div>
</template>
