<template>
  <div class="p-4">
    <h3 class="text-sm font-bold mb-3" style="color: var(--text-primary)">{{ t('filter.title') }}</h3>

    <!-- Node types -->
    <div class="mb-3">
      <div class="text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('filter.nodeTypes') }}</div>
      <div class="flex flex-wrap gap-2">
        <label
          v-for="type in allNodeTypes" :key="type"
          class="flex items-center gap-1.5 text-xs"
          :class="isNodeTypeAvailable(type) ? 'cursor-pointer' : 'opacity-40 cursor-not-allowed'"
          :style="{ color: isNodeTypeAvailable(type) && graphStore.visibleNodeTypes.includes(type) ? 'var(--text-primary)' : 'var(--text-muted)' }"
        >
          <input
            type="checkbox"
            :checked="graphStore.visibleNodeTypes.includes(type)"
            :disabled="!isNodeTypeAvailable(type)"
            @change="graphStore.toggleNodeType(type)"
            class="accent-[var(--accent)]"
          />
          {{ t('nodeType.' + type) }}
          <span v-if="nodeTypeCounts[type]" class="text-[10px]">({{ nodeTypeCounts[type] }})</span>
        </label>
      </div>
    </div>

    <!-- Edge types -->
    <div>
      <div class="text-xs font-medium mb-2" style="color: var(--text-secondary)">{{ t('filter.edgeTypes') }}</div>
      <div class="flex flex-wrap gap-2">
        <label
          v-for="type in allEdgeTypes" :key="type"
          class="flex items-center gap-1.5 text-xs"
          :class="isEdgeTypeAvailable(type) ? 'cursor-pointer' : 'opacity-40 cursor-not-allowed'"
          :style="{ color: isEdgeTypeAvailable(type) && graphStore.visibleEdgeTypes.includes(type) ? 'var(--text-primary)' : 'var(--text-muted)' }"
        >
          <input
            type="checkbox"
            :checked="graphStore.visibleEdgeTypes.includes(type)"
            :disabled="!isEdgeTypeAvailable(type)"
            @change="graphStore.toggleEdgeType(type)"
            class="accent-[var(--accent)]"
          />
          {{ t('edgeType.' + type) }}
          <span v-if="edgeTypeCounts[type]" class="text-[10px]">({{ edgeTypeCounts[type] }})</span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useGraphStore } from '../../stores/graph'

const { t } = useI18n()
const graphStore = useGraphStore()

const allNodeTypes = [
  'function', 'class', 'field', 'annotation', 'marker_annotation', 'comment', 'file'
]

const allEdgeTypes = [
  'calls', 'injection_calls', 'implemented_by', 'contains', 'depends_on',
  'out_calls', 'super_calls', 'interface_calls', 'subtype_calls',
  'overridden_by', 'instance_of', 'documented_by', 'reads_field', 'writes_field',
  'maps_to', 'passes_to'
]

function normalizeNodeType(raw) {
  if (raw === 'annotations') return 'annotation'
  if (raw === 'marker_annotations') return 'marker_annotation'
  return raw
}

const nodeTypeCounts = computed(() => {
  const counts = {}
  graphStore.nodes.forEach(n => {
    const t = normalizeNodeType(n?.type || n?.properties?.node_type || 'unknown')
    counts[t] = (counts[t] || 0) + 1
  })
  return counts
})

const edgeTypeCounts = computed(() => {
  const counts = {}
  graphStore.edges.forEach(e => {
    const t = e?.type || e?.properties?.type || 'unknown'
    counts[t] = (counts[t] || 0) + 1
  })
  return counts
})

const availableNodeTypes = computed(() =>
  new Set(graphStore.nodes.map(n => normalizeNodeType(n?.type || n?.properties?.node_type || 'unknown')))
)

const availableEdgeTypes = computed(() =>
  new Set(graphStore.edges.map(e => e?.type || e?.properties?.type || 'unknown'))
)

function isNodeTypeAvailable(type) {
  return availableNodeTypes.value.has(type)
}

function isEdgeTypeAvailable(type) {
  return availableEdgeTypes.value.has(type)
}
</script>
