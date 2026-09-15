<script setup lang="ts">
withDefaults(defineProps<{ size?: number }>(), { size: 40 });
</script>

<template>
  <svg :width="size" :height="(size * 190) / 220" viewBox="0 0 220 190" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <text x="8" y="140" font-family="'Playfair Display', Georgia, serif" font-size="140" font-weight="700" fill="#163D72">U</text>
    <text x="100" y="140" font-family="'Playfair Display', Georgia, serif" font-size="140" font-weight="700" fill="#2A9BB4">H</text>
    <path d="M14 95 C 60 145, 150 145, 196 95" stroke="#2A9BB4" stroke-width="4" fill="none" stroke-linecap="round" />
    <g transform="translate(76, 106)">
      <path d="M31 0 L54 10 L31 20 L8 10 Z" fill="#B89A5E" />
      <path d="M31 20 v9" stroke="#B89A5E" stroke-width="2.5" stroke-linecap="round" />
      <circle cx="31" cy="31" r="2.5" fill="#B89A5E" />
      <path d="M14 22 h4 l4 20 h20 l5 -14 h-25" stroke="#B89A5E" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round" />
      <circle cx="24" cy="48" r="2.8" fill="#B89A5E" />
      <circle cx="38" cy="48" r="2.8" fill="#B89A5E" />
    </g>
  </svg>
</template>
