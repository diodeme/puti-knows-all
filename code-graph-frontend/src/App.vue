<template>
  <div class="app-container">
    <GlobalHeader />
    <main class="pt-14">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import GlobalHeader from './components/layout/GlobalHeader.vue'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: var(--bg-base-gradient);
}
</style>
