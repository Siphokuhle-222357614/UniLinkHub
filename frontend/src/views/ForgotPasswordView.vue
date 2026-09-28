<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { CircleAlert, CircleCheck, Info } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import AuthLayout from "@/components/AuthLayout.vue";
import PasswordInput from "@/components/ui/PasswordInput.vue";

const router = useRouter();

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
    requestStatus.value = "If that email is registered, a reset code has been sent.";
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
    resetStatus.value = "Password reset. Redirecting to login…";
    setTimeout(() => router.push({ name: "login" }), 1500);
  } catch (err) {
    resetError.value = extractErrorMessage(err);
  } finally {
    resetting.value = false;
  }
}
</script>

<template>
  <AuthLayout
    :title="step === 1 ? 'Forgot your password?' : 'Choose a new password'"
    :subtitle="step === 1 ? 'Enter your student email and we\'ll send you a reset code.' : 'Paste the reset code you received, then pick a new password.'"
  >
    <div class="mb-6 grid grid-cols-2 gap-1 rounded-control bg-soft-grey p-1">
      <button
        v-for="n in [1, 2] as const"
        :key="n"
        class="rounded-lg py-2 text-xs font-semibold transition"
        :class="step === n ? 'bg-white text-uni-navy shadow-card' : 'text-medium-grey hover:text-uni-navy'"
        @click="step = n"
      >
        {{ n }}. {{ n === 1 ? "Request code" : "Reset password" }}
      </button>
    </div>

    <form v-if="step === 1" class="space-y-4" @submit.prevent="requestReset">
      <div>
        <label for="fp-email" class="field-label">Student email</label>
        <input id="fp-email" v-model="email" type="email" required autocomplete="email" placeholder="you@mycput.ac.za" class="input-field" />
      </div>
      <p v-if="requestError" class="flex items-start gap-2 rounded-control border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-danger">
        <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" /> {{ requestError }}
      </p>
      <button type="submit" class="btn-primary w-full py-3" :disabled="requesting">
        {{ requesting ? "Sending…" : "Send reset code" }}
      </button>
    </form>

    <form v-else class="space-y-4" @submit.prevent="resetPassword">
      <p v-if="requestStatus" class="flex items-start gap-2 rounded-control border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-800">
        <CircleCheck class="mt-0.5 h-4 w-4 shrink-0" /> {{ requestStatus }}
      </p>
      <div>
        <label for="fp-token" class="field-label">Reset code</label>
        <input id="fp-token" v-model="token" required placeholder="Paste your code" class="input-field font-mono text-xs" />
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
        {{ resetting ? "Resetting…" : "Reset password" }}
      </button>
      <p class="flex gap-2 rounded-control bg-soft-grey p-3 text-xs text-medium-grey">
        <Info class="h-4 w-4 shrink-0" />
        No SMTP provider is wired up yet in dev - the reset code is logged to the backend console the same way the verification link is.
      </p>
    </form>

    <p class="mt-8 text-center text-sm text-medium-grey">
      Remembered it after all? <RouterLink to="/login" class="link">Back to log in</RouterLink>
    </p>
  </AuthLayout>
</template>
