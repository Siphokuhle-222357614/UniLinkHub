<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, extractErrorMessage } from "@/lib/api";
import AdminNav from "@/components/AdminNav.vue";
import type {
  AdminBusinessView,
  AdminStatsDTO,
  AdminUserDetailDTO,
  AnnouncementDTO,
  AuditLogEntryDTO,
  BookingStatsDTO,
  ListingDTO,
  OrderStatsDTO,
  PromoStatsDTO,
  ReportSummaryView,
  ReportStatus,
  ReportStatusCounts,
  ReviewStatsDTO,
  ReviewView,
  UserResponse,
} from "@/lib/types";

type Section =
  | "overview"
  | "reports"
  | "businesses"
  | "accounts"
  | "announcements"
  | "activity"
  | "bookings"
  | "reviews"
  | "orders"
  | "promos"
  | "broadcast";
const activeSection = ref<Section>("overview");

// ---- Overview ----
const stats = ref<AdminStatsDTO | null>(null);
const statsLoading = ref(false);
const statsError = ref("");

async function loadStats() {
  statsLoading.value = true;
  statsError.value = "";
  try {
    const { data } = await api.get<AdminStatsDTO>("/admin/stats");
    stats.value = data;
  } catch (err) {
    statsError.value = extractErrorMessage(err);
  } finally {
    statsLoading.value = false;
  }
}

// ---- Reports ----
const reports = ref<ReportSummaryView[]>([]);
const counts = ref<ReportStatusCounts>({ open: 0, underReview: 0, resolved: 0, dismissed: 0 });
const activeFilter = ref<ReportStatus | "ALL">("ALL");
const selectedId = ref<string | null>(null);
const adminNote = ref("");
const reportsLoading = ref(false);
const acting = ref(false);
const reportsError = ref("");

const filters: { value: ReportStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "OPEN", label: "Open" },
  { value: "UNDER_REVIEW", label: "Under review" },
  { value: "RESOLVED", label: "Resolved" },
  { value: "DISMISSED", label: "Dismissed" },
];

function countFor(value: ReportStatus | "ALL"): number {
  if (value === "ALL") return counts.value.open + counts.value.underReview + counts.value.resolved + counts.value.dismissed;
  if (value === "OPEN") return counts.value.open;
  if (value === "UNDER_REVIEW") return counts.value.underReview;
  if (value === "RESOLVED") return counts.value.resolved;
  return counts.value.dismissed;
}

const selected = computed(() => reports.value.find((r) => r.id === selectedId.value) ?? null);

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

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

async function loadCounts() {
  try {
    const { data } = await api.get<ReportStatusCounts>("/admin/reports/counts");
    counts.value = data;
  } catch (err) {
    reportsError.value = extractErrorMessage(err);
  }
}

async function loadReports() {
  reportsLoading.value = true;
  reportsError.value = "";
  try {
    const { data } = await api.get<ReportSummaryView[]>("/admin/reports", {
      params: activeFilter.value === "ALL" ? {} : { status: activeFilter.value },
    });
    reports.value = data;
    if (!data.find((r) => r.id === selectedId.value)) {
      selectedId.value = data[0]?.id ?? null;
      adminNote.value = "";
    }
  } catch (err) {
    reportsError.value = extractErrorMessage(err);
  } finally {
    reportsLoading.value = false;
  }
}

function selectReport(id: string) {
  selectedId.value = id;
  adminNote.value = selected.value?.adminNote ?? "";
}

function setFilter(value: ReportStatus | "ALL") {
  activeFilter.value = value;
  loadReports();
}

