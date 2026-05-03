<template>
  <div class="p-4">
    <div class="relative mb-4" ref="searchContainerRef">
      <div class="flex items-center gap-2 rounded-xl px-3 py-2"
           style="background: var(--input-bg); border: 1px solid var(--input-border);"
           :style="searchFocused ? { borderColor: 'var(--accent)', boxShadow: '0 0 0 3px var(--accent-glow)' } : {}">
        <Search :size="14" style="color: var(--text-muted); flex-shrink: 0" />
        <input
          v-model="searchQuery"
          @focus="searchFocused = true"
          @blur="handleSearchBlur"
          @keydown.enter="handleSearch"
          @input="handleSearchInput"
          type="text"
          :placeholder="t('search.placeholder')"
          class="flex-1 bg-transparent border-none outline-none text-sm"
          style="color: var(--text-primary)"
        />
        <button v-if="searchQuery" @click="clearSearch" class="p-0.5 rounded"
                style="color: var(--text-muted)">
          <X :size="12" />
        </button>
      </div>

      <Transition name="dropdown">
        <div v-if="showSearchDropdown && (searchStore.results.length > 0 || searchStore.loading)"
             class="absolute top-full left-0 right-0 mt-2 glass-panel p-2 max-h-64 overflow-y-auto z-50">
          <div v-if="searchStore.loading" class="p-2.5 text-center text-xs"
               style="color: var(--text-secondary)">
            {{ t('search.searching') }}
          </div>
          <div
            v-for="(method, idx) in searchStore.results" :key="method.node_id || idx"
            @mousedown.prevent="selectMethod(method)"
            class="p-2.5 rounded-lg cursor-pointer transition-colors duration-100"
            :style="searchResultStyle(idx)"
            @mouseenter="hoveredSearchIdx = idx"
            @mouseleave="hoveredSearchIdx = -1"
          >
            <div class="text-sm font-medium truncate" style="color: var(--text-primary)">{{ method.name }}</div>
            <div class="text-xs truncate mt-0.5" style="color: var(--text-secondary)">{{ method.full_name }}</div>
          </div>
        </div>
      </Transition>
    </div>

    <div v-if="graphStore.currentMethod" class="glass-panel-inner p-3 mb-4">
      <div class="text-xs mb-1" style="color: var(--text-muted)">{{ t('search.currentMethod') }}</div>
      <div class="text-sm font-medium truncate" style="color: var(--text-primary)">
        {{ graphStore.currentMethod.name }}
      </div>
      <div class="text-xs truncate mt-1" style="color: var(--text-secondary)">
        {{ graphStore.currentMethod.full_name }}
      </div>
    </div>

    <div class="mb-3">
      <div class="text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('search.direction') }}</div>
      <div class="flex gap-2">
        <button
          v-for="qt in queryTypes" :key="qt.value"
          @click="changeQueryType(qt.value)"
          class="flex-1 py-1.5 text-xs rounded-lg border transition-colors"
          :style="queryTypeStyle(qt.value)"
        >
          {{ qt.label }}
        </button>
      </div>
    </div>

    <div class="mb-3">
      <div class="text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('search.traversalMode') }}</div>
      <div class="flex gap-2">
        <button
          v-for="mode in traversalModes" :key="mode.value"
          @click="changeTraversalMode(mode.value)"
          class="flex-1 py-1.5 text-xs rounded-lg border transition-colors"
          :style="traversalModeStyle(mode.value)"
        >
          {{ mode.label }}
        </button>
      </div>
    </div>

    <div class="mb-3">
      <div class="text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('search.pathDepth') }}</div>
      <div class="depth-select-wrap" ref="depthDropdownRef">
        <button
          @click="depthOpen = !depthOpen"
          class="depth-trigger"
          :class="{ 'depth-trigger-active': depthOpen }"
        >
          <span>{{ depthLabel }}</span>
          <ChevronDown :size="14" class="depth-chevron" :class="{ 'depth-chevron-open': depthOpen }" />
        </button>
        <Transition name="dropdown">
          <div v-if="depthOpen" class="depth-dropdown glass-panel">
            <div
              v-for="d in depthOptions" :key="d.value"
              @click="selectDepth(d.value)"
              class="depth-option"
              :class="{ 'depth-option-selected': graphStore.pathDepth === d.value }"
            >
              {{ d.label }}
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, X, ChevronDown } from 'lucide-vue-next'
import { useGraphStore } from '../../stores/graph'
import { useSearchStore } from '../../stores/search'

const { t } = useI18n()
const graphStore = useGraphStore()
const searchStore = useSearchStore()

