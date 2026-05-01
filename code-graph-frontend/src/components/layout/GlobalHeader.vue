<template>
  <header class="fixed top-0 left-0 right-0 z-50 h-14 flex items-center px-5 gap-4"
          :style="headerStyle">
    <RouterLink to="/" class="flex items-center gap-2 no-underline shrink-0">
      <span class="text-lg font-bold tracking-tight" style="color: var(--accent)">Puti KG</span>
    </RouterLink>

    <nav class="flex gap-1 shrink-0">
      <RouterLink
        v-for="item in navItems" :key="item.path"
        :to="item.path"
        class="px-3 py-1.5 rounded-lg text-sm font-medium no-underline transition-all duration-150"
        :style="navItemStyle(item.path)"
      >
        {{ item.label }}
      </RouterLink>
    </nav>

    <div class="flex-1" />

    <div class="flex items-center gap-2 shrink-0">
      <button
        @click="toggleLocale"
        class="px-2 py-1 rounded-lg text-xs font-medium transition-colors duration-150"
        style="color: var(--text-secondary); border: 1px solid var(--border-glass)"
        @mouseenter="$event.target.style.background = 'var(--bg-glass-hover)'"
        @mouseleave="$event.target.style.background = 'transparent'"
      >
        {{ locale === 'zh' ? 'EN' : '中' }}
      </button>
      <button
        @click="toggleTheme"
        class="p-2 rounded-lg transition-colors duration-150"
        style="color: var(--text-secondary)"
        @mouseenter="$event.target.style.background = 'var(--bg-glass-hover)'"
        @mouseleave="$event.target.style.background = 'transparent'"
        :title="isDark ? t('theme.light') : t('theme.dark')"
      >
        <Sun v-if="isDark" :size="18" />
        <Moon v-else :size="18" />
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Sun, Moon } from 'lucide-vue-next'

const { t, locale } = useI18n()
const route = useRoute()
const isDark = ref(true)

const navItems = computed(() => [
  { path: '/', label: t('nav.overview') },
  { path: '/explore', label: t('nav.explore') },
  { path: '/docs', label: t('nav.docs') }
])

const headerStyle = computed(() => ({
  background: 'var(--header-bg)',
  backdropFilter: 'var(--backdrop-blur)',
  WebkitBackdropFilter: 'var(--backdrop-blur)',
  borderBottom: '1px solid var(--header-border)'
}))

function navItemStyle(path) {
  const active = route.path === path || (path === '/explore' && route.path.startsWith('/explore'))
  return {
    color: active ? 'var(--accent)' : 'var(--text-secondary)',
    background: active ? 'var(--accent-glow)' : 'transparent'
  }
}

function toggleTheme() {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.removeAttribute('data-theme')
  } else {
    document.documentElement.setAttribute('data-theme', 'light')
  }
}

function toggleLocale() {
  locale.value = locale.value === 'zh' ? 'en' : 'zh'
  localStorage.setItem('locale', locale.value)
}
</script>
