<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useToastStore } from "@/stores/toast";
import { useCategories } from "@/lib/categories";
import type { BusinessDTO, ListingDTO, ReportSummaryView } from "@/lib/types";

const router = useRouter();
const auth = useAuthStore();
const saved = useSavedListingsStore();
const followed = useFollowedProvidersStore();
const categories = useCategories();
const toast = useToastStore();

// ---- Profile ----
const profileForm = ref({ firstName: "", lastName: "", phoneNumber: "" });
const savingProfile = ref(false);
const profileStatus = ref("");
const profileError = ref("");

function resetProfileForm() {
  profileForm.value = {
    firstName: auth.user?.firstName ?? "",
    lastName: auth.user?.lastName ?? "",
    phoneNumber: auth.user?.phoneNumber ?? "",
  };
}

async function saveProfile() {
  savingProfile.value = true;
  profileStatus.value = "";
  profileError.value = "";
  try {
    await auth.updateProfile(profileForm.value);
    profileStatus.value = "Profile updated.";
    toast.success("Profile updated!");
  } catch (err) {
    profileError.value = extractErrorMessage(err);
  } finally {
    savingProfile.value = false;
  }
}

// ---- Business ----
const businesses = ref<BusinessDTO[]>([]);
const selectedBusinessId = ref("");
const businessForm = ref({ businessName: "", description: "", category: "", imageUrl: "" });
const savingBusiness = ref(false);
const businessStatus = ref("");
const businessError = ref("");

const selectedBusiness = computed(() => businesses.value.find((b) => b.id === selectedBusinessId.value) ?? null);

watch(selectedBusiness, (business) => {
  if (business) {
    businessForm.value = {
      businessName: business.businessName,
      description: business.description,
      category: business.category,
      imageUrl: business.imageUrl ?? "",
    };
  }
});

async function loadBusinesses() {
  if (!auth.isSeller) return;
  try {
    const { data } = await api.get<BusinessDTO[]>("/businesses/mine");
    businesses.value = data;
    if (data.length > 0) {
      selectedBusinessId.value = data[0].id;
    }
  } catch (err) {
    businessError.value = extractErrorMessage(err);
  }
}

async function saveBusiness() {
  if (!selectedBusiness.value) return;
  savingBusiness.value = true;
  businessStatus.value = "";
  businessError.value = "";
  try {
    const { data } = await api.patch<BusinessDTO>(`/businesses/${selectedBusiness.value.id}`, businessForm.value);
    const index = businesses.value.findIndex((b) => b.id === data.id);
    if (index !== -1) businesses.value[index] = data;
    businessStatus.value = "Business details updated.";
  } catch (err) {
    businessError.value = extractErrorMessage(err);
  } finally {
    savingBusiness.value = false;
  }
}

const resubmitting = ref(false);

async function resubmitVerification() {
  if (!selectedBusiness.value) return;
  resubmitting.value = true;
  businessStatus.value = "";
  businessError.value = "";
  try {
    const { data } = await api.post<BusinessDTO>(`/businesses/${selectedBusiness.value.id}/request-verification`);
    const index = businesses.value.findIndex((b) => b.id === data.id);
    if (index !== -1) businesses.value[index] = data;
    businessStatus.value = "Resubmitted - an admin will take another look.";
  } catch (err) {
    businessError.value = extractErrorMessage(err);
  } finally {
    resubmitting.value = false;
  }
}

// ---- Password ----
const passwordForm = ref({ currentPassword: "", newPassword: "", confirmPassword: "" });
const savingPassword = ref(false);
const passwordStatus = ref("");
const passwordError = ref("");

async function savePassword() {
  passwordStatus.value = "";
  passwordError.value = "";
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = "New password and confirmation do not match.";
    return;
  }
  savingPassword.value = true;
  try {
    await api.post("/users/me/change-password", {
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
    });
    passwordStatus.value = "Password updated.";
    toast.success("Password updated!");
    passwordForm.value = { currentPassword: "", newPassword: "", confirmPassword: "" };
  } catch (err) {
    passwordError.value = extractErrorMessage(err);
  } finally {
    savingPassword.value = false;
  }
}

