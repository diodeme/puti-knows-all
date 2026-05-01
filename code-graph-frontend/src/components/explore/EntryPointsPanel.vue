<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-3">
      <h3 class="text-sm font-bold" style="color: var(--text-primary)">{{ t('entryPoints.title') }}</h3>
      <span v-if="entryPoints.length" class="text-xs px-2 py-0.5 rounded-full"
            style="background: var(--accent-glow); color: var(--accent)">
        {{ entryPoints.length }}
      </span>
    </div>

    <div v-if="loading" class="text-center py-4 text-xs" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
    <div v-else-if="entryPoints.length === 0" class="text-center py-4 text-xs" style="color: var(--text-muted)">
      {{ t('entryPoints.noData') }}
    </div>
    <div v-else class="space-y-1 max-h-60 overflow-y-auto">
      <div
        v-for="ep in entryPoints" :key="ep.id"
        @click="loadEntryPoint(ep)"
        class="flex items-center gap-2 p-2 rounded-lg cursor-pointer transition-colors"
        :style="ep === selected ? { background: 'var(--accent-glow)' } : {}"
        @mouseenter="onEpEnter($event, ep)"
        @mouseleave="onEpLeave($event, ep)"
      >
        <span class="text-[10px] font-mono font-bold px-1.5 py-0.5 rounded min-w-[38px] text-center"
              :style="methodStyle(ep.http_method || 'GET')">
          {{ (ep.http_method || 'GET').substring(0, 4) }}
        </span>
        <span class="text-xs truncate" style="color: var(--text-primary)">{{ ep.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getEntryPoints } from '../../api/graph'
import { useGraphStore } from '../../stores/graph'

const { t } = useI18n()
const graphStore = useGraphStore()

const entryPoints = ref([])
const loading = ref(false)
const selected = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getEntryPoints()
    entryPoints.value = res?.entry_points || []
  } catch (e) {
    console.error('Failed to load entry points:', e)
  } finally {
    loading.value = false
  }
})

function methodStyle(method) {
  const map = {
    GET: { background: 'rgba(52,211,153,0.15)', color: '#34d399' },
    POST: { background: 'var(--accent-glow)', color: 'var(--accent)' },
    PUT: { background: 'rgba(251,191,36,0.15)', color: '#fbbf24' },
    DELETE: { background: 'rgba(248,113,113,0.15)', color: '#f87171' }
  }
  return map[method] || map.GET
}

function onEpEnter(event, ep) {
  if (ep !== selected.value) event.currentTarget.style.background = 'var(--bg-glass-hover)'
}

function onEpLeave(event, ep) {
  if (ep !== selected.value) event.currentTarget.style.background = 'transparent'
}

function loadEntryPoint(ep) {
  selected.value = ep
  const methodData = {
    full_name: ep.full_name,
    fullName: ep.full_name,
    nodeId: ep.id,
    name: ep.name
  }
  graphStore.setCurrentMethod(methodData)
  graphStore.loadGraph(ep.full_name, graphStore.queryType, graphStore.pathDepth)
}
</script>
