<script setup lang="ts">
import { ref } from "vue";
import { Eye, EyeOff } from "@lucide/vue";

const model = defineModel<string>({ required: true });
defineProps<{ id?: string; placeholder?: string; autocomplete?: string; minlength?: number }>();
const visible = ref(false);
</script>

<template>
  <div class="relative">
    <input
      :id="id"
      v-model="model"
      :type="visible ? 'text' : 'password'"
      required
      :minlength="minlength"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      class="input-field pr-11"
    />
    <button
      type="button"
      class="absolute right-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 transition hover:bg-soft-grey hover:text-uni-navy"
      :aria-label="visible ? 'Hide password' : 'Show password'"
      @click="visible = !visible"
    >
      <component :is="visible ? EyeOff : Eye" class="h-4 w-4" />
    </button>
  </div>
</template>