// ---- Change email ----
const emailForm = ref({ newEmail: "", currentPassword: "" });
const savingEmail = ref(false);
const emailStatus = ref("");
const emailError = ref("");

async function changeEmail() {
  savingEmail.value = true;
  emailStatus.value = "";
  emailError.value = "";
  try {
    await api.post("/users/me/change-email", emailForm.value);
    emailStatus.value = "If that address isn't already in use, a confirmation link has been sent.";
    emailForm.value = { newEmail: "", currentPassword: "" };
    await auth.fetchCurrentUser();
  } catch (err) {
    emailError.value = extractErrorMessage(err);
  } finally {
    savingEmail.value = false;
  }
}

// ---- My activity ----
const activity = ref({ listingsCount: 0, savedCount: 0, followingCount: 0, reportsFiledCount: 0 });

async function loadActivity() {
  try {
    const [{ data: listings }, { data: reports }] = await Promise.all([
      auth.isSeller ? api.get<ListingDTO[]>("/listings/mine") : Promise.resolve({ data: [] as ListingDTO[] }),
      api.get<ReportSummaryView[]>("/reports/mine"),
    ]);
    activity.value = {
      listingsCount: listings.length,
      savedCount: saved.listings.length,
      followingCount: followed.providers.length,
      reportsFiledCount: reports.length,
    };
  } catch {
    // My activity is a nice-to-have summary; ignore failures here.
  }
}

// ---- Notification preferences ----
const NOTIFICATION_CATEGORIES: { value: string; label: string; description: string }[] = [
  { value: "BUSINESS", label: "Business verification updates", description: "When your business is verified or rejected" },
  { value: "BOOKING", label: "Booking requests", description: "New requests, and accept/decline updates" },
  { value: "ORDER", label: "Orders", description: "New orders, and confirm/ready/cancel updates" },
  { value: "MESSAGE", label: "Messages", description: "New direct messages from buyers or sellers" },
  { value: "QUESTION", label: "Listing Q&A", description: "New questions on your listings, and answers to yours" },
  { value: "STOCK", label: "Stock & price alerts", description: "When a saved listing restocks, or your stock runs low" },
  { value: "REVIEW", label: "New reviews", description: "When someone reviews your business" },
  { value: "SAVED_SEARCH", label: "Saved search matches", description: "When a new listing matches one of your saved searches" },
  { value: "ANNOUNCEMENT", label: "Site announcements", description: "Platform updates sent to your notification bell" },
];
const enabledCategories = ref<Set<string>>(new Set(NOTIFICATION_CATEGORIES.map((c) => c.value)));
const savingNotificationPrefs = ref(false);
const notificationPrefsStatus = ref("");

function resetNotificationPrefsForm() {
  const disabled = new Set(auth.user?.disabledNotificationCategories ?? []);
  enabledCategories.value = new Set(NOTIFICATION_CATEGORIES.map((c) => c.value).filter((c) => !disabled.has(c)));
}

function toggleCategory(value: string) {
  if (enabledCategories.value.has(value)) {
    enabledCategories.value.delete(value);
  } else {
    enabledCategories.value.add(value);
  }
}

async function saveNotificationPreferences() {
  savingNotificationPrefs.value = true;
  notificationPrefsStatus.value = "";
  try {
    const disabledCategories = NOTIFICATION_CATEGORIES.map((c) => c.value).filter((c) => !enabledCategories.value.has(c));
    await api.patch("/users/me/notification-preferences", { disabledCategories });
    notificationPrefsStatus.value = "Preferences saved.";
    toast.success("Notification preferences saved!");
    await auth.fetchCurrentUser();
  } catch (err) {
    notificationPrefsStatus.value = extractErrorMessage(err);
  } finally {
    savingNotificationPrefs.value = false;
  }
}

// ---- Deactivate account ----
const deactivateConfirmOpen = ref(false);
const deactivatePassword = ref("");
const deactivating = ref(false);
const deactivateError = ref("");

