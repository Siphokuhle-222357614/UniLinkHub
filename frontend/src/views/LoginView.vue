<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { CircleAlert, LogIn } from "@lucide/vue";
import { useAuthStore } from "@/stores/auth";
import { extractErrorMessage } from "@/lib/api";
import AuthLayout from "@/components/AuthLayout.vue";
import PasswordInput from "@/components/ui/PasswordInput.vue";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const email = ref("");
const password = ref("");
const error = ref("");
const loading = ref(false);

async function submit() {
  loading.value = true;
  error.value = "";
  try {
    await auth.login(email.value, password.value);
    const redirect = (route.query.redirect as string) || "/dashboard";
    router.push(redirect);
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthLayout title="Welcome back" subtitle="Log in with your student email to pick up where you left off.">
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label for="login-email" class="field-label">Student email</label>
        <input id="login-email" v-model="email" type="email" required autocomplete="email" placeholder="you@mycput.ac.za" class="input-field" />
      </div>
      <div>
        <div class="mb-1.5 flex items-center justify-between">
          <label for="login-password" class="text-xs font-semibold text-charcoal">Password</label>
          <RouterLink to="/forgot-password" class="text-xs font-semibold text-teal-600 hover:text-teal-700">Forgot password?</RouterLink>
        </div>
        <PasswordInput id="login-password" v-model="password" autocomplete="current-password" placeholder="••••••••" />
      </div>

      <p v-if="error" class="flex items-start gap-2 rounded-control border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-danger">
        <CircleAlert class="mt-0.5 h-4 w-4 shrink-0" /> {{ error }}
      </p>

      <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
        <span v-if="loading" class="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
        <LogIn v-else class="h-4 w-4" />
        {{ loading ? "Logging in…" : "Log in" }}
      </button>
    </form>

    <p class="mt-8 text-center text-sm text-medium-grey">
      New to UniLinkHub? <RouterLink to="/register" class="link">Create a free account</RouterLink>
    </p>
  </AuthLayout>
</template>
