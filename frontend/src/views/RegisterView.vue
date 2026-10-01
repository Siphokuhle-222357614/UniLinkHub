<script setup lang="ts">
import { computed, ref } from "vue";
import { CircleAlert, MailCheck, UserPlus } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { extractErrorMessage } from "@/lib/api";
import AuthLayout from "@/components/AuthLayout.vue";
import PasswordInput from "@/components/ui/PasswordInput.vue";

const auth = useAuthStore();

const studentNumber = ref("");
const firstName = ref("");
const lastName = ref("");
const email = ref("");
const password = ref("");
const error = ref("");
const success = ref(false);
const loading = ref(false);

// A rough guide only - the backend's own minimum (8 characters) is what's actually enforced.
const strength = computed(() => {
  const p = password.value;
  if (!p) return null;
  let score = 0;
  if (p.length >= 8) score++;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++;
  if (/\d/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  if (p.length < 8) return { level: 1, label: "Too short", color: "bg-danger", text: "text-danger" };
  if (score <= 2) return { level: 2, label: "Weak", color: "bg-warning", text: "text-warning" };
  if (score <= 3) return { level: 3, label: "Good", color: "bg-teal-500", text: "text-teal-600" };
  return { level: 4, label: "Strong", color: "bg-success", text: "text-success" };
});

// Shown as soon as they've typed a whole address; the backend enforces the same rule.
const emailLooksWrong = computed(() => email.value.includes("@") && email.value.includes(".") && !email.value.trim().toLowerCase().endsWith("@mycput.ac.za"));

async function submit() {
  if (emailLooksWrong.value) return;
  loading.value = true;
  error.value = "";
  try {
    await auth.register({
      studentNumber: studentNumber.value,
      firstName: firstName.value,
      lastName: lastName.value,
      email: email.value,
      password: password.value,
    });
    success.value = true;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthLayout
    v-if="success"
    title="Check your inbox"
    subtitle="Your account has been created - one last step."
  >
    <div class="rounded-card border border-emerald-200 bg-emerald-50 p-5">
      <div class="flex gap-3">
        <MailCheck class="h-5 w-5 shrink-0 text-emerald-600" />
        <p class="text-sm leading-relaxed text-emerald-900">
          We've sent a verification link to <span class="font-semibold">{{ email }}</span>. Click it to activate your account, then
          log in. It can take a minute to arrive - check your spam folder too. The link works for 48 hours.
        </p>
      </div>
    </div>
    <RouterLink to="/login" class="btn-primary mt-6 w-full py-3">Go to login</RouterLink>
  </AuthLayout>

  <AuthLayout v-else title="Join UniLinkHub" subtitle="Create your free student account to buy, sell and book services on campus.">
    <form class="space-y-4" @submit.prevent="submit">
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label for="reg-first" class="field-label">First name</label>
          <input id="reg-first" v-model="firstName" required autocomplete="given-name" class="input-field" />
        </div>
        <div>
          <label for="reg-last" class="field-label">Last name</label>
          <input id="reg-last" v-model="lastName" required autocomplete="family-name" class="input-field" />
        </div>
      </div>
      <div>
        <label for="reg-student" class="field-label">Student number</label>
        <input id="reg-student" v-model="studentNumber" required inputmode="numeric" placeholder="e.g. 222357614" class="input-field" />
      </div>
      <div>
        <label for="reg-email" class="field-label">CPUT student email</label>
        <input id="reg-email" v-model="email" type="email" required autocomplete="email" placeholder="you@mycput.ac.za" class="input-field" />
        <p class="mt-1.5 text-xs" :class="emailLooksWrong ? 'font-medium text-danger' : 'text-medium-grey'">
          {{ emailLooksWrong ? "This must be your CPUT student email - it ends in @mycput.ac.za." : "We'll send a link to this address to confirm you're a CPUT student." }}
        </p>
      </div>
      <div>
        <label for="reg-password" class="field-label">Password</label>
        <PasswordInput id="reg-password" v-model="password" :minlength="8" autocomplete="new-password" placeholder="At least 8 characters" />
        <div v-if="strength" class="mt-2 flex items-center gap-3">
          <div class="grid flex-1 grid-cols-4 gap-1">
            <span v-for="n in 4" :key="n" class="h-1 rounded-full transition" :class="n <= strength.level ? strength.color : 'bg-light-grey'"></span>
          </div>
          <span class="w-16 text-right text-xs font-semibold" :class="strength.text">{{ strength.label }}</span>
        </div>
      </div>

      <p v-if="error" class="flex items-start gap-2 rounded-control border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-danger">
        <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" /> {{ error }}
      </p>

      <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
        <span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
        <UserPlus v-else class="h-4 w-4" />
        {{ loading ? "Creating account…" : "Create account" }}
      </button>
    </form>

    <p class="mt-6 text-center text-xs leading-relaxed text-medium-grey">
      By creating an account you agree to the <RouterLink to="/marketplace-rules" class="link">marketplace rules</RouterLink>.
    </p>
    <p class="mt-3 text-center text-sm text-medium-grey">
      Already have an account? <RouterLink to="/login" class="link">Log in</RouterLink>
    </p>
  </AuthLayout>
</template>
