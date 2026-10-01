<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { CircleCheck, CircleX, LoaderCircle, MailCheck } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import AuthLayout from "@/components/AuthLayout.vue";

const route = useRoute();
const state = ref<"working" | "done" | "failed">("working");
const error = ref("");

const resendEmail = ref("");
const resending = ref(false);
const resent = ref(false);

onMounted(async () => {
  const token = typeof route.query.token === "string" ? route.query.token : "";
  if (!token) {
    state.value = "failed";
    error.value = "This link is missing its verification code. Please open the link straight from your email.";
    return;
  }
  try {
    await api.get("/auth/verify", { params: { token } });
    state.value = "done";
  } catch (err) {
    state.value = "failed";
    error.value = extractErrorMessage(err);
  }
});

async function resend() {
  resending.value = true;
  try {
    await api.post("/auth/resend-verification", { email: resendEmail.value });
    resent.value = true;
  } catch {
    // Errors are shown by the form's own validation; the endpoint itself never reveals whether an email exists.
  } finally {
    resending.value = false;
  }
}
</script>

<template>
  <AuthLayout
    :title="state === 'done' ? 'Email verified!' : state === 'failed' ? 'We couldn\'t verify that link' : 'Verifying your email…'"
    :subtitle="state === 'done' ? 'Your UniLinkHub account is active. You can log in now.' : undefined"
  >
    <div v-if="state === 'working'" class="flex items-center gap-3 text-sm text-medium-grey">
      <LoaderCircle class="h-5 w-5 animate-spin text-teal-600" /> Checking your link…
    </div>

    <template v-else-if="state === 'done'">
      <div class="flex items-center gap-3 rounded-card border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
        <CircleCheck class="h-5 w-5 shrink-0 text-emerald-600" /> Thanks for confirming your CPUT student email address.
      </div>
      <RouterLink to="/login" class="btn-primary mt-6 w-full py-3">Log in</RouterLink>
    </template>

    <template v-else>
      <div class="flex gap-3 rounded-card border border-red-200 bg-red-50 p-4 text-sm text-red-900">
        <CircleX class="mt-0.5 h-5 w-5 shrink-0 text-danger" /> {{ error }}
      </div>

      <form v-if="!resent" class="mt-6 space-y-3" @submit.prevent="resend">
        <label for="resend-email" class="field-label">Send me a new verification link</label>
        <input id="resend-email" v-model="resendEmail" type="email" required placeholder="you@mycput.ac.za" class="input-field" />
        <button type="submit" class="btn-primary w-full py-3" :disabled="resending">{{ resending ? "Sending…" : "Send a new link" }}</button>
      </form>
      <div v-else class="mt-6 flex gap-3 rounded-card border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
        <MailCheck class="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
        If that address has an account waiting to be verified, a new link is on its way. Check your inbox and spam folder.
      </div>
      <p class="mt-8 text-center text-sm text-medium-grey">Already verified? <RouterLink to="/login" class="link">Log in</RouterLink></p>
    </template>
  </AuthLayout>
</template>
