<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, extractErrorMessage } from "@/lib/api";
import { useToastStore } from "@/stores/toast";
import type { QuestionView } from "@/lib/types";

const toast = useToastStore();

const questions = ref<QuestionView[]>([]);
const loading = ref(false);
const error = ref("");
const statusFilter = ref<"ALL" | "PENDING" | "ANSWERED">("ALL");
const keyword = ref("");
const editingId = ref<string | null>(null);
const drafts = ref<Record<string, string>>({});
const savingId = ref<string | null>(null);

function initials(name: string): string {
  return name
    .split(" ")
    .map((part) => part.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

function statusCountFor(value: string): number {
  if (value === "ALL") return questions.value.length;
  if (value === "PENDING") return questions.value.filter((q) => !q.answerText).length;
  return questions.value.filter((q) => q.answerText).length;
}

const filteredQuestions = computed(() => {
  let list = questions.value;
  if (statusFilter.value === "PENDING") list = list.filter((q) => !q.answerText);
  if (statusFilter.value === "ANSWERED") list = list.filter((q) => q.answerText);
  const needle = keyword.value.trim().toLowerCase();
  if (needle) {
    list = list.filter(
      (q) =>
        q.questionText.toLowerCase().includes(needle) ||
        q.listingName.toLowerCase().includes(needle) ||
        q.askerName.toLowerCase().includes(needle),
    );
  }
  return list;
});

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const { data } = await api.get<QuestionView[]>("/questions/mine");
    questions.value = data;
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    loading.value = false;
  }
}

function startEdit(q: QuestionView) {
  editingId.value = q.id;
  drafts.value[q.id] = q.answerText ?? "";
}

function cancelEdit(id: string) {
  if (editingId.value === id) editingId.value = null;
}

async function submitAnswer(id: string) {
  const answerText = drafts.value[id];
  if (!answerText?.trim()) return;
  savingId.value = id;
  error.value = "";
  try {
    await api.post(`/questions/${id}/answer`, { answerText });
    toast.success("Answer posted!");
    editingId.value = null;
    await load();
  } catch (err) {
    error.value = extractErrorMessage(err);
  } finally {
    savingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-5">
    <div>
      <h1 class="font-display text-2xl font-bold text-uni-navy">Listing Q&amp;A</h1>
      <p class="text-sm text-medium-grey">Every question asked across your listings, answered or not.</p>
    </div>

    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="s in ['ALL', 'PENDING', 'ANSWERED']"
          :key="s"
          class="rounded-full border px-3.5 py-1.5 text-[13px] font-semibold"
          :class="statusFilter === s ? 'border-campus-teal bg-campus-teal text-white' : 'border-light-grey bg-white text-charcoal hover:border-campus-teal'"
          @click="statusFilter = s as typeof statusFilter"
        >
          {{ s === "ALL" ? "All" : s === "PENDING" ? "Pending" : "Answered" }} &nbsp;{{ statusCountFor(s) }}
        </button>
      </div>
      <input v-model="keyword" type="search" placeholder="Search questions..." class="input-field sm:w-64" />
    </div>

    <p v-if="error" class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="loading" class="text-sm text-medium-grey">Loading...</p>
    <div v-else-if="filteredQuestions.length === 0" class="card text-sm text-medium-grey">
      No questions match this filter.
    </div>

    <div v-else class="space-y-3">
      <div v-for="q in filteredQuestions" :key="q.id" class="card">
        <div class="flex gap-3">
          <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-blue text-xs font-semibold text-white">
            {{ initials(q.askerName) }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center justify-between gap-1">
              <p class="text-xs text-medium-grey">
                <span class="font-medium text-charcoal">{{ q.askerName }}</span> asked on
                <RouterLink :to="`/listings/${q.listingId}`" class="font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600">{{ q.listingName }}</RouterLink>
                &middot; {{ relativeTime(q.createdAt) }}
              </p>
              <span v-if="!q.answerText" class="badge bg-warning/15 text-warning shrink-0">Pending</span>
              <span v-else class="badge bg-success/15 text-success shrink-0">Answered</span>
            </div>
            <p class="mt-1 text-sm font-medium text-charcoal">{{ q.questionText }}</p>

            <div v-if="q.answerText && editingId !== q.id" class="mt-2 flex items-start justify-between gap-2 rounded-control bg-campus-teal/5 p-3">
              <div>
                <p class="text-xs font-semibold text-uni-navy">Your answer &middot; {{ relativeTime(q.answeredAt!) }}</p>
                <p class="mt-0.5 text-sm text-charcoal">{{ q.answerText }}</p>
              </div>
              <button class="shrink-0 text-xs font-medium text-teal-600 underline decoration-teal-600/30 underline-offset-4 hover:decoration-teal-600" @click="startEdit(q)">Edit</button>
            </div>

            <div v-if="!q.answerText || editingId === q.id" class="mt-2 flex gap-2">
              <input
                v-model="drafts[q.id]"
                class="input-field"
                placeholder="Type your answer..."
                @keyup.enter="submitAnswer(q.id)"
                @focus="drafts[q.id] = drafts[q.id] ?? q.answerText ?? ''"
              />
              <button
                class="btn-primary shrink-0 text-sm disabled:opacity-50"
                :disabled="savingId === q.id || !drafts[q.id]?.trim()"
                @click="submitAnswer(q.id)"
              >
                {{ q.answerText ? "Save" : "Reply" }}
              </button>
              <button v-if="q.answerText" class="btn-secondary shrink-0 text-sm" @click="cancelEdit(q.id)">Cancel</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
