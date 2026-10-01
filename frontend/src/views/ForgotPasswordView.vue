<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { CircleAlert, CircleCheck } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import AuthLayout from "@/components/AuthLayout.vue";
import PasswordInput from "@/components/ui/PasswordInput.vue";

const router = useRouter();
const route = useRoute();
// Admin invites reuse this page: the emailed link carries ?token=...&invite=1.
const isInvite = ref(false);

// Two steps on one screen: request a code, then redeem it. Step 2 is reachable directly too,
// for someone who already has a code from an earlier request.
const step = ref<1 | 2>(1);

const email = ref("");
const requesting = ref(false);
const requestStatus = ref("");
const requestError = ref("");

async function requestReset() {
  requesting.value = true;
  requestStatus.value = "";
  requestError.value = "";
  try {
    await api.post("/auth/forgot-password", { email: email.value });
    requestStatus.value = "If that email has an account, we've sent it a link to reset your password. Check your inbox and spam folder.";
    step.value = 2;
  } catch (err) {
    requestError.value = extractErrorMessage(err);
  } finally {
    requesting.value = false;
  }
}

const token = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const resetting = ref(false);
const resetStatus = ref("");
const resetError = ref("");

async function resetPassword() {
  resetStatus.value = "";
  resetError.value = "";
  if (newPassword.value !== confirmPassword.value) {
    resetError.value = "New password and confirmation do not match.";
    return;
  }
  resetting.value = true;
  try {
    await api.post("/auth/reset-password", { token: token.value, newPassword: newPassword.value });
    resetStatus.value = isInvite.value ? "Password set! Taking you to login…" : "Password reset. Taking you to login…";
    setTimeout(() => router.push({ name: "login" }), 1500);
  } catch (err) {
    resetError.value = extractErrorMessage(err);
  } finally {
    resetting.value = false;
  }
}

onMounted(() => {
  if (typeof route.query.token === "string") {
    token.value = route.query.token;
    step.value = 2;
  }
  isInvite.value = route.query.invite === "1";
});
</script>

<template>
  <AuthLayout
    :title="isInvite ? 'Set up your admin account' : step === 1 ? 'Forgot your password?' : 'Choose a new password'"
    :subtitle="isInvite ? 'Choose a password for your new UniLinkHub admin account.' : step === 1 ? 'Enter your email and we\'ll send you a link to reset your password.' : 'Pick a new password for your account.'"
  >
    <div v-if="!isInvite" class="mb-6 grid grid-cols-2 gap-1 rounded-control bg-soft-grey p-1">
      <button
        v-for="n in [1, 2] as const"
        :key="n"
        class="rounded-lg py-2 text-xs font-semibold transition"
        :class="step === n ? 'bg-white text-uni-navy shadow-card' : 'text-medium-grey hover:text-uni-navy'"
        @click="step = n"
      >
        {{ n }}. {{ n === 1 ? "Request link" : "New password" }}
      </button>
    </div>

    <form v-if="step === 1" class="space-y-4" @submit.prevent="requestReset">
      <div>
        <label for="fp-email" class="field-label">Email address</label>
        <input id="fp-email" v-model="email" type="email" required autocomplete="email" placeholder="you@mycput.ac.za" class="input-field" />
      </div>
      <p v-if="requestError" class="flex items-start gap-2 rounded-control border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-danger">
        <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" /> {{ requestError }}
      </p>
      <button type="submit" class="btn-primary w-full py-3" :disabled="requesting">
        {{ requesting ? "Sending…" : "Email me a reset link" }}
      </button>
    </form>

    <form v-else class="space-y-4" @submit.prevent="resetPassword">
      <p v-if="requestStatus" class="flex items-start gap-2 rounded-control border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-800">
        <CircleCheck class="mt-0.5 h-4 w-4 shrink-0" /> {{ requestStatus }}
      </p>
      <div v-if="!route.query.token">
        <label for="fp-token" class="field-label">Reset code</label>
        <input id="fp-token" v-model="token" required placeholder="Paste the code from your email" class="input-field font-mono text-xs" />
        <p class="mt-1.5 text-xs text-medium-grey">Easiest: just click the button in the email - it fills this in for you.</p>
      </div>
      <div>
        <label for="fp-new" class="field-label">New password</label>
        <PasswordInput id="fp-new" v-model="newPassword" autocomplete="new-password" />
      </div>
      <div>
        <label for="fp-confirm" class="field-label">Confirm new password</label>
        <PasswordInput id="fp-confirm" v-model="confirmPassword" autocomplete="new-password" />
      </div>
      <p v-if="resetError" class="flex items-start gap-2 rounded-control border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-danger">
        <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" /> {{ resetError }}
      </p>
      <p v-else-if="resetStatus" class="flex items-start gap-2 rounded-control border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-800">
        <CircleCheck class="mt-0.5 h-4 w-4 shrink-0" /> {{ resetStatus }}
      </p>
      <button type="submit" class="btn-primary w-full py-3" :disabled="resetting">
        {{ resetting ? "Saving…" : isInvite ? "Set password" : "Reset password" }}
      </button>
    </form>

    <p class="mt-8 text-center text-sm text-medium-grey">
      Remembered it after all? <RouterLink to="/login" class="link">Back to log in</RouterLink>
    </p>
  </AuthLayout>
</template>
