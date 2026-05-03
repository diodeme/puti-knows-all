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

    <!-- Project Selector -->
    <div class="project-select-wrap" ref="projectDropdownRef">
      <button
        @click="toggleProjectDropdown"
        class="project-trigger"
        :class="{ 'project-trigger-active': projectOpen }"
      >
        <FolderKanban :size="14" style="flex-shrink: 0" />
        <span class="project-trigger-text">{{ projectLabel }}</span>
        <ChevronDown :size="14" class="project-chevron" :class="{ 'project-chevron-open': projectOpen }" />
      </button>
      <Transition name="dropdown">
        <div v-if="projectOpen" class="project-dropdown glass-panel">
          <div v-if="projectStore.loading" class="project-option" style="color: var(--text-muted)">
            {{ t('common.loading') }}
          </div>
          <div v-else-if="projectStore.projects.length === 0" class="project-option" style="color: var(--text-muted)">
            {{ t('project.noProjects') }}
          </div>
          <template v-else>
            <div
              v-for="proj in projectStore.projects" :key="proj.projectId"
              @click="handleSelectProject(proj.projectId)"
              class="project-option"
              :class="{ 'project-option-selected': projectStore.selectedProjectId === proj.projectId }"
            >
              <div class="project-option-name">{{ proj.projectId }}</div>
              <div class="project-option-branch">{{ proj.branchName }}</div>
            </div>
          </template>
        </div>
      </Transition>
    </div>

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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Sun, Moon, FolderKanban, ChevronDown } from 'lucide-vue-next'
import { useProjectStore } from '../../stores/project'

const { t, locale } = useI18n()
const route = useRoute()
const isDark = ref(true)
const projectStore = useProjectStore()
const projectOpen = ref(false)
const projectDropdownRef = ref(null)

const navItems = computed(() => [
  { path: '/', label: t('nav.overview') },
  { path: '/explore', label: t('nav.explore') },
  { path: '/docs', label: t('nav.docs') }
])

const projectLabel = computed(() => {
  if (projectStore.selectedProjectId) {
    const branch = projectStore.selectedBranch ? ` (${projectStore.selectedBranch})` : ''
    return projectStore.selectedProjectId + branch
  }
  return t('project.selectPlaceholder')
})

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

function toggleProjectDropdown() {
  projectOpen.value = !projectOpen.value
}

function handleSelectProject(projectId) {
  projectStore.selectProject(projectId)
  projectOpen.value = false
}

function handleProjectClickOutside(e) {
  if (projectDropdownRef.value && !projectDropdownRef.value.contains(e.target)) {
    projectOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleProjectClickOutside)
  projectStore.fetchProjects()
})
onUnmounted(() => document.removeEventListener('click', handleProjectClickOutside))

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

<style scoped>
.project-select-wrap {
  position: relative;
  margin-left: 8px;
}

.project-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  padding: 6px 12px;
  font-size: 13px;
  font-family: inherit;
  color: var(--text-primary);
  cursor: pointer;
  outline: none;
  max-width: 220px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.project-trigger:hover {
  border-color: var(--border-glass-hover);
}

.project-trigger-active {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.project-trigger-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  text-align: left;
}

.project-chevron {
  color: var(--text-muted);
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.project-chevron-open {
  transform: rotate(180deg);
}

.project-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  min-width: 240px;
  max-width: 320px;
  z-index: 60;
  padding: 4px;
  border-radius: 12px;
}

.project-option {
  padding: 8px 12px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: background 0.1s ease, color 0.1s ease;
}

.project-option:hover {
  background: var(--bg-glass-hover);
  color: var(--text-primary);
}

.project-option-selected {
  color: var(--accent);
  background: var(--accent-glow);
}

.project-option-name {
  font-weight: 500;
}

.project-option-branch {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.project-option-selected .project-option-branch {
  color: var(--accent);
  opacity: 0.7;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