const searchQuery = ref('')
const searchFocused = ref(false)
const hoveredSearchIdx = ref(-1)
const searchContainerRef = ref(null)
const depthOpen = ref(false)
const depthDropdownRef = ref(null)

const depthOptions = computed(() => [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
  { value: 5, label: '5' },
  { value: 10, label: '10' },
  { value: -1, label: t('search.all') }
])

const depthLabel = computed(() => {
  const cur = graphStore.pathDepth
  const opt = depthOptions.value.find(d => d.value === cur)
  return opt ? opt.label : String(cur)
})

function selectDepth(value) {
  depthOpen.value = false
  changePathDepth(value)
}

function handleClickOutside(e) {
  if (depthDropdownRef.value && !depthDropdownRef.value.contains(e.target)) {
    depthOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

const showSearchDropdown = computed(() => searchFocused.value && searchQuery.value.length > 0)

const queryTypes = computed(() => [
  { value: 'upstream', label: t('search.upstream') },
  { value: 'downstream', label: t('search.downstream') },
  { value: 'both', label: t('search.both') }
])

const traversalModes = computed(() => [
  { value: 'callChain', label: t('search.callChain') },
  { value: 'fullGraph', label: t('search.fullGraph') }
])

const currentTraversalMode = computed(() => graphStore.traversalEdgeTypes === null ? 'callChain' : 'fullGraph')

function queryTypeStyle(value) {
  const active = graphStore.queryType === value
  return {
    borderColor: active ? 'var(--accent)' : 'var(--border-glass)',
    color: active ? 'var(--accent)' : 'var(--text-secondary)',
    background: active ? 'var(--accent-glow)' : 'transparent'
  }
}

function handleSearch() {
  if (searchQuery.value.trim()) {
    searchStore.search(searchQuery.value.trim())
  }
}

let inputTimer = null
function handleSearchInput() {
  clearTimeout(inputTimer)
  if (searchQuery.value.length >= 2) {
    inputTimer = setTimeout(() => {
      searchStore.searchQuiet(searchQuery.value.trim())
    }, 300)
  }
}

function handleSearchBlur() {
  setTimeout(() => { searchFocused.value = false }, 200)
}

function clearSearch() {
  searchQuery.value = ''
  searchStore.results = []
}

function searchResultStyle(idx) {
  return hoveredSearchIdx.value === idx
    ? { background: 'var(--bg-glass-hover)' }
    : {}
}

function selectMethod(method) {
  const methodData = {
    ...method,
    full_name: method.full_name,
    fullName: method.full_name,
    nodeId: method.node_id,
    name: method.name
  }
  graphStore.setCurrentMethod(methodData)
  searchStore.selectMethod(method)
  searchFocused.value = false
  graphStore.loadGraph(method.full_name, graphStore.queryType, graphStore.pathDepth)
}

function changeQueryType(type) {
  graphStore.setQueryType(type)
  reloadGraph()
}

function changeTraversalMode(mode) {
  graphStore.setTraversalEdgeTypes(mode === 'callChain' ? null : [])
  reloadGraph()
}

function traversalModeStyle(value) {
  const active = currentTraversalMode.value === value
  return {
    borderColor: active ? 'var(--accent)' : 'var(--border-glass)',
    color: active ? 'var(--accent)' : 'var(--text-secondary)',
    background: active ? 'var(--accent-glow)' : 'transparent'
  }
}

function changePathDepth(depth) {
  graphStore.setPathDepth(depth)
  reloadGraph()
}

function reloadGraph() {
  if (graphStore.currentMethod?.full_name) {
    graphStore.loadGraph(graphStore.currentMethod.full_name, graphStore.queryType, graphStore.pathDepth)
  }
}
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.depth-select-wrap {
  position: relative;
  width: 100%;
}

.depth-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 13px;
  font-family: inherit;
  color: var(--text-primary);
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.depth-trigger:hover {
  border-color: var(--border-glass-hover);
}

.depth-trigger-active {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.depth-chevron {
  color: var(--text-muted);
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.depth-chevron-open {
  transform: rotate(180deg);
}

.depth-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 50;
  padding: 4px;
  border-radius: 12px;
  min-width: 100%;
}

.depth-option {
  padding: 7px 12px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: background 0.1s ease, color 0.1s ease;
}

.depth-option:hover {
  background: var(--bg-glass-hover);
  color: var(--text-primary);
}

.depth-option-selected {
  color: var(--accent);
  background: var(--accent-glow);
  font-weight: 500;
}
</style>
