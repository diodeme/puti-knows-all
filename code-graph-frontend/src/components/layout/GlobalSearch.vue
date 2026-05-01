<template>
  <div class="relative" ref="containerRef">
    <div class="flex items-center gap-2 rounded-xl px-3 py-2"
         style="background: var(--input-bg); border: 1px solid var(--input-border);"
         :style="focused ? { borderColor: 'var(--accent)', boxShadow: '0 0 0 3px var(--accent-glow)' } : {}">
      <Search :size="16" style="color: var(--text-muted); flex-shrink: 0" />
      <input
        v-model="localQuery"
        @focus="focused = true"
        @blur="handleBlur"
        @keydown.enter="handleSearch"
        @input="handleInput"
        type="text"
        :placeholder="$t('search.placeholder')"
        class="flex-1 bg-transparent border-none outline-none text-sm"
        style="color: var(--text-primary)"
      />
      <button v-if="localQuery" @click="clearQuery" class="p-0.5 rounded"
              style="color: var(--text-muted)">
        <X :size="14" />
      </button>
    </div>

    <!-- 搜索结果下拉 -->
    <Transition name="dropdown">
      <div v-if="showDropdown && searchStore.results.length > 0"
           class="absolute top-full left-0 right-0 mt-2 glass-panel p-2 max-h-80 overflow-y-auto z-50">
        <div
          v-for="(method, idx) in searchStore.results" :key="method.node_id || idx"
          @mousedown.prevent="selectMethod(method)"
          class="p-3 rounded-lg cursor-pointer transition-colors duration-100"
          :style="methodHoverStyle(idx)"
          @mouseenter="hoveredIdx = idx"
          @mouseleave="hoveredIdx = -1"
        >
          <div class="font-medium text-sm" style="color: var(--text-primary)">
            {{ method.name }}
          </div>
          <div class="text-xs mt-1 truncate" style="color: var(--text-secondary)">
            {{ method.full_name }}
          </div>
          <div class="flex gap-2 mt-1.5">
            <span class="text-xs px-1.5 py-0.5 rounded"
                  style="background: var(--accent-glow); color: var(--accent)">
              {{ method.visibility || 'public' }}
            </span>
            <span v-if="method.branch_name" class="text-xs" style="color: var(--text-muted)">
              {{ method.branch_name }}
            </span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 加载中 -->
    <Transition name="dropdown">
      <div v-if="showDropdown && searchStore.loading"
           class="absolute top-full left-0 right-0 mt-2 glass-panel p-4 text-center z-50"
           style="color: var(--text-secondary)">
        {{ $t('search.searching') }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, X } from 'lucide-vue-next'
import { useSearchStore } from '../../stores/search'

const router = useRouter()
const searchStore = useSearchStore()

const localQuery = ref('')
const focused = ref(false)
const hoveredIdx = ref(-1)
const containerRef = ref(null)

const showDropdown = computed(() => focused.value && localQuery.value.length > 0)

// 回车搜索：记录历史
function handleSearch() {
  if (localQuery.value.trim()) {
    searchStore.search(localQuery.value.trim())
  }
}

// 输入时仅搜索，不记录历史
let inputTimer = null
function handleInput() {
  clearTimeout(inputTimer)
  if (localQuery.value.length >= 2) {
    inputTimer = setTimeout(() => {
      searchStore.searchQuiet(localQuery.value.trim())
    }, 300)
  }
}

function selectMethod(method) {
  searchStore.selectMethod(method)
  focused.value = false
  router.push({
    path: '/explore',
    query: { method: method.full_name, id: method.node_id }
  })
}

function clearQuery() {
  localQuery.value = ''
  searchStore.results = []
}

function handleBlur() {
  setTimeout(() => { focused.value = false }, 200)
}

function methodHoverStyle(idx) {
  return hoveredIdx.value === idx
    ? { background: 'var(--bg-glass-hover)' }
    : {}
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
</style>