async function confirmDeactivate() {
  deactivating.value = true;
  deactivateError.value = "";
  try {
    await api.post("/users/me/deactivate", { currentPassword: deactivatePassword.value });
    auth.logout();
    router.push({ name: "login" });
  } catch (err) {
    deactivateError.value = extractErrorMessage(err);
  } finally {
    deactivating.value = false;
  }
}

onMounted(async () => {
  resetProfileForm();
  resetNotificationPrefsForm();
  await loadBusinesses();
  await Promise.all([saved.fetchSaved(), followed.fetchFollowed()]);
  await loadActivity();
});
</script>

<template>
  <section class="mx-auto max-w-2xl space-y-6">
    <h1 class="font-display text-xl font-bold text-uni-navy">My account</h1>

    <!-- Profile -->
    <div class="card space-y-3">
      <h2 class="font-display text-base font-semibold text-uni-navy">Your profile</h2>
      <div class="grid gap-3 sm:grid-cols-2">
        <div>
          <label class="mb-1 block text-xs font-medium text-medium-grey">First name</label>
          <input v-model="profileForm.firstName" class="input-field" />
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-medium-grey">Last name</label>
          <input v-model="profileForm.lastName" class="input-field" />
        </div>
        <div class="sm:col-span-2">
          <label class="mb-1 block text-xs font-medium text-medium-grey">Email</label>
          <input :value="auth.user?.email" disabled class="input-field bg-soft-grey text-medium-grey" />
        </div>
        <div class="sm:col-span-2">
          <label class="mb-1 block text-xs font-medium text-medium-grey">Contact number</label>
          <input v-model="profileForm.phoneNumber" class="input-field" />
        </div>
      </div>
      <p v-if="profileError" class="text-sm text-danger">{{ profileError }}</p>
      <p v-else-if="profileStatus" class="text-sm text-success">{{ profileStatus }}</p>
      <div class="flex justify-end">
        <button class="btn-primary text-sm" :disabled="savingProfile" @click="saveProfile">
          {{ savingProfile ? "Saving..." : "Save profile" }}
        </button>
      </div>
    </div>

    <!-- My activity -->
    <div class="card space-y-3">
      <h2 class="font-display text-base font-semibold text-uni-navy">My activity</h2>
      <div class="grid grid-cols-2 gap-3 text-center sm:grid-cols-4">
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ businesses.length }}</p>
          <p class="text-[11px] text-medium-grey">Businesses</p>
        </div>
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ activity.listingsCount }}</p>
          <p class="text-[11px] text-medium-grey">Listings</p>
        </div>
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ activity.savedCount }}</p>
          <p class="text-[11px] text-medium-grey">Saved</p>
        </div>
        <div>
          <p class="font-display text-xl font-bold text-uni-navy">{{ activity.followingCount }}</p>
          <p class="text-[11px] text-medium-grey">Following</p>
        </div>
      </div>
      <p class="border-t border-light-grey pt-3 text-xs text-medium-grey">
        {{ activity.reportsFiledCount }} report{{ activity.reportsFiledCount === 1 ? "" : "s" }} filed
      </p>
    </div>

    <!-- Business details -->
    <div v-if="auth.isSeller && businesses.length > 0" class="card space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-display text-base font-semibold text-uni-navy">Business details</h2>
        <select v-model="selectedBusinessId" class="input-field w-40 text-sm">
          <option v-for="b in businesses" :key="b.id" :value="b.id">{{ b.businessName }}</option>
        </select>
      </div>

      <div v-if="selectedBusiness?.verificationStatus === 'REJECTED'" class="flex items-start gap-2.5 rounded-control bg-danger/10 p-3">
        <span class="badge bg-danger/15 text-danger shrink-0">Rejected</span>
        <div class="text-[13px] text-charcoal">
          <p class="mb-1">This business wasn't approved on its last review:</p>
          <p v-if="selectedBusiness.rejectionReason" class="italic text-charcoal">"{{ selectedBusiness.rejectionReason }}"</p>
          <p v-else>Update the details below if needed, then resubmit for another look.</p>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <div
          v-if="businessForm.imageUrl"
          class="h-16 w-16 shrink-0 rounded-full border border-light-grey bg-cover bg-center"
          :style="{ backgroundImage: `url(${businessForm.imageUrl})` }"
        ></div>
        <div v-else class="flex h-16 w-16 shrink-0 items-center justify-center rounded-full border border-light-grey bg-soft-grey text-xs text-medium-grey">
          No logo
        </div>
        <div class="flex-1 space-y-1.5">
          <label class="block text-xs font-medium text-medium-grey">Logo URL <span class="font-normal">(optional)</span></label>
          <input v-model="businessForm.imageUrl" placeholder="https://..." class="input-field" />
        </div>
      </div>

      <div class="grid gap-3 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label class="mb-1 block text-xs font-medium text-medium-grey">Business name</label>
          <input v-model="businessForm.businessName" class="input-field" />
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-medium-grey">Category</label>
          <select v-model="businessForm.category" class="input-field">
            <option v-if="businessForm.category && !categories.includes(businessForm.category)" :value="businessForm.category">
              {{ businessForm.category }}
            </option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-xs font-medium text-medium-grey">Status</label>
          <input
            :value="selectedBusiness?.verificationStatus"
            disabled
            class="input-field bg-soft-grey font-medium"
            :class="{
              'text-success': selectedBusiness?.verificationStatus === 'VERIFIED',
              'text-warning': selectedBusiness?.verificationStatus === 'PENDING',
              'text-danger': selectedBusiness?.verificationStatus === 'REJECTED',
            }"
          />
        </div>
        <div class="sm:col-span-2">
          <label class="mb-1 block text-xs font-medium text-medium-grey">Description</label>
          <textarea v-model="businessForm.description" class="input-field" rows="2"></textarea>
        </div>
      </div>
      <p v-if="businessError" class="text-sm text-danger">{{ businessError }}</p>
      <p v-else-if="businessStatus" class="text-sm text-success">{{ businessStatus }}</p>
      <div class="flex flex-wrap justify-end gap-2">
        <button class="btn-secondary text-sm" :disabled="savingBusiness" @click="saveBusiness">
          {{ savingBusiness ? "Saving..." : "Save business" }}
        </button>
        <button
          v-if="selectedBusiness?.verificationStatus === 'REJECTED'"
          class="btn-primary text-sm"
          :disabled="resubmitting"
          @click="resubmitVerification"
        >
          {{ resubmitting ? "Resubmitting..." : "Resubmit for verification" }}
        </button>
      </div>
    </div>

    <!-- Change email -->
    <div class="card space-y-3">
      <h2 class="font-display text-base font-semibold text-uni-navy">Change email</h2>
      <div v-if="auth.user?.pendingEmail" class="flex items-center gap-2 rounded-control bg-warning/10 p-2.5 text-xs text-charcoal">
        <span class="badge bg-warning/15 text-warning shrink-0">Pending</span>
        New address {{ auth.user.pendingEmail }} awaiting verification - check the console link to confirm.
      </div>
      <div>
        <label class="mb-1 block text-xs font-medium text-medium-grey">New email</label>
        <input v-model="emailForm.newEmail" type="email" placeholder="new.email@mycput.ac.za" class="input-field" />
      </div>
      <div>
        <label class="mb-1 block text-xs font-medium text-medium-grey">Confirm password</label>
        <input v-model="emailForm.currentPassword" type="password" class="input-field" />
      </div>
      <p v-if="emailError" class="text-sm text-danger">{{ emailError }}</p>
      <p v-else-if="emailStatus" class="text-sm text-success">{{ emailStatus }}</p>
      <div class="flex justify-end">
        <button class="btn-primary text-sm" :disabled="savingEmail" @click="changeEmail">
          {{ savingEmail ? "Sending..." : "Send verification link" }}
        </button>
      </div>
    </div>

    <!-- Change password -->
    <div class="card space-y-3">
      <h2 class="font-display text-base font-semibold text-uni-navy">Change password</h2>
      <div class="grid gap-3">
        <div>
          <label class="mb-1 block text-xs font-medium text-medium-grey">Current password</label>
          <input v-model="passwordForm.currentPassword" type="password" class="input-field" />
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <div>
            <label class="mb-1 block text-xs font-medium text-medium-grey">New password</label>
            <input v-model="passwordForm.newPassword" type="password" placeholder="At least 8 characters" class="input-field" />
          </div>
          <div>
            <label class="mb-1 block text-xs font-medium text-medium-grey">Confirm new password</label>
            <input v-model="passwordForm.confirmPassword" type="password" class="input-field" />
          </div>
        </div>
      </div>
      <p v-if="passwordError" class="text-sm text-danger">{{ passwordError }}</p>
      <p v-else-if="passwordStatus" class="text-sm text-success">{{ passwordStatus }}</p>
      <div class="flex justify-end">
        <button class="btn-primary text-sm" :disabled="savingPassword" @click="savePassword">
          {{ savingPassword ? "Updating..." : "Update password" }}
        </button>
      </div>
    </div>

    <!-- Notification preferences -->
    <div class="card space-y-1">
      <h2 class="font-display text-base font-semibold text-uni-navy">Notification preferences</h2>
      <p class="mb-2 text-sm text-medium-grey">
        Choose which in-app notifications you'd like to receive - these only appear in the bell icon, there's no email delivery yet.
      </p>

      <label
        v-for="c in NOTIFICATION_CATEGORIES"
        :key="c.value"
        class="flex items-center justify-between border-t border-light-grey py-3"
      >
        <div>
          <p class="text-sm font-medium text-charcoal">{{ c.label }}</p>
          <p class="text-xs text-medium-grey">{{ c.description }}</p>
        </div>
        <input
          type="checkbox"
          class="h-5 w-5 accent-campus-teal"
          :checked="enabledCategories.has(c.value)"
          @change="toggleCategory(c.value)"
        />
      </label>

      <p v-if="notificationPrefsStatus" class="pt-1 text-sm" :class="notificationPrefsStatus === 'Preferences saved.' ? 'text-success' : 'text-danger'">
        {{ notificationPrefsStatus }}
      </p>
      <div class="flex justify-end pt-2">
        <button class="btn-primary text-sm" :disabled="savingNotificationPrefs" @click="saveNotificationPreferences">
          {{ savingNotificationPrefs ? "Saving..." : "Save preferences" }}
        </button>
      </div>
    </div>

    <!-- Danger zone -->
    <div class="card space-y-2 !border-danger/40">
      <h2 class="font-display text-base font-semibold text-danger">Danger zone</h2>
      <p class="text-sm text-charcoal">
        Deactivating hides your businesses and listings from buyers immediately. Nothing is deleted - log back
        in any time to reactivate.
      </p>
      <button
        class="inline-flex items-center justify-center rounded-control border border-danger bg-white px-4 py-2 text-sm font-semibold text-danger"
        @click="deactivateConfirmOpen = true; deactivatePassword = ''; deactivateError = ''"
      >
        Deactivate my account
      </button>
    </div>

    <div v-if="deactivateConfirmOpen" class="fixed inset-0 z-20 flex items-center justify-center bg-charcoal/40 p-6" @click.self="deactivateConfirmOpen = false">
      <div class="w-full max-w-sm rounded-modal border border-light-grey bg-white p-5 shadow-lg">
        <h2 class="font-display text-base font-bold text-danger">Deactivate your account?</h2>
        <p class="mt-2 text-sm text-charcoal">
          Your businesses and listings will be hidden from buyers immediately. You can reactivate any time by
          logging back in - nothing is deleted.
        </p>
        <label class="mt-4 block text-xs font-semibold text-medium-grey">Confirm your password</label>
        <input v-model="deactivatePassword" type="password" class="input-field mt-1" placeholder="Current password" />
        <p v-if="deactivateError" class="mt-2 text-sm text-danger">{{ deactivateError }}</p>
        <div class="mt-4 flex gap-2">
          <button
            class="inline-flex flex-1 items-center justify-center rounded-control bg-danger px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            :disabled="deactivating || !deactivatePassword"
            @click="confirmDeactivate"
          >
            {{ deactivating ? "Deactivating..." : "Deactivate account" }}
          </button>
          <button class="btn-secondary flex-1 text-sm" @click="deactivateConfirmOpen = false">Cancel</button>
        </div>
      </div>
    </div>
  </section>
</template>
