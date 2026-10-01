<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { useRouter } from "vue-router";
import {
  BadgeCheck,
  Ban,
  Check,
  Ellipsis,
  Flag,
  Heart,
  Link as LinkIcon,
  MessageCircle,
  Pencil,
  Pin,
  PinOff,
  Send,
  ShieldAlert,
  Trash,
  X,
} from "@lucide/vue";
import { api } from "@/lib/api";
import { formatPrice, initials, relativeTime } from "@/lib/format";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";
import { useClickOutside } from "@/composables/useClickOutside";
import PostComposer from "@/components/posts/PostComposer.vue";
import type { CommentView, ListingDTO, PostView } from "@/lib/types";

const props = withDefaults(
  defineProps<{
    post: PostView;
    /** Show the business name as a link (feeds); on the business's own page it's implied. */
    showBusiness?: boolean;
    /** The business's listings, needed to edit a post's attachment. */
    listings?: ListingDTO[];
  }>(),
  { showBusiness: false, listings: () => [] },
);
const emit = defineEmits<{ updated: [post: PostView]; deleted: [id: string] }>();

const auth = useAuthStore();
const toast = useToastStore();
const router = useRouter();

const COLLAPSE_AT = 360;
const expanded = ref(false);
const isLong = computed(() => (props.post.body?.length ?? 0) > COLLAPSE_AT);
const shownBody = computed(() =>
  isLong.value && !expanded.value ? props.post.body!.slice(0, COLLAPSE_AT).trimEnd() + "…" : props.post.body,
);
const isStudent = computed(() => auth.isAuthenticated && !auth.isAdmin);

