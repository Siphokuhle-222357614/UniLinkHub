<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Info, Send, ShieldCheck } from "@lucide/vue";
import { api, extractErrorMessage } from "@/lib/api";
import { initials } from "@/lib/format";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";
import type { UserResponse } from "@/lib/types";

const auth = useAuthStore();
const toast = useToastStore();

const admins = ref<UserResponse[]>([]);
const loading = ref(false);
const error = ref("");

const form = ref({ firstName: "", lastName: "", email: "" });
const inviting = ref(false);
const inviteError = ref("");

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<UserResponse[]>("/admin/admins");
    admins.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

async function invite() {
  inviting.value = true;
  inviteError.value = "";
  try {
    const { data } = await api.post<UserResponse>("/admin/admins", form.value);
    admins.value.push(data);
    toast.success("Invite sent", `${data.firstName} will get an email to set their password.`);
    form.value = { firstName: "", lastName: "", email: "" };
  } catch (err) {
    inviteError.value = extractErrorMessage(err);
  } finally {
    inviting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
    <div class="min-w-0 space-y-4">
      <div>
        <h2 class="section-title">Admin team</h2>
        <p class="text-sm text-medium-grey">Everyone who can use this console.</p>
      </div>
      <p v-if="error" class="rounded-control border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger">{{ error }}</p>
      <div v-else-if="loading" class="skeleton h-32 rounded-card"></div>
      <ul v-else class="divide-y divide-light-grey overflow-hidden rounded-card border border-light-grey bg-white">
        <li v-for="a in admins" :key="a.id" class="flex items-center gap-3 px-4 py-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-uni-navy to-teal-500 text-xs font-bold text-white">
            {{ initials(`${a.firstName} ${a.lastName}`) }}
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-charcoal">
              {{ a.firstName }} {{ a.lastName }} <span v-if="a.id === auth.user?.id" class="font-normal text-medium-grey">(you)</span>
            </p>
            <p class="truncate text-xs text-medium-grey">{{ a.email }}</p>
          </div>
          <span v-if="a.seller" class="badge bg-amber-50 text-warning" title="Created before admin and student accounts were separated">Also a seller</span>
        </li>
      </ul>
    </div>

    <div class="card h-fit space-y-4">
      <div>
        <h3 class="section-title flex items-center gap-2"><ShieldCheck class="h-5 w-5 text-gold-600" /> Invite an admin</h3>
        <p class="mt-1 text-sm text-medium-grey">They get an email with a link to set their own password.</p>
      </div>
      <div class="flex gap-2.5 rounded-control bg-navy-50 p-3 text-xs leading-relaxed text-navy-800">
        <Info class="h-4 w-4 shrink-0" />
        <p>
          Admin accounts are separate from student accounts, so admins stay neutral - they can't buy, sell or review. Use a staff
          address (like @cput.ac.za) that isn't already a student account.
        </p>
      </div>
      <form class="space-y-3" @submit.prevent="invite">
        <div class="grid grid-cols-2 gap-3">
          <div><label for="inv-first" class="field-label">First name</label><input id="inv-first" v-model="form.firstName" required class="input-field" /></div>
          <div><label for="inv-last" class="field-label">Last name</label><input id="inv-last" v-model="form.lastName" required class="input-field" /></div>
        </div>
        <div>
          <label for="inv-email" class="field-label">Email address</label>
          <input id="inv-email" v-model="form.email" type="email" required placeholder="name@cput.ac.za" class="input-field" />
        </div>
        <p v-if="inviteError" class="text-sm text-danger" role="alert">{{ inviteError }}</p>
        <button type="submit" class="btn-primary w-full" :disabled="inviting"><Send class="h-4 w-4" /> {{ inviting ? "Sending…" : "Send invite" }}</button>
      </form>
    </div>
  </section>
</template>
