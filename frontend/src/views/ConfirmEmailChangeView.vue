<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { CircleCheck, CircleX, LoaderCircle } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import AuthLayout from "@/components/AuthLayout.vue";
import type { UserResponse } from "@/lib/types";

const route = useRoute();
const auth = useAuthStore();
const state = ref<"working" | "done" | "failed">("working");
const error = ref("");
const newEmail = ref("");

onMounted(async () => {
  const token = typeof route.query.token === "string" ? route.query.token : "";
  if (!token) {
    state.value = "failed";
    error.value = "This link is missing its confirmation code. Please open the link straight from your email.";
    return;
  }
  try {
    const { data } = await api.get<UserResponse>("/auth/confirm-email-change", { params: { token } });
    newEmail.value = data.email;
    state.value = "done";
    // Sessions are tied to the old address, so start a fresh one with the new email.
    auth.clearSession();
  } catch (err) {
    state.value = "failed";
    error.value = extractErrorMessage(err);
  }
});
</script>

<template>
  <AuthLayout :title="state === 'done' ? 'Email address updated' : state === 'failed' ? 'We couldn\'t confirm that link' : 'Confirming…'">
    <div v-if="state === 'working'" class="flex items-center gap-3 text-sm text-medium-grey">
      <LoaderCircle class="h-5 w-5 animate-spin text-teal-600" /> Checking your link…
    </div>
    <template v-else-if="state === 'done'">
      <div class="flex gap-3 rounded-card border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
        <CircleCheck class="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
        Your account now uses {{ newEmail }}. Please log in again with your new address.
      </div>
      <RouterLink to="/login" class="btn-primary mt-6 w-full py-3">Log in</RouterLink>
    </template>
    <template v-else>
      <div class="flex gap-3 rounded-card border border-red-200 bg-red-50 p-4 text-sm text-red-900">
        <CircleX class="mt-0.5 h-5 w-5 shrink-0 text-danger" /> {{ error }}
      </div>
      <RouterLink to="/account" class="btn-secondary mt-6 w-full py-3">Back to account settings</RouterLink>
    </template>
  </AuthLayout>
</template>
