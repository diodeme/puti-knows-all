<template>
  <div class="p-6 max-w-6xl mx-auto">
    <section class="glass-panel p-6 mb-6">
      <h2 class="text-lg font-bold mb-4" style="color: var(--text-primary)">{{ t('overview.title') }}</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard v-for="stat in stats" :key="stat.label" :stat="stat" />
      </div>
    </section>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <section class="glass-panel p-5">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-base font-bold" style="color: var(--text-primary)">{{ t('overview.entryPoints') }}</h2>
          <span v-if="entryPoints.length" class="text-xs px-2 py-1 rounded-full"
                style="background: var(--accent-glow); color: var(--accent)">
            {{ entryPoints.length }}
          </span>
        </div>
        <div v-if="loadingData" class="text-center py-8" style="color: var(--text-muted)">
          {{ t('common.loading') }}
        </div>
        <div v-else-if="entryPoints.length === 0" class="text-center py-8" style="color: var(--text-muted)">
          {{ t('overview.noEntryPoints') }}
        </div>
        <div v-else class="space-y-1 max-h-80 overflow-y-auto pr-1">
          <div
            v-for="ep in displayedEntryPoints" :key="ep.id"
            @click="goToExplore(ep)"
            class="flex items-center gap-3 p-2.5 rounded-lg cursor-pointer transition-colors duration-100"
            style="color: var(--text-secondary)"
            @mouseenter="$event.currentTarget.style.background = 'var(--bg-glass-hover)'"
            @mouseleave="$event.currentTarget.style.background = 'transparent'"
          >
            <span class="text-xs font-mono font-bold px-1.5 py-0.5 rounded min-w-[42px] text-center"
                  :style="httpMethodStyle(ep.http_method || 'GET')">
              {{ ep.http_method || 'GET' }}
            </span>
            <span class="text-sm truncate" style="color: var(--text-primary)">{{ ep.name }}</span>
            <span class="text-xs truncate ml-auto" style="color: var(--text-muted)">{{ ep.source_code_location }}</span>
          </div>
          <button v-if="entryPoints.length > 10 && !showAllEntryPoints"
                  @click="showAllEntryPoints = true"
                  class="w-full text-center text-xs py-2 rounded-lg transition-colors"
                  style="color: var(--accent)"
                  @mouseenter="$event.target.style.background = 'var(--bg-glass-hover)'"
                  @mouseleave="$event.target.style.background = 'transparent'">
            {{ t('overview.viewAll', { count: entryPoints.length }) }}
          </button>
        </div>
      </section>

      <section class="glass-panel p-5">
        <h2 class="text-base font-bold mb-4" style="color: var(--text-primary)">{{ t('overview.typeDistribution') }}</h2>
        <div class="space-y-3">
          <div v-for="(item, idx) in typeDistribution" :key="item.type">
            <div class="flex items-center justify-between text-sm mb-1">
              <span style="color: var(--text-secondary)">{{ nodeTypeLabel(item.type) }}</span>
              <span style="color: var(--text-muted)">{{ item.count.toLocaleString() }}</span>
            </div>
            <div class="h-2 rounded-full overflow-hidden" style="background: var(--bg-glass-inner)">
              <div class="h-full rounded-full transition-all duration-500"
                   :style="{ width: item.percentage + '%', background: typeColor(item.type) }" />
            </div>
          </div>
          <div v-if="typeDistribution.length === 0" class="text-center py-8" style="color: var(--text-muted)">
            {{ t('common.noData') }}
          </div>
        </div>
      </section>
    </div>

    <section v-if="searchStore.history.length > 0" class="glass-panel p-5">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base font-bold" style="color: var(--text-primary)">{{ t('overview.recentSearches') }}</h2>
        <button @click="searchStore.clearHistory()" class="text-xs" style="color: var(--text-muted)">
          {{ t('common.clear') }}
        </button>
      </div>
      <div class="space-y-1">
        <div
          v-for="item in searchStore.history.slice(0, 5)" :key="item.query"
          @click="goToExploreWithSearch(item.query)"
          class="flex items-center justify-between p-2.5 rounded-lg cursor-pointer transition-colors"
          @mouseenter="$event.currentTarget.style.background = 'var(--bg-glass-hover)'"
          @mouseleave="$event.currentTarget.style.background = 'transparent'"
        >
          <span class="text-sm" style="color: var(--text-primary)">{{ item.query }}</span>
          <span class="text-xs" style="color: var(--text-muted)">{{ timeAgo(item.timestamp) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getEntryPoints, getGlobalStats } from '../api/graph'
import { useSearchStore } from '../stores/search'
import StatCard from '../components/overview/StatCard.vue'

const { t } = useI18n()
const router = useRouter()
const searchStore = useSearchStore()

const entryPoints = ref([])
const loadingData = ref(false)
const showAllEntryPoints = ref(false)

const globalStats = ref({
  total_nodes: 0,
  total_edges: 0,
  node_type_stats: {},
  edge_type_stats: {},
  entry_point_count: 0,
  repo_count: 0
})

const displayedEntryPoints = computed(() =>
  showAllEntryPoints.value ? entryPoints.value : entryPoints.value.slice(0, 10)
)

const stats = computed(() => [
  { label: t('overview.stats.nodes'), value: globalStats.value.total_nodes ? globalStats.value.total_nodes.toLocaleString() : '-' },
  { label: t('overview.stats.edges'), value: globalStats.value.total_edges ? globalStats.value.total_edges.toLocaleString() : '-' },
  { label: t('overview.stats.entryPoints'), value: globalStats.value.entry_point_count || entryPoints.value.length || '-' },
  { label: t('overview.stats.repos'), value: globalStats.value.repo_count || '-' }
])

function normalizeNodeType(raw) {
  if (raw === 'annotations') return 'annotation'
  if (raw === 'marker_annotations') return 'marker_annotation'
  return raw
}

const typeDistribution = computed(() => {
  const entries = Object.entries(globalStats.value.node_type_stats)
  if (entries.length === 0) return []
  const total = entries.reduce((sum, [, count]) => sum + count, 0)
  return entries
    .map(([type, count]) => ({ type: normalizeNodeType(type), count, percentage: Math.round(count / total * 100) }))
    .sort((a, b) => b.count - a.count)
})

onMounted(async () => {
  loadingData.value = true
  try {
    const [epRes, statsRes] = await Promise.all([
      getEntryPoints().catch(() => null),
      getGlobalStats().catch(() => null)
    ])
    entryPoints.value = epRes?.entry_points || []
    if (statsRes) {
      globalStats.value = statsRes
    }
  } catch (e) {
    console.error('Failed to load data:', e)
  } finally {
    loadingData.value = false
  }
})

function httpMethodStyle(method) {
  const map = {
    GET: { background: 'rgba(52,211,153,0.15)', color: '#34d399' },
    POST: { background: 'var(--accent-glow)', color: 'var(--accent)' },
    PUT: { background: 'rgba(251,191,36,0.15)', color: '#fbbf24' },
    DELETE: { background: 'rgba(248,113,113,0.15)', color: '#f87171' }
  }
  return map[method] || map.GET
}

function nodeTypeLabel(type) {
  return t('nodeType.' + type, type)
}

function typeColor(type) {
  const map = {
    function: '#3b82f6', class: '#f97316', field: '#06b6d4',
    annotation: '#ec4899', marker_annotation: '#ec4899',
    file: '#64748b', comment: '#94a3b8'
  }
  if (type === 'annotations') return map.annotation
  if (type === 'marker_annotations') return map.marker_annotation
  return map[type] || '#94a3b8'
}

function timeAgo(ts) {
  const diff = Date.now() - ts
  if (diff < 60000) return t('overview.timeAgo.justNow')
  if (diff < 3600000) return t('overview.timeAgo.minutesAgo', { n: Math.floor(diff / 60000) })
  if (diff < 86400000) return t('overview.timeAgo.hoursAgo', { n: Math.floor(diff / 3600000) })
  return t('overview.timeAgo.daysAgo', { n: Math.floor(diff / 86400000) })
}

function goToExplore(ep) {
  router.push({ path: '/explore', query: { method: ep.full_name, id: ep.id } })
}

function goToExploreWithSearch(query) {
  router.push({ path: '/explore', query: { search: query } })
}
</script>