// ---- Like (optimistic: the heart responds instantly, then syncs with the server) ----
const liking = ref(false);
async function toggleLike() {
  if (!auth.isAuthenticated) {
    router.push({ name: "login", query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  if (liking.value) return;
  liking.value = true;
  const before = props.post;
  emit("updated", { ...before, likedByMe: !before.likedByMe, likeCount: before.likeCount + (before.likedByMe ? -1 : 1) });
  try {
    const { data } = before.likedByMe
      ? await api.delete<PostView>(`/posts/${before.id}/like`)
      : await api.post<PostView>(`/posts/${before.id}/like`);
    emit("updated", data);
  } catch {
    emit("updated", before);
  } finally {
    liking.value = false;
  }
}

// ---- Comments ----
const commentsOpen = ref(false);
const comments = ref<CommentView[]>([]);
const commentsLoading = ref(false);
const draft = ref("");
const sending = ref(false);
const commentInput = ref<HTMLInputElement | null>(null);

async function openComments() {
  commentsOpen.value = !commentsOpen.value;
  if (!commentsOpen.value) return;
  commentsLoading.value = true;
  try {
    const { data } = await api.get<CommentView[]>(`/posts/${props.post.id}/comments`);
    comments.value = data;
  } finally {
    commentsLoading.value = false;
  }
  if (isStudent.value) {
    await nextTick();
    commentInput.value?.focus();
  }
}

async function sendComment() {
  if (!draft.value.trim() || sending.value) return;
  sending.value = true;
  try {
    const { data } = await api.post<CommentView>(`/posts/${props.post.id}/comments`, { body: draft.value.trim() });
    comments.value.push(data);
    draft.value = "";
    emit("updated", { ...props.post, commentCount: props.post.commentCount + 1 });
  } finally {
    sending.value = false;
  }
}

async function deleteComment(c: CommentView) {
  await api.delete(`/post-comments/${c.id}`);
  comments.value = comments.value.filter((x) => x.id !== c.id);
  emit("updated", { ...props.post, commentCount: Math.max(0, props.post.commentCount - 1) });
}

// ---- Share ----
const copied = ref(false);
async function share() {
  const url = `${window.location.origin}/providers/${props.post.businessId}#post-${props.post.id}`;
  try {
    if (navigator.share) {
      await navigator.share({ title: props.post.businessName, text: props.post.body ?? "", url });
      return;
    }
    await navigator.clipboard.writeText(url);
    copied.value = true;
    setTimeout(() => (copied.value = false), 2000);
  } catch {
    // Share sheet dismissed or clipboard blocked - nothing to do.
  }
}

// ---- Menu: owner / student / admin actions ----
const menuOpen = ref(false);
const menuRoot = ref<HTMLElement | null>(null);
useClickOutside(menuRoot, () => (menuOpen.value = false));
const editing = ref(false);

async function setPinned(pinned: boolean) {
  menuOpen.value = false;
  const { data } = await api.post<PostView>(`/posts/${props.post.id}/pin`, { pinned });
  emit("updated", data);
  toast.success(pinned ? "Pinned to the top of your page" : "Unpinned");
}

const confirmDelete = ref(false);
async function deletePost() {
  await api.delete(`/posts/${props.post.id}`);
  emit("deleted", props.post.id);
  toast.success("Post deleted");
}

const reported = ref(false);
async function report() {
  menuOpen.value = false;
  await api.post(`/posts/${props.post.id}/flag`);
  reported.value = true;
  toast.info("Thanks for reporting", "An admin will review this post.");
}

const removeOpen = ref(false);
const removeReason = ref("");
async function remove() {
  if (!removeReason.value.trim()) return;
  const { data } = await api.post<PostView>(`/admin/posts/${props.post.id}/remove`, { reason: removeReason.value.trim() });
  removeOpen.value = false;
  emit("updated", data);
  toast.success("Post removed", "The business has been notified.");
}

function onEdited(post: PostView) {
  editing.value = false;
  emit("updated", post);
  toast.success("Post updated");
}
</script>

<template>
  <PostComposer
    v-if="editing"
    :business-id="post.businessId"
    :business-name="post.businessName"
    :logo-url="post.businessImageUrl"
    :listings="listings"
    :editing="post"
    @saved="onEdited"
    @cancel="editing = false"
  />

  <article v-else :id="`post-${post.id}`" class="card scroll-mt-24 p-0 transition" :class="{ 'opacity-80': post.removedReason }">
    <div v-if="post.removedReason" class="flex gap-2 rounded-t-card border-b border-red-200 bg-red-50 px-4 py-2.5 text-xs text-red-900">
      <ShieldAlert class="mt-0.5 h-4 w-4 shrink-0 text-danger" />
      <span>Removed by an admin and hidden from everyone else. Reason: {{ post.removedReason }}</span>
    </div>

    <header class="flex items-start gap-3 px-4 pt-4 sm:px-5">
      <RouterLink :to="`/providers/${post.businessId}`" class="shrink-0">
        <img v-if="post.businessImageUrl" :src="post.businessImageUrl" alt="" class="h-10 w-10 rounded-full object-cover ring-1 ring-light-grey" />
        <span v-else class="flex h-10 w-10 items-center justify-center rounded-full bg-navy-100 font-display text-sm font-bold text-navy-700">
          {{ post.businessName.charAt(0) }}
        </span>
      </RouterLink>
      <div class="min-w-0 flex-1">
        <p class="flex items-center gap-1.5 text-sm font-semibold text-charcoal">
          <RouterLink :to="`/providers/${post.businessId}`" class="truncate hover:text-teal-700">{{ post.businessName }}</RouterLink>
          <BadgeCheck v-if="post.businessVerified" class="h-4 w-4 shrink-0 text-emerald-500" aria-label="Verified business" />
        </p>
        <p class="flex flex-wrap items-center gap-x-1.5 text-xs text-medium-grey">
          <time :datetime="post.createdAt" :title="new Date(post.createdAt).toLocaleString('en-ZA')">{{ relativeTime(post.createdAt) }}</time>
          <span v-if="post.edited">· edited</span>
          <span v-if="post.pinned" class="inline-flex items-center gap-0.5 font-semibold text-teal-700">· <Pin class="h-3 w-3" /> Pinned</span>
          <span v-if="post.flagCount > 0" class="inline-flex items-center gap-0.5 font-semibold text-danger">· <Flag class="h-3 w-3" /> {{ post.flagCount }} report{{ post.flagCount === 1 ? "" : "s" }}</span>
        </p>
      </div>

      <div v-if="post.canManage || isStudent || auth.isAdmin" ref="menuRoot" class="relative -mr-2 -mt-1">
        <button class="btn-icon h-9 w-9" aria-label="Post options" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">
          <Ellipsis class="h-5 w-5" />
        </button>
        <div v-if="menuOpen" class="absolute right-0 top-[calc(100%+4px)] z-20 w-52 animate-scale-in overflow-hidden rounded-card border border-light-grey bg-white p-1.5 shadow-pop" role="menu">
          <template v-if="post.canManage && !post.removedReason">
            <button class="menu-item" role="menuitem" @click="setPinned(!post.pinned)">
              <component :is="post.pinned ? PinOff : Pin" class="h-4 w-4" /> {{ post.pinned ? "Unpin from page" : "Pin to top of page" }}
            </button>
            <button class="menu-item" role="menuitem" @click="editing = true; menuOpen = false"><Pencil class="h-4 w-4" /> Edit post</button>
          </template>
          <button v-if="post.canManage" class="menu-item !text-danger" role="menuitem" @click="confirmDelete = true; menuOpen = false">
            <Trash class="h-4 w-4" /> Delete post
          </button>
          <button v-if="isStudent && !post.canManage" class="menu-item" role="menuitem" :disabled="reported" @click="report">
            <Flag class="h-4 w-4" /> {{ reported ? "Reported" : "Report post" }}
          </button>
          <button v-if="auth.isAdmin && !post.removedReason" class="menu-item !text-danger" role="menuitem" @click="removeOpen = true; menuOpen = false">
            <Ban class="h-4 w-4" /> Remove post
          </button>
          <button class="menu-item" role="menuitem" @click="share(); menuOpen = false"><LinkIcon class="h-4 w-4" /> Copy link</button>
        </div>
      </div>
    </header>

    <div v-if="post.body" class="px-4 pt-3 sm:px-5">
      <p class="whitespace-pre-line break-words text-[15px] leading-relaxed text-charcoal">{{ shownBody }}</p>
      <button v-if="isLong" class="mt-1 text-sm font-semibold text-medium-grey hover:text-uni-navy" @click="expanded = !expanded">
        {{ expanded ? "See less" : "See more" }}
      </button>
    </div>

    <img v-if="post.imageUrl" :src="post.imageUrl" alt="" loading="lazy" class="mt-3 max-h-[32rem] w-full bg-soft-grey object-cover" />

    <RouterLink
      v-if="post.listing"
      :to="`/listings/${post.listing.id}`"
      class="group mx-4 mt-3 flex items-center gap-3 rounded-control border border-light-grey bg-soft-grey/60 p-2.5 transition hover:border-teal-300 hover:bg-teal-50/50 sm:mx-5"
    >
      <img v-if="post.listing.imageUrl" :src="post.listing.imageUrl" alt="" class="h-12 w-12 rounded-lg object-cover" />
      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-semibold text-charcoal group-hover:text-teal-700">{{ post.listing.name }}</p>
        <p class="text-xs text-medium-grey">
          {{ formatPrice(post.listing.price) }} · {{ post.listing.status === "SOLD_OUT" ? "Sold out" : post.listing.category }}
        </p>
      </div>
      <span class="btn-secondary shrink-0 px-3 py-1.5 text-xs">View</span>
    </RouterLink>

    <div v-if="post.likeCount > 0 || post.commentCount > 0" class="flex items-center justify-between px-4 pt-3 text-xs text-medium-grey sm:px-5">
      <span v-if="post.likeCount > 0" class="inline-flex items-center gap-1">
        <span class="flex h-4 w-4 items-center justify-center rounded-full bg-danger"><Heart class="h-2.5 w-2.5 fill-white text-white" /></span>
        {{ post.likeCount }}
      </span>
      <span v-else></span>
      <button v-if="post.commentCount > 0" class="hover:underline" @click="openComments">
        {{ post.commentCount }} comment{{ post.commentCount === 1 ? "" : "s" }}
      </button>
    </div>

    <div v-if="!post.removedReason" class="mx-4 mt-2 grid grid-cols-3 border-t border-light-grey py-1 sm:mx-5">
      <button
        v-if="!auth.isAdmin"
        class="post-action"
        :class="{ 'text-danger': post.likedByMe }"
        :aria-pressed="post.likedByMe"
        @click="toggleLike"
      >
        <Heart class="h-[18px] w-[18px] transition" :class="post.likedByMe ? 'scale-110 fill-danger' : ''" /> Like
      </button>
      <span v-else></span>
      <button class="post-action" :aria-expanded="commentsOpen" @click="openComments"><MessageCircle class="h-[18px] w-[18px]" /> Comment</button>
      <button class="post-action" @click="share">
        <component :is="copied ? Check : LinkIcon" class="h-[18px] w-[18px]" /> {{ copied ? "Copied" : "Share" }}
      </button>
    </div>

    <div v-if="commentsOpen" class="space-y-3 border-t border-light-grey bg-soft-grey/40 px-4 py-3 sm:px-5">
      <div v-if="commentsLoading" class="skeleton h-10 rounded-control"></div>
      <p v-else-if="comments.length === 0" class="text-xs text-medium-grey">No comments yet{{ isStudent ? " - be the first." : "." }}</p>
      <div v-for="c in comments" :key="c.id" class="group flex gap-2.5">
        <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white text-[11px] font-bold text-navy-700 ring-1 ring-light-grey">
          {{ initials(c.authorName) }}
        </span>
        <div class="min-w-0 flex-1">
          <div class="inline-block max-w-full rounded-2xl bg-white px-3.5 py-2 ring-1 ring-light-grey">
            <p class="text-xs font-semibold text-charcoal">{{ c.authorName }}</p>
            <p class="whitespace-pre-line break-words text-sm text-charcoal">{{ c.body }}</p>
          </div>
          <p class="mt-0.5 flex items-center gap-2 px-2 text-[11px] text-medium-grey">
            {{ relativeTime(c.createdAt) }}
            <button v-if="c.canDelete" class="font-semibold hover:text-danger" @click="deleteComment(c)">Delete</button>
          </p>
        </div>
      </div>
      <form v-if="isStudent" class="flex items-center gap-2" @submit.prevent="sendComment">
        <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-uni-navy to-teal-500 text-[11px] font-bold text-white">
          {{ initials(`${auth.user?.firstName ?? ""} ${auth.user?.lastName ?? ""}`) }}
        </span>
        <input ref="commentInput" v-model="draft" maxlength="1000" class="input-field rounded-full py-2" placeholder="Write a comment…" aria-label="Write a comment" />
        <button type="submit" class="btn-icon h-9 w-9 text-teal-600" :disabled="!draft.trim() || sending" aria-label="Post comment">
          <Send class="h-4 w-4" />
        </button>
      </form>
      <p v-else-if="!auth.isAuthenticated" class="text-xs text-medium-grey">
        <RouterLink :to="{ name: 'login', query: { redirect: $route.fullPath } }" class="link">Log in</RouterLink> to comment.
      </p>
    </div>
  </article>

  <Teleport to="body">
    <div v-if="confirmDelete" class="modal-backdrop" @click.self="confirmDelete = false">
      <div v-dialog="() => (confirmDelete = false)" class="modal-panel" role="dialog" aria-modal="true" aria-label="Delete this post?">
        <h2 class="font-display text-lg font-bold text-uni-navy">Delete this post?</h2>
        <p class="mt-1 text-sm text-medium-grey">Its likes and comments are deleted too. This can't be undone.</p>
        <div class="mt-6 flex gap-2">
          <button class="btn-secondary flex-1" @click="confirmDelete = false">Keep it</button>
          <button class="btn-danger flex-1" @click="deletePost">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="removeOpen" class="modal-backdrop" @click.self="removeOpen = false">
      <div v-dialog="() => (removeOpen = false)" class="modal-panel" role="dialog" aria-modal="true" aria-label="Remove this post?">
        <div class="flex items-center justify-between">
          <h2 class="font-display text-lg font-bold text-uni-navy">Remove this post?</h2>
          <button class="btn-icon h-8 w-8" aria-label="Close" @click="removeOpen = false"><X class="h-4 w-4" /></button>
        </div>
        <p class="mb-4 mt-1 text-sm text-medium-grey">It's hidden from everyone straight away, and {{ post.businessName }} is told your reason.</p>
        <label for="post-remove-reason" class="field-label">Reason shown to the business</label>
        <textarea id="post-remove-reason" v-model="removeReason" rows="3" class="input-field" placeholder="e.g. Advertising alcohol"></textarea>
        <div class="mt-6 flex gap-2">
          <button class="btn-secondary flex-1" @click="removeOpen = false">Cancel</button>
          <button class="btn-danger flex-1" :disabled="!removeReason.trim()" @click="remove">Remove</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.menu-item {
  @apply flex w-full items-center gap-2.5 rounded-control px-3 py-2 text-left text-sm font-medium text-charcoal transition hover:bg-navy-50 disabled:opacity-50;
}
.post-action {
  @apply flex items-center justify-center gap-2 rounded-control py-2 text-sm font-semibold text-medium-grey transition hover:bg-soft-grey hover:text-uni-navy;
}
</style>