function exportReportsCsv() {
  const header = ["Reason", "Status", "Target type", "Target", "Reporter", "Filed at"];
  const rows = reports.value.map((r) => [
    REASON_LABELS[r.reason] ?? r.reason,
    STATUS_LABELS[r.status],
    r.target.type,
    r.target.label,
    r.reporter.fullName,
    r.createdAt,
  ]);
  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `reports-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

async function act(action: "begin-review" | "resolve" | "dismiss") {
  if (!selected.value) return;
  acting.value = true;
  reportsError.value = "";
  try {
    if (action === "begin-review") {
      await api.post(`/admin/reports/${selected.value.id}/begin-review`);
    } else {
      await api.post(`/admin/reports/${selected.value.id}/${action}`, { note: adminNote.value });
    }
    await Promise.all([loadReports(), loadCounts()]);
  } catch (err) {
    reportsError.value = extractErrorMessage(err);
  } finally {
    acting.value = false;
  }
}

// ---- Business verification ----
const allBusinesses = ref<AdminBusinessView[]>([]);
const businessesLoading = ref(false);
const actingBusinessId = ref<string | null>(null);
const businessesError = ref("");
const businessFilter = ref<"ALL" | "PENDING" | "VERIFIED" | "REJECTED">("PENDING");
const businessKeyword = ref("");

const businessFilters: { value: "ALL" | "PENDING" | "VERIFIED" | "REJECTED"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "VERIFIED", label: "Verified" },
  { value: "REJECTED", label: "Rejected" },
];

function businessCountFor(value: string): number {
  if (value === "ALL") return allBusinesses.value.length;
  return allBusinesses.value.filter((b) => b.verificationStatus === value).length;
}

const filteredBusinesses = computed(() => {
  let list = allBusinesses.value;
  if (businessFilter.value !== "ALL") {
    list = list.filter((b) => b.verificationStatus === businessFilter.value);
  }
  const needle = businessKeyword.value.trim().toLowerCase();
  if (needle) {
    list = list.filter(
      (b) => b.businessName.toLowerCase().includes(needle) || b.category.toLowerCase().includes(needle),
    );
  }
  return list;
});

function relativeDays(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const days = Math.round(diffMs / 86400000);
  if (days < 1) return "today";
  if (days === 1) return "1 day ago";
  return `${days} days ago`;
}

async function loadBusinesses() {
  businessesLoading.value = true;
  businessesError.value = "";
  try {
    const { data } = await api.get<AdminBusinessView[]>("/admin/businesses", { params: { status: "ALL" } });
    allBusinesses.value = data;
  } catch (err) {
    businessesError.value = extractErrorMessage(err);
  } finally {
    businessesLoading.value = false;
  }
}

const expandedBusinessId = ref<string | null>(null);
const businessListings = ref<Record<string, ListingDTO[]>>({});
const businessListingsLoading = ref<string | null>(null);

async function toggleBusinessExpand(id: string) {
  if (expandedBusinessId.value === id) {
    expandedBusinessId.value = null;
    return;
  }
  expandedBusinessId.value = id;
  if (!businessListings.value[id]) {
    businessListingsLoading.value = id;
    try {
      const { data } = await api.get<ListingDTO[]>(`/listings/business/${id}`);
      businessListings.value[id] = data;
    } catch (err) {
      businessesError.value = extractErrorMessage(err);
    } finally {
      businessListingsLoading.value = null;
    }
  }
}

function formatPrice(price: number) {
  return new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" }).format(price);
}

async function verify(id: string) {
  actingBusinessId.value = id;
  businessesError.value = "";
  try {
    await api.post(`/admin/businesses/${id}/verify`);
    await loadBusinesses();
  } catch (err) {
    businessesError.value = extractErrorMessage(err);
  } finally {
    actingBusinessId.value = null;
  }
}

const rejectingBusinessId = ref<string | null>(null);
const rejectReason = ref("");

function startReject(id: string) {
  rejectingBusinessId.value = id;
  rejectReason.value = "";
}

function cancelReject() {
  rejectingBusinessId.value = null;
}

async function confirmReject(id: string) {
  actingBusinessId.value = id;
  businessesError.value = "";
  try {
    await api.post(`/admin/businesses/${id}/reject`, { reason: rejectReason.value });
    rejectingBusinessId.value = null;
    await loadBusinesses();
  } catch (err) {
    businessesError.value = extractErrorMessage(err);
  } finally {
    actingBusinessId.value = null;
  }
}

// ---- Student accounts ----
const allAccounts = ref<UserResponse[]>([]);
const accountsLoading = ref(false);
const actingAccountId = ref<string | null>(null);
const accountsError = ref("");
const accountFilter = ref<"ALL" | "PENDING_VERIFICATION" | "ACTIVE" | "SUSPENDED">("ALL");
const accountKeyword = ref("");

const accountFilters: { value: "ALL" | "PENDING_VERIFICATION" | "ACTIVE" | "SUSPENDED"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING_VERIFICATION", label: "Pending" },
  { value: "ACTIVE", label: "Active" },
  { value: "SUSPENDED", label: "Suspended" },
];

function accountCountFor(value: string): number {
  if (value === "ALL") return allAccounts.value.length;
  return allAccounts.value.filter((u) => u.accountStatus === value).length;
}

const filteredAccounts = computed(() => {
  let list = allAccounts.value;
  if (accountFilter.value !== "ALL") {
    list = list.filter((u) => u.accountStatus === accountFilter.value);
  }
  const needle = accountKeyword.value.trim().toLowerCase();
  if (needle) {
    list = list.filter(
      (u) =>
        `${u.firstName} ${u.lastName}`.toLowerCase().includes(needle) ||
        u.email.toLowerCase().includes(needle) ||
        u.studentNumber.toLowerCase().includes(needle),
    );
  }
  return list;
});

function accountAge(iso: string): string {
  const days = Math.round((Date.now() - new Date(iso).getTime()) / 86400000);
  if (days < 1) return "joined today";
  if (days === 1) return "joined 1 day ago";
  return `joined ${days} days ago`;
}

async function loadAccounts() {
  accountsLoading.value = true;
  accountsError.value = "";
  try {
    const { data } = await api.get<UserResponse[]>("/admin/users", { params: { status: "ALL" } });
    allAccounts.value = data;
  } catch (err) {
    accountsError.value = extractErrorMessage(err);
  } finally {
    accountsLoading.value = false;
  }
}

async function acceptOrChangeAccount(id: string, action: "approve" | "reactivate") {
  actingAccountId.value = id;
  accountsError.value = "";
  try {
    await api.post(`/admin/users/${id}/${action}`);
    await loadAccounts();
  } catch (err) {
    accountsError.value = extractErrorMessage(err);
  } finally {
    actingAccountId.value = null;
  }
}

const suspendingAccountId = ref<string | null>(null);
const suspendReason = ref("");

function startSuspend(id: string) {
  suspendingAccountId.value = id;
  suspendReason.value = "";
}

function cancelSuspend() {
  suspendingAccountId.value = null;
}

async function confirmSuspend(id: string) {
  actingAccountId.value = id;
  accountsError.value = "";
  try {
    await api.post(`/admin/users/${id}/suspend`, { reason: suspendReason.value });
    suspendingAccountId.value = null;
    await loadAccounts();
  } catch (err) {
    accountsError.value = extractErrorMessage(err);
  } finally {
    actingAccountId.value = null;
  }
}

const promotingAccountId = ref<string | null>(null);

async function confirmPromote(id: string) {
  actingAccountId.value = id;
  accountsError.value = "";
  try {
    await api.post(`/admin/users/${id}/promote`);
    promotingAccountId.value = null;
    await loadAccounts();
  } catch (err) {
    accountsError.value = extractErrorMessage(err);
  } finally {
    actingAccountId.value = null;
  }
}

const expandedAccountId = ref<string | null>(null);
const accountDetail = ref<Record<string, AdminUserDetailDTO>>({});
const accountDetailLoading = ref<string | null>(null);

async function toggleAccountExpand(id: string) {
  if (expandedAccountId.value === id) {
    expandedAccountId.value = null;
    return;
  }
  expandedAccountId.value = id;
  if (!accountDetail.value[id]) {
    accountDetailLoading.value = id;
    try {
      const { data } = await api.get<AdminUserDetailDTO>(`/admin/users/${id}/detail`);
      accountDetail.value[id] = data;
    } catch (err) {
      accountsError.value = extractErrorMessage(err);
    } finally {
      accountDetailLoading.value = null;
    }
  }
}

// ---- Announcements ----
const announcements = ref<AnnouncementDTO[]>([]);
const announcementsLoading = ref(false);
const announcementsError = ref("");
const newAnnouncementMessage = ref("");
const newAnnouncementActive = ref(true);
const publishingAnnouncement = ref(false);
const decidingAnnouncementId = ref<string | null>(null);

async function loadAnnouncements() {
  announcementsLoading.value = true;
  announcementsError.value = "";
  try {
    const { data } = await api.get<AnnouncementDTO[]>("/admin/announcements");
    announcements.value = data;
  } catch (err) {
    announcementsError.value = extractErrorMessage(err);
  } finally {
    announcementsLoading.value = false;
  }
}

async function publishAnnouncement() {
  if (!newAnnouncementMessage.value.trim()) return;
  publishingAnnouncement.value = true;
  announcementsError.value = "";
  try {
    await api.post("/admin/announcements", { message: newAnnouncementMessage.value, active: newAnnouncementActive.value });
    newAnnouncementMessage.value = "";
    newAnnouncementActive.value = true;
    await loadAnnouncements();
  } catch (err) {
    announcementsError.value = extractErrorMessage(err);
  } finally {
    publishingAnnouncement.value = false;
  }
}

async function deactivateAnnouncement(id: string) {
  decidingAnnouncementId.value = id;
  announcementsError.value = "";
  try {
    await api.post(`/admin/announcements/${id}/deactivate`);
    await loadAnnouncements();
  } catch (err) {
    announcementsError.value = extractErrorMessage(err);
  } finally {
    decidingAnnouncementId.value = null;
  }
}

// ---- Activity log ----
const activityEntries = ref<AuditLogEntryDTO[]>([]);
const activityLoading = ref(false);
const activityError = ref("");
const activityFilter = ref<"ALL" | "BUSINESS" | "ACCOUNT" | "REPORT" | "ANNOUNCEMENT" | "REVIEW">("ALL");

const activityFilters: { value: typeof activityFilter.value; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "BUSINESS", label: "Verifications" },
  { value: "ACCOUNT", label: "Accounts" },
  { value: "REPORT", label: "Reports" },
  { value: "REVIEW", label: "Reviews" },
  { value: "ANNOUNCEMENT", label: "Announcements" },
];

const CATEGORY_ICONS: Record<string, string> = {
  BUSINESS: "✓",
  ACCOUNT: "⏸",
  REPORT: "🚩",
  REVIEW: "⭐",
  ANNOUNCEMENT: "📢",
};

function activityRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

async function loadActivityLog() {
  activityLoading.value = true;
  activityError.value = "";
  try {
    const { data } = await api.get<AuditLogEntryDTO[]>("/admin/audit-log", { params: { category: activityFilter.value } });
    activityEntries.value = data;
  } catch (err) {
    activityError.value = extractErrorMessage(err);
  } finally {
    activityLoading.value = false;
  }
}

function setActivityFilter(value: typeof activityFilter.value) {
  activityFilter.value = value;
  loadActivityLog();
}

// ---- Bookings ----
const bookingStats = ref<BookingStatsDTO | null>(null);
const bookingStatsLoading = ref(false);
const bookingStatsError = ref("");

async function loadBookingStats() {
  bookingStatsLoading.value = true;
  bookingStatsError.value = "";
  try {
    const { data } = await api.get<BookingStatsDTO>("/admin/bookings/stats");
    bookingStats.value = data;
  } catch (err) {
    bookingStatsError.value = extractErrorMessage(err);
  } finally {
    bookingStatsLoading.value = false;
  }
}

// ---- Reviews ----
const reviewStats = ref<ReviewStatsDTO | null>(null);
const flaggedReviews = ref<ReviewView[]>([]);
const reviewsLoading = ref(false);
const reviewsError = ref("");
const removingReviewId = ref<string | null>(null);

async function loadReviewsSection() {
  reviewsLoading.value = true;
  reviewsError.value = "";
  try {
    const [{ data: stats }, { data: flagged }] = await Promise.all([
      api.get<ReviewStatsDTO>("/admin/reviews/stats"),
      api.get<ReviewView[]>("/admin/reviews", { params: { flaggedOnly: true } }),
    ]);
    reviewStats.value = stats;
    flaggedReviews.value = flagged;
  } catch (err) {
    reviewsError.value = extractErrorMessage(err);
  } finally {
    reviewsLoading.value = false;
  }
}

async function removeReview(id: string) {
  removingReviewId.value = id;
  reviewsError.value = "";
  try {
    await api.delete(`/admin/reviews/${id}`);
    await loadReviewsSection();
  } catch (err) {
    reviewsError.value = extractErrorMessage(err);
  } finally {
    removingReviewId.value = null;
  }
}

// ---- Orders ----
const orderStats = ref<OrderStatsDTO | null>(null);
const orderStatsLoading = ref(false);
const orderStatsError = ref("");

async function loadOrderStats() {
  orderStatsLoading.value = true;
  orderStatsError.value = "";
  try {
    const { data } = await api.get<OrderStatsDTO>("/admin/orders/stats");
    orderStats.value = data;
  } catch (err) {
    orderStatsError.value = extractErrorMessage(err);
  } finally {
    orderStatsLoading.value = false;
  }
}

// ---- Promo codes ----
const promoStats = ref<PromoStatsDTO | null>(null);
const promoStatsLoading = ref(false);
const promoStatsError = ref("");

async function loadPromoStats() {
  promoStatsLoading.value = true;
  promoStatsError.value = "";
  try {
    const { data } = await api.get<PromoStatsDTO>("/admin/promo-codes/stats");
    promoStats.value = data;
  } catch (err) {
    promoStatsError.value = extractErrorMessage(err);
  } finally {
    promoStatsLoading.value = false;
  }
}

// ---- Broadcast notification ----
const broadcastAudience = ref<"ALL_STUDENTS" | "ALL_SELLERS" | "PENDING_BUSINESS_OWNERS">("ALL_STUDENTS");
const broadcastMessage = ref("");
const broadcastSending = ref(false);
const broadcastStatus = ref("");

async function sendBroadcast() {
  if (!broadcastMessage.value.trim()) return;
  broadcastSending.value = true;
  broadcastStatus.value = "";
  try {
    const { data } = await api.post<{ recipientCount: number }>("/admin/notifications/broadcast", {
      audience: broadcastAudience.value,
      message: broadcastMessage.value,
    });
    broadcastStatus.value = `Sent to ${data.recipientCount} user(s).`;
    broadcastMessage.value = "";
  } catch (err) {
    broadcastStatus.value = extractErrorMessage(err);
  } finally {
    broadcastSending.value = false;
  }
}

onMounted(async () => {
  await Promise.all([
    loadStats(),
    loadCounts(),
    loadReports(),
    loadBusinesses(),
    loadAccounts(),
    loadAnnouncements(),
    loadActivityLog(),
    loadBookingStats(),
    loadReviewsSection(),
    loadOrderStats(),
    loadPromoStats(),
  ]);
});
</script>

<template>
  <div class="min-h-screen bg-soft-grey">
    <AdminNav />

    <main class="mx-auto max-w-6xl space-y-5 px-4 py-6 sm:px-6">
      <div>
        <h1 class="font-display text-2xl font-bold text-uni-navy sm:text-[26px]">Admin dashboard</h1>
        <p class="text-sm text-medium-grey">Trust &amp; safety and business verification, in one place.</p>
      </div>

      <div class="flex gap-2 overflow-x-auto border-b border-light-grey">
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'overview' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'overview'"
        >
          Overview
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'reports' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'reports'"
        >
          Reports
          <span class="ml-1.5 rounded-full bg-soft-grey px-2 py-0.5 text-xs">{{ countFor('ALL') }}</span>
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'businesses' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'businesses'"
        >
          Business verification
          <span class="ml-1.5 rounded-full bg-soft-grey px-2 py-0.5 text-xs">{{ businessCountFor('PENDING') }}</span>
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'accounts' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'accounts'"
        >
          Student accounts
          <span class="ml-1.5 rounded-full bg-soft-grey px-2 py-0.5 text-xs">{{ accountCountFor('PENDING_VERIFICATION') }}</span>
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'announcements' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'announcements'"
        >
          Announcements
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'activity' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'activity'"
        >
          Activity log
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'bookings' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'bookings'"
        >
          Bookings
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'reviews' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'reviews'"
        >
          Reviews
          <span v-if="flaggedReviews.length > 0" class="ml-1.5 rounded-full bg-danger/15 px-2 py-0.5 text-xs text-danger">{{ flaggedReviews.length }}</span>
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'orders' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'orders'"
        >
          Orders
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'promos' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'promos'"
        >
          Promo codes
        </button>
        <button
          class="border-b-2 px-1 pb-3 text-sm font-semibold"
          :class="activeSection === 'broadcast' ? 'border-campus-teal text-uni-navy' : 'border-transparent text-medium-grey hover:text-charcoal'"
          @click="activeSection = 'broadcast'"
        >
          Send notification
        </button>
      </div>

      <!-- Reports section -->
      <!-- Overview section -->
      <section v-if="activeSection === 'overview'" class="space-y-6">
        <p v-if="statsError" class="text-sm text-danger">{{ statsError }}</p>
        <p v-else-if="statsLoading || !stats" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Students</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ stats.totalStudents }}</p>
              <p class="text-xs text-medium-grey">{{ stats.pendingAccounts }} pending approval</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Businesses</p>
              <p class="font-display text-2xl font-bold text-uni-navy">
                {{ stats.businesses.pending + stats.businesses.verified + stats.businesses.rejected }}
              </p>
              <p class="text-xs text-medium-grey">{{ stats.businesses.pending }} pending review</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Active listings</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ stats.listings.active }}</p>
              <p class="text-xs text-medium-grey">
                {{ stats.listings.active + stats.listings.inactive + stats.listings.soldOut }} total ever created
              </p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Open reports</p>
              <p class="font-display text-2xl font-bold text-danger">{{ stats.reports.open }}</p>
              <p class="text-xs text-medium-grey">{{ stats.reports.resolved }} resolved</p>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div class="card space-y-2.5">
              <h3 class="font-display text-sm font-semibold text-uni-navy">Businesses by status</h3>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Verified</span>
                <span class="badge bg-success/15 text-success">{{ stats.businesses.verified }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Pending</span>
                <span class="badge bg-warning/15 text-warning">{{ stats.businesses.pending }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Rejected</span>
                <span class="badge bg-danger/15 text-danger">{{ stats.businesses.rejected }}</span>
              </div>
            </div>

            <div class="card space-y-2.5">
              <h3 class="font-display text-sm font-semibold text-uni-navy">Listings by status</h3>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Active</span>
                <span class="badge bg-success/15 text-success">{{ stats.listings.active }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Inactive</span>
                <span class="badge bg-medium-grey/15 text-medium-grey">{{ stats.listings.inactive }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Sold out</span>
                <span class="badge bg-medium-grey/15 text-medium-grey">{{ stats.listings.soldOut }}</span>
              </div>
            </div>

            <div class="card space-y-2.5">
              <h3 class="font-display text-sm font-semibold text-uni-navy">Reports by status</h3>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Open</span>
                <span class="badge bg-warning/15 text-warning">{{ stats.reports.open }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Under review</span>
                <span class="badge bg-info/15 text-info">{{ stats.reports.underReview }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Resolved</span>
                <span class="badge bg-success/15 text-success">{{ stats.reports.resolved }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-charcoal">Dismissed</span>
                <span class="badge bg-medium-grey/15 text-medium-grey">{{ stats.reports.dismissed }}</span>
              </div>
            </div>
          </div>
        </template>
      </section>

      <section v-else-if="activeSection === 'reports'" class="space-y-5">
        <p v-if="reportsError" class="text-sm text-danger">{{ reportsError }}</p>

        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="f in filters"
              :key="f.value"
              class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
              :class="activeFilter === f.value ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
              @click="setFilter(f.value)"
            >
              {{ f.label }} &nbsp;{{ countFor(f.value) }}
            </button>
          </div>
          <button
            class="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-control border border-uni-navy bg-white px-3.5 py-1.5 text-sm font-semibold text-uni-navy disabled:opacity-50"
            :disabled="reports.length === 0"
            @click="exportReportsCsv"
          >
            Export CSV
          </button>
        </div>

        <p v-if="reportsLoading" class="text-sm text-medium-grey">Loading...</p>

        <div v-else-if="reports.length === 0" class="card text-sm text-medium-grey">
          No reports in this filter.
        </div>

        <div v-else class="flex flex-col gap-5 lg:flex-row lg:items-start">
          <div class="flex w-full flex-col gap-2.5 lg:w-[400px] lg:shrink-0">
            <button
              v-for="r in reports"
              :key="r.id"
              class="rounded-card border bg-white p-3.5 text-left shadow-sm transition"
              :class="r.id === selectedId ? 'border-[1.5px] border-campus-teal' : 'border-light-grey hover:border-campus-teal'"
              @click="selectReport(r.id)"
            >
              <div class="mb-1.5 flex items-center justify-between">
                <span class="text-sm font-semibold text-uni-navy">{{ REASON_LABELS[r.reason] ?? r.reason }}</span>
                <span class="badge" :class="STATUS_STYLES[r.status]">{{ STATUS_LABELS[r.status] }}</span>
              </div>
              <p class="mb-2 line-clamp-2 text-[13px] text-charcoal">{{ r.details || "No further details provided." }}</p>
              <div class="flex items-center justify-between text-xs text-medium-grey">
                <span>{{ r.target.type === "LISTING" ? "Listing" : "Provider" }} &middot; {{ r.target.label }}</span>
                <span>{{ relativeTime(r.createdAt) }}</span>
              </div>
            </button>
          </div>

          <div v-if="selected" class="card flex-1 space-y-5 p-6">
            <div>
              <div class="mb-1 flex items-center gap-2.5">
                <h2 class="font-display text-[19px] font-semibold text-uni-navy">
                  {{ REASON_LABELS[selected.reason] ?? selected.reason }}
                </h2>
                <span class="badge" :class="STATUS_STYLES[selected.status]">{{ STATUS_LABELS[selected.status] }}</span>
              </div>
              <p class="text-[13px] text-medium-grey">
                Report #{{ selected.id.slice(0, 8) }} &middot; Filed {{ relativeTime(selected.createdAt) }}
              </p>
            </div>

            <div v-if="selected.totalReportsOnTarget > 1" class="flex items-start gap-2.5 rounded-control border border-danger/30 bg-danger/10 p-3">
              <span class="badge bg-danger/15 text-danger shrink-0">{{ selected.totalReportsOnTarget }} total reports</span>
              <p class="text-[13px] text-charcoal">
                This {{ selected.target.type === "LISTING" ? "listing" : "provider" }} has been reported
                {{ selected.totalReportsOnTarget }} times in total.
              </p>
            </div>

            <div class="grid grid-cols-1 gap-4 rounded-control bg-soft-grey p-4 sm:grid-cols-2">
              <div>
                <p class="mb-1 text-xs uppercase tracking-wide text-medium-grey">Reported by</p>
                <p class="text-sm font-medium text-charcoal">
                  {{ selected.reporter.fullName }} &middot; #{{ selected.reporter.studentNumber }}
                </p>
              </div>
              <div>
                <p class="mb-1 text-xs uppercase tracking-wide text-medium-grey">Target</p>
                <p class="text-sm font-medium text-charcoal">
                  <RouterLink v-if="selected.target.type === 'LISTING'" :to="`/listings/${selected.target.id}`" class="underline">
                    {{ selected.target.label }}
                  </RouterLink>
                  <span v-else>{{ selected.target.label }}</span>
                  &middot; {{ selected.target.type === "LISTING" ? "Listing" : "Provider" }}
                </p>
              </div>
              <div v-if="selected.target.secondaryLabel">
                <p class="mb-1 text-xs uppercase tracking-wide text-medium-grey">
                  {{ selected.target.type === "LISTING" ? "Business" : "Details" }}
                </p>
                <p class="text-sm font-medium text-charcoal">{{ selected.target.secondaryLabel }}</p>
              </div>
              <div>
                <p class="mb-1 text-xs uppercase tracking-wide text-medium-grey">Reason</p>
                <p class="text-sm font-medium text-charcoal">{{ REASON_LABELS[selected.reason] ?? selected.reason }}</p>
              </div>
            </div>

            <div>
              <p class="mb-1.5 text-xs uppercase tracking-wide text-medium-grey">Details from the reporter</p>
              <p class="text-sm leading-relaxed text-charcoal">{{ selected.details || "No further details provided." }}</p>
            </div>

            <div>
              <label class="mb-1.5 block text-xs uppercase tracking-wide text-medium-grey">
                Admin note (visible to reviewers only)
              </label>
              <textarea
                v-model="adminNote"
                rows="3"
                placeholder="What did you check, and what's the outcome?"
                class="input-field resize-y"
              ></textarea>
            </div>

            <div class="flex flex-wrap gap-2.5 border-t border-light-grey pt-4">
              <button
                class="btn-secondary text-sm"
                :disabled="acting || selected.status !== 'OPEN'"
                @click="act('begin-review')"
              >
                Begin review
              </button>
              <button
                class="inline-flex items-center justify-center rounded-control bg-success px-[18px] py-2.5 text-sm font-semibold text-white disabled:opacity-50"
                :disabled="acting || selected.status === 'RESOLVED' || selected.status === 'DISMISSED'"
                @click="act('resolve')"
              >
                Resolve
              </button>
              <button
                class="ml-auto inline-flex items-center justify-center rounded-control border border-danger px-[18px] py-2.5 text-sm font-semibold text-danger disabled:opacity-50"
                :disabled="acting || selected.status === 'RESOLVED' || selected.status === 'DISMISSED'"
                @click="act('dismiss')"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Business verification section -->
      <section v-else-if="activeSection === 'businesses'" class="space-y-5">
        <p v-if="businessesError" class="text-sm text-danger">{{ businessesError }}</p>
        <p v-else-if="businessesLoading" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex flex-wrap gap-2">
              <button
                v-for="f in businessFilters"
                :key="f.value"
                class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
                :class="businessFilter === f.value ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
                @click="businessFilter = f.value"
              >
                {{ f.label }} &nbsp;{{ businessCountFor(f.value) }}
              </button>
            </div>
            <input v-model="businessKeyword" type="search" placeholder="Search businesses..." class="input-field sm:w-64" />
          </div>

          <p v-if="filteredBusinesses.length === 0" class="card text-sm text-medium-grey">No businesses match this filter.</p>

          <div v-else class="flex flex-col gap-3">
            <div v-for="b in filteredBusinesses" :key="b.id" class="card space-y-4 p-[18px]">
              <div class="flex flex-col items-start justify-between gap-4 sm:flex-row">
                <div class="flex-1">
                  <div class="mb-1.5 flex items-center gap-2.5">
                    <span class="text-base font-semibold text-uni-navy">{{ b.businessName }}</span>
                    <span class="badge bg-academic-gold/20 text-uni-navy">{{ b.category }}</span>
                    <span
                      class="badge"
                      :class="{
                        'bg-success/15 text-success': b.verificationStatus === 'VERIFIED',
                        'bg-warning/15 text-warning': b.verificationStatus === 'PENDING',
                        'bg-danger/15 text-danger': b.verificationStatus === 'REJECTED',
                      }"
                    >
                      {{ b.verificationStatus }}
                    </span>
                  </div>
                  <p class="mb-2 text-[13px] leading-relaxed text-charcoal">{{ b.description }}</p>
                  <p class="text-xs text-medium-grey">
                    Owner: {{ b.ownerFullName }} &middot; #{{ b.ownerStudentNumber }} &middot;
                    {{ b.verificationStatus === "PENDING" ? "Submitted" : "Updated" }} {{ relativeDays(b.updatedAt) }}
                    &middot;
                    <button class="font-medium text-campus-teal underline" @click="toggleBusinessExpand(b.id)">
                      {{ expandedBusinessId === b.id ? "Hide listings" : "View listings" }}
                    </button>
                  </p>
                </div>
                <div v-if="b.verificationStatus === 'PENDING'" class="flex shrink-0 gap-2.5">
                  <button
                    class="inline-flex items-center justify-center rounded-control border border-danger bg-white px-4 py-2.5 text-sm font-semibold text-danger disabled:opacity-50"
                    :disabled="actingBusinessId === b.id"
                    @click="startReject(b.id)"
                  >
                    Reject
                  </button>
                  <button
                    class="inline-flex items-center justify-center rounded-control bg-success px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
                    :disabled="actingBusinessId === b.id"
                    @click="verify(b.id)"
                  >
                    Verify
                  </button>
                </div>
              </div>

              <div v-if="rejectingBusinessId === b.id" class="space-y-2 rounded-control border border-danger/30 bg-danger/5 p-3">
                <textarea
                  v-model="rejectReason"
                  rows="2"
                  placeholder="Why is this being rejected?"
                  class="input-field resize-y"
                ></textarea>
                <div class="flex justify-end gap-2">
                  <button class="btn-secondary text-sm" @click="cancelReject">Cancel</button>
                  <button
                    class="inline-flex items-center justify-center rounded-control bg-danger px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                    :disabled="actingBusinessId === b.id"
                    @click="confirmReject(b.id)"
                  >
                    Reject with reason
                  </button>
                </div>
              </div>

              <div v-if="expandedBusinessId === b.id" class="rounded-control border border-light-grey bg-soft-grey p-3">
                <p v-if="businessListingsLoading === b.id" class="text-xs text-medium-grey">Loading listings...</p>
                <template v-else>
                  <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-medium-grey">
                    Listings ({{ (businessListings[b.id] ?? []).length }})
                  </p>
                  <p v-if="(businessListings[b.id] ?? []).length === 0" class="text-sm text-medium-grey">
                    No listings yet.
                  </p>
                  <ul v-else class="space-y-1.5">
                    <li v-for="l in businessListings[b.id]" :key="l.id" class="flex items-center justify-between text-sm">
                      <span class="text-charcoal">{{ l.name }}</span>
                      <span class="font-medium text-campus-teal">{{ formatPrice(l.price) }}</span>
                    </li>
                  </ul>
                </template>
              </div>
            </div>
          </div>
        </template>
      </section>

      <!-- Student accounts section -->
      <section v-else-if="activeSection === 'accounts'" class="space-y-5">
        <p v-if="accountsError" class="text-sm text-danger">{{ accountsError }}</p>
        <p v-else-if="accountsLoading" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex flex-wrap gap-2">
              <button
                v-for="f in accountFilters"
                :key="f.value"
                class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
                :class="accountFilter === f.value ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
                @click="accountFilter = f.value"
              >
                {{ f.label }} &nbsp;{{ accountCountFor(f.value) }}
              </button>
            </div>
            <input
              v-model="accountKeyword"
              type="search"
              placeholder="Search by name, email or student number..."
              class="input-field sm:w-72"
            />
          </div>

          <p v-if="filteredAccounts.length === 0" class="card text-sm text-medium-grey">
            No accounts match this filter.
          </p>

          <div v-else class="flex flex-col gap-2">
            <div
              v-for="u in filteredAccounts"
              :key="u.id"
              class="card space-y-3 p-[18px]"
              :class="u.accountStatus === 'SUSPENDED' ? 'border-danger/30 bg-danger/5' : ''"
            >
              <div class="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
                <div class="flex-1">
                  <div class="mb-1 flex items-center gap-2.5">
                    <span class="text-base font-semibold text-uni-navy">{{ u.firstName }} {{ u.lastName }}</span>
                    <span
                      class="badge"
                      :class="{
                        'bg-success/15 text-success': u.accountStatus === 'ACTIVE',
                        'bg-warning/15 text-warning': u.accountStatus === 'PENDING_VERIFICATION',
                        'bg-danger/15 text-danger': u.accountStatus === 'SUSPENDED',
                        'bg-medium-grey/15 text-medium-grey': u.accountStatus === 'DEACTIVATED',
                      }"
                    >
                      {{ u.accountStatus === "PENDING_VERIFICATION" ? "Pending" : u.accountStatus.charAt(0) + u.accountStatus.slice(1).toLowerCase() }}
                    </span>
                    <span v-if="u.seller" class="badge bg-sky-blue/20 text-uni-navy">Seller</span>
                    <span v-if="u.role === 'ADMIN'" class="badge bg-academic-gold/20 text-uni-navy">Admin</span>
                  </div>
                  <p v-if="u.accountStatus === 'SUSPENDED' && u.suspensionReason" class="mb-1 text-[13px] italic text-danger">
                    "{{ u.suspensionReason }}"
                  </p>
                  <p class="text-[13px] text-medium-grey">
                    {{ u.email }} &middot; #{{ u.studentNumber }} &middot; {{ accountAge(u.createdAt) }}
                    &middot;
                    <button class="font-medium text-campus-teal underline" @click="toggleAccountExpand(u.id)">
                      {{ expandedAccountId === u.id ? "Hide details" : "View details" }}
                    </button>
                  </p>
                </div>
                <div class="flex shrink-0 flex-wrap gap-2">
                  <button
                    v-if="u.accountStatus === 'PENDING_VERIFICATION'"
                    class="btn-primary text-sm"
                    :disabled="actingAccountId === u.id"
                    @click="acceptOrChangeAccount(u.id, 'approve')"
                  >
                    Approve
                  </button>
                  <button
                    v-else-if="u.accountStatus === 'SUSPENDED'"
                    class="inline-flex items-center justify-center rounded-control bg-success px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                    :disabled="actingAccountId === u.id"
                    @click="acceptOrChangeAccount(u.id, 'reactivate')"
                  >
                    Reactivate
                  </button>
                  <button
                    v-else-if="u.accountStatus === 'ACTIVE' && u.role !== 'ADMIN'"
                    class="inline-flex items-center justify-center rounded-control border border-danger bg-white px-4 py-2 text-sm font-semibold text-danger disabled:opacity-50"
                    :disabled="actingAccountId === u.id"
                    @click="startSuspend(u.id)"
                  >
                    Suspend
                  </button>
                  <button
                    v-if="u.accountStatus === 'ACTIVE' && u.role !== 'ADMIN'"
                    class="inline-flex items-center justify-center rounded-control border border-academic-gold bg-white px-4 py-2 text-sm font-semibold text-uni-navy disabled:opacity-50"
                    :disabled="actingAccountId === u.id"
                    @click="promotingAccountId = u.id"
                  >
                    Promote to Admin
                  </button>
                </div>
              </div>

              <div v-if="suspendingAccountId === u.id" class="space-y-2 rounded-control border border-danger/30 bg-danger/5 p-3">
                <textarea
                  v-model="suspendReason"
                  rows="2"
                  placeholder="Why is this account being suspended? The student will see this if they try to log in."
                  class="input-field resize-y"
                ></textarea>
                <div class="flex justify-end gap-2">
                  <button class="btn-secondary text-sm" @click="cancelSuspend">Cancel</button>
                  <button
                    class="inline-flex items-center justify-center rounded-control bg-danger px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                    :disabled="actingAccountId === u.id"
                    @click="confirmSuspend(u.id)"
                  >
                    Suspend account
                  </button>
                </div>
              </div>

              <div v-if="promotingAccountId === u.id" class="fixed inset-0 z-20 flex items-center justify-center bg-charcoal/40 p-6" @click.self="promotingAccountId = null">
                <div class="w-full max-w-sm rounded-modal border border-light-grey bg-white p-5 shadow-lg">
                  <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-academic-gold/15 text-2xl">👑</div>
                  <h2 class="mt-3 text-center font-display text-base font-bold text-uni-navy">Promote {{ u.firstName }} {{ u.lastName }} to Admin?</h2>
                  <p class="mt-2 text-center text-sm text-charcoal">
                    They will gain full access to the admin console - business verification, report review, account
                    management and site announcements.
                  </p>
                  <div class="mt-4 flex gap-2">
                    <button class="btn-secondary flex-1 text-sm" @click="promotingAccountId = null">Cancel</button>
                    <button
                      class="inline-flex flex-1 items-center justify-center rounded-control bg-academic-gold px-4 py-2 text-sm font-semibold text-uni-navy disabled:opacity-50"
                      :disabled="actingAccountId === u.id"
                      @click="confirmPromote(u.id)"
                    >
                      Promote to Admin
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="expandedAccountId === u.id" class="space-y-3 rounded-control border border-light-grey bg-soft-grey p-3">
                <p v-if="accountDetailLoading === u.id" class="text-xs text-medium-grey">Loading...</p>
                <template v-else-if="accountDetail[u.id]">
                  <div class="grid grid-cols-3 gap-3 text-center">
                    <div>
                      <p class="font-display text-lg font-bold text-uni-navy">{{ accountDetail[u.id].businesses.length }}</p>
                      <p class="text-[11px] text-medium-grey">Businesses</p>
                    </div>
                    <div>
                      <p class="font-display text-lg font-bold text-uni-navy">{{ accountDetail[u.id].reportsFiled.length }}</p>
                      <p class="text-[11px] text-medium-grey">Reports filed</p>
                    </div>
                    <div>
                      <p class="font-display text-lg font-bold text-danger">{{ accountDetail[u.id].reportsReceived.length }}</p>
                      <p class="text-[11px] text-medium-grey">Reports received</p>
                    </div>
                  </div>

                  <div v-if="accountDetail[u.id].businesses.length > 0" class="space-y-1.5">
                    <p class="text-xs font-semibold uppercase tracking-wide text-medium-grey">Businesses</p>
                    <div
                      v-for="b in accountDetail[u.id].businesses"
                      :key="b.id"
                      class="flex items-center justify-between rounded-control bg-white px-3 py-2 text-sm"
                    >
                      <span class="text-charcoal">{{ b.businessName }}</span>
                      <span
                        class="badge"
                        :class="{
                          'bg-success/15 text-success': b.verificationStatus === 'VERIFIED',
                          'bg-warning/15 text-warning': b.verificationStatus === 'PENDING',
                          'bg-danger/15 text-danger': b.verificationStatus === 'REJECTED',
                        }"
                      >
                        {{ b.verificationStatus }}
                      </span>
                    </div>
                  </div>

                  <div v-if="accountDetail[u.id].reportsReceived.length > 0" class="space-y-1.5">
                    <p class="text-xs font-semibold uppercase tracking-wide text-medium-grey">Reports received</p>
                    <div
                      v-for="r in accountDetail[u.id].reportsReceived"
                      :key="r.id"
                      class="rounded-control bg-danger/10 px-3 py-2 text-sm text-charcoal"
                    >
                      {{ REASON_LABELS[r.reason] ?? r.reason }} on "{{ r.target.label }}" &middot;
                      <span class="font-medium">{{ STATUS_LABELS[r.status] }}</span>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </template>
      </section>

      <!-- Announcements section -->
      <section v-else-if="activeSection === 'announcements'" class="space-y-5">
        <p v-if="announcementsError" class="text-sm text-danger">{{ announcementsError }}</p>

        <div class="card space-y-3">
          <h2 class="font-display text-base font-semibold text-uni-navy">New announcement</h2>
          <textarea
            v-model="newAnnouncementMessage"
            rows="2"
            placeholder="Message shown to every visitor..."
            class="input-field resize-y"
          ></textarea>
          <label class="flex items-center gap-2 text-sm text-charcoal">
            <input v-model="newAnnouncementActive" type="checkbox" class="accent-campus-teal" />
            Active - show this banner site-wide now
          </label>
          <button
            class="btn-primary text-sm"
            :disabled="publishingAnnouncement || !newAnnouncementMessage.trim()"
            @click="publishAnnouncement"
          >
            {{ publishingAnnouncement ? "Publishing..." : "Publish announcement" }}
          </button>
        </div>

        <div class="space-y-2">
          <h2 class="font-display text-base font-semibold text-uni-navy">History</h2>
          <p v-if="announcementsLoading" class="text-sm text-medium-grey">Loading...</p>
          <p v-else-if="announcements.length === 0" class="card text-sm text-medium-grey">No announcements published yet.</p>
          <div v-else class="flex flex-col gap-2">
            <div
              v-for="a in announcements"
              :key="a.id"
              class="card flex items-center justify-between gap-3"
              :class="{ 'opacity-70': !a.active }"
            >
              <div>
                <p class="text-sm text-charcoal">{{ a.message }}</p>
                <p class="text-xs text-medium-grey">Posted {{ relativeTime(a.createdAt) }}</p>
              </div>
              <div class="flex shrink-0 items-center gap-2">
                <span class="badge" :class="a.active ? 'bg-success/15 text-success' : 'bg-medium-grey/15 text-medium-grey'">
                  {{ a.active ? "Active" : "Inactive" }}
                </span>
                <button
                  v-if="a.active"
                  class="text-xs font-semibold text-danger disabled:opacity-50"
                  :disabled="decidingAnnouncementId === a.id"
                  @click="deactivateAnnouncement(a.id)"
                >
                  Deactivate
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Activity log section -->
      <section v-else-if="activeSection === 'activity'" class="space-y-5">
        <p v-if="activityError" class="text-sm text-danger">{{ activityError }}</p>

        <div class="flex flex-wrap gap-2">
          <button
            v-for="f in activityFilters"
            :key="f.value"
            class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
            :class="activityFilter === f.value ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
            @click="setActivityFilter(f.value)"
          >
            {{ f.label }}
          </button>
        </div>

        <p v-if="activityLoading" class="text-sm text-medium-grey">Loading...</p>
        <div v-else-if="activityEntries.length === 0" class="card text-sm text-medium-grey">No activity in this filter yet.</div>

        <div v-else class="card divide-y divide-light-grey !p-0">
          <div v-for="entry in activityEntries" :key="entry.id" class="flex items-start gap-3 px-4 py-3">
            <span
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
              :class="{
                'bg-success/15 text-success': entry.category === 'BUSINESS',
                'bg-warning/15 text-warning': entry.category === 'ACCOUNT',
                'bg-charcoal/10 text-charcoal': entry.category === 'REPORT',
                'bg-academic-gold/20 text-uni-navy': entry.category === 'REVIEW',
                'bg-info/15 text-info': entry.category === 'ANNOUNCEMENT',
              }"
            >
              {{ CATEGORY_ICONS[entry.category] }}
            </span>
            <div class="flex-1">
              <p class="text-sm text-charcoal">{{ entry.description }}</p>
              <p class="text-xs text-medium-grey">by {{ entry.adminName }} &middot; {{ activityRelativeTime(entry.createdAt) }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Bookings section -->
      <section v-else-if="activeSection === 'bookings'" class="space-y-5">
        <p v-if="bookingStatsError" class="text-sm text-danger">{{ bookingStatsError }}</p>
        <p v-else-if="bookingStatsLoading || !bookingStats" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Total bookings</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ bookingStats.total }}</p>
              <p class="text-xs text-medium-grey">all time</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Pending</p>
              <p class="font-display text-2xl font-bold text-warning">{{ bookingStats.pending }}</p>
              <p class="text-xs text-medium-grey">awaiting seller reply</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Accepted</p>
              <p class="font-display text-2xl font-bold text-success">{{ bookingStats.accepted }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Declined</p>
              <p class="font-display text-2xl font-bold text-danger">{{ bookingStats.declined }}</p>
            </div>
          </div>

          <div class="card space-y-2.5">
            <h3 class="font-display text-sm font-semibold text-uni-navy">Most-booked services</h3>
            <p v-if="bookingStats.mostBooked.length === 0" class="text-sm text-medium-grey">No bookings yet.</p>
            <div v-for="s in bookingStats.mostBooked" :key="s.listingName + s.businessName" class="flex items-center justify-between text-sm">
              <span class="text-charcoal">{{ s.listingName }} &middot; {{ s.businessName }}</span>
              <span class="badge bg-academic-gold/20 text-uni-navy">{{ s.count }} booking{{ s.count === 1 ? "" : "s" }}</span>
            </div>
          </div>
        </template>
      </section>

      <!-- Reviews section -->
      <section v-else-if="activeSection === 'reviews'" class="space-y-5">
        <p v-if="reviewsError" class="text-sm text-danger">{{ reviewsError }}</p>
        <p v-else-if="reviewsLoading || !reviewStats" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Platform average</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ reviewStats.platformAverage || "-" }} <span class="text-base text-academic-gold">★</span></p>
              <p class="text-xs text-medium-grey">across {{ reviewStats.totalReviews }} reviews</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Flagged reviews</p>
              <p class="font-display text-2xl font-bold text-danger">{{ reviewStats.flaggedCount }}</p>
              <p class="text-xs text-medium-grey">need moderation</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Reviewed businesses</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ reviewStats.reviewedBusinessCount }}</p>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="card space-y-2.5">
              <h3 class="font-display text-sm font-semibold text-uni-navy">Top rated</h3>
              <p v-if="reviewStats.topRated.length === 0" class="text-sm text-medium-grey">No reviews yet.</p>
              <div v-for="r in reviewStats.topRated" :key="r.businessName" class="flex items-center justify-between text-sm">
                <span class="text-charcoal">{{ r.businessName }}</span>
                <span class="font-medium text-academic-gold">{{ r.average }} ★</span>
              </div>
            </div>
            <div class="card space-y-2.5">
              <h3 class="font-display text-sm font-semibold text-uni-navy">Needs attention</h3>
              <p v-if="reviewStats.lowestRated.length === 0" class="text-sm text-medium-grey">No reviews yet.</p>
              <div v-for="r in reviewStats.lowestRated" :key="r.businessName" class="flex items-center justify-between text-sm">
                <span class="text-charcoal">{{ r.businessName }}</span>
                <span class="font-medium text-danger">{{ r.average }} ★</span>
              </div>
            </div>
          </div>

          <div class="space-y-2">
            <h3 class="font-display text-sm font-semibold text-uni-navy">Flagged for moderation</h3>
            <p v-if="flaggedReviews.length === 0" class="card text-sm text-medium-grey">No flagged reviews right now.</p>
            <div v-else class="card !p-0 divide-y divide-light-grey">
              <div v-for="r in flaggedReviews" :key="r.id" class="flex items-start justify-between gap-3 px-4 py-3.5">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-semibold text-uni-navy">{{ r.reviewerName }}</span>
                    <span class="text-sm text-academic-gold">
                      <template v-for="n in 5" :key="n"><span :class="n <= r.rating ? '' : 'text-light-grey'">★</span></template>
                    </span>
                    <span class="badge bg-danger/15 text-danger">Flagged &times;{{ r.flagCount }}</span>
                  </div>
                  <p v-if="r.comment" class="mt-1 text-sm text-charcoal">{{ r.comment }}</p>
                  <p class="mt-1 text-xs text-medium-grey">on {{ r.businessName }}</p>
                </div>
                <button
                  class="inline-flex shrink-0 items-center justify-center rounded-control border border-danger bg-white px-3 py-1.5 text-xs font-semibold text-danger disabled:opacity-50"
                  :disabled="removingReviewId === r.id"
                  @click="removeReview(r.id)"
                >
                  Remove
                </button>
              </div>
            </div>
          </div>
        </template>
      </section>

      <!-- Orders section -->
      <section v-else-if="activeSection === 'orders'" class="space-y-5">
        <p v-if="orderStatsError" class="text-sm text-danger">{{ orderStatsError }}</p>
        <p v-else-if="orderStatsLoading || !orderStats" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Total orders</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ orderStats.totalOrders }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Gross order value</p>
              <p class="font-display text-2xl font-bold text-uni-navy">R{{ orderStats.grossValue.toFixed(2) }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Placed / awaiting seller</p>
              <p class="font-display text-2xl font-bold text-warning">{{ orderStats.placed }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Completed</p>
              <p class="font-display text-2xl font-bold text-success">{{ orderStats.completed }}</p>
            </div>
          </div>

          <div class="card space-y-2.5">
            <h3 class="font-display text-sm font-semibold text-uni-navy">Recent orders</h3>
            <p v-if="orderStats.recentOrders.length === 0" class="text-sm text-medium-grey">No orders yet.</p>
            <table v-else class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-light-grey text-xs uppercase tracking-wide text-medium-grey">
                  <th class="pb-2 font-medium">Buyer</th>
                  <th class="pb-2 font-medium">Business</th>
                  <th class="pb-2 font-medium">Total</th>
                  <th class="pb-2 font-medium">Status</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-light-grey">
                <tr v-for="o in orderStats.recentOrders" :key="o.id">
                  <td class="py-2 text-charcoal">{{ o.buyerName }}</td>
                  <td class="py-2 text-charcoal">{{ o.businessName }}</td>
                  <td class="py-2 text-charcoal">R{{ o.total.toFixed(2) }}</td>
                  <td class="py-2">
                    <span
                      class="badge"
                      :class="{
                        'bg-warning/15 text-warning': o.status === 'PLACED',
                        'bg-info/15 text-info': o.status === 'CONFIRMED',
                        'bg-slate-blue/15 text-slate-blue': o.status === 'READY',
                        'bg-success/15 text-success': o.status === 'COMPLETED',
                        'bg-danger/15 text-danger': o.status === 'CANCELLED',
                      }"
                    >
                      {{ o.status.charAt(0) + o.status.slice(1).toLowerCase() }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </section>

      <!-- Promo codes section -->
      <section v-else-if="activeSection === 'promos'" class="space-y-5">
        <p v-if="promoStatsError" class="text-sm text-danger">{{ promoStatsError }}</p>
        <p v-else-if="promoStatsLoading || !promoStats" class="text-sm text-medium-grey">Loading...</p>

        <template v-else>
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Active promo codes</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ promoStats.activeCount }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Total redemptions</p>
              <p class="font-display text-2xl font-bold text-uni-navy">{{ promoStats.totalRedemptions }}</p>
            </div>
            <div class="card">
              <p class="text-xs uppercase tracking-wide text-medium-grey">Estimated discounts given</p>
              <p class="font-display text-2xl font-bold text-uni-navy">R{{ promoStats.totalDiscountGiven.toFixed(2) }}</p>
            </div>
          </div>

          <div class="card space-y-2.5">
            <h3 class="font-display text-sm font-semibold text-uni-navy">Most-used promo codes</h3>
            <p v-if="promoStats.topUsed.length === 0" class="text-sm text-medium-grey">No promo codes yet.</p>
            <div v-for="p in promoStats.topUsed" :key="p.code + p.businessName" class="flex items-center justify-between text-sm">
              <span class="text-charcoal">{{ p.code }} &middot; {{ p.businessName }} &middot; {{ p.discountLabel }}</span>
              <span class="badge" :class="p.active ? 'bg-success/15 text-success' : 'bg-medium-grey/15 text-medium-grey'">
                {{ p.usageCount }} redemption{{ p.usageCount === 1 ? "" : "s" }}
              </span>
            </div>
          </div>
        </template>
      </section>

      <!-- Broadcast notification section -->
      <section v-else-if="activeSection === 'broadcast'" class="max-w-2xl space-y-4">
        <div class="card space-y-3">
          <h2 class="font-display text-base font-semibold text-uni-navy">Send a targeted notification</h2>
          <p class="text-xs text-medium-grey">Unlike a site banner, this lands directly in the recipients' notification bell.</p>

          <label class="block text-xs font-semibold text-medium-grey">Audience</label>
          <select v-model="broadcastAudience" class="input-field">
            <option value="ALL_STUDENTS">All students</option>
            <option value="ALL_SELLERS">All sellers</option>
            <option value="PENDING_BUSINESS_OWNERS">Pending business owners</option>
          </select>

          <label class="block text-xs font-semibold text-medium-grey">Message</label>
          <textarea v-model="broadcastMessage" class="input-field" rows="3" placeholder="e.g. New: you can now request bookings for services!"></textarea>

          <p v-if="broadcastStatus" class="text-sm" :class="broadcastStatus.startsWith('Sent') ? 'text-success' : 'text-danger'">{{ broadcastStatus }}</p>

          <div class="flex justify-end">
            <button class="btn-primary text-sm" :disabled="broadcastSending || !broadcastMessage.trim()" @click="sendBroadcast">
              {{ broadcastSending ? "Sending..." : "Send notification" }}
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>
