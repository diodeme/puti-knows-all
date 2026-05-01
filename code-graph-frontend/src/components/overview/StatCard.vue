<template>
  <div class="glass-panel-inner p-4">
    <div class="text-xs mb-1" style="color: var(--text-muted)">{{ stat.label }}</div>
    <div class="text-2xl font-bold" style="color: var(--text-primary)">{{ stat.value }}</div>
  </div>
</template>

<script setup>
defineProps({
  stat: { type: Object, required: true }
})
</script>
