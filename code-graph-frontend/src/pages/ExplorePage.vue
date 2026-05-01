<template>
  <div class="flex flex-col" style="height: calc(100vh - 56px)">
    <div class="flex flex-1 overflow-hidden">
      <aside
        v-show="!leftCollapsed"
        class="w-[280px] shrink-0 flex flex-col overflow-y-auto border-r"
        style="border-color: var(--divider)"
      >
        <SearchPanel />
        <div style="border-top: 1px solid var(--divider)" />
        <EntryPointsPanel />
        <div style="border-top: 1px solid var(--divider)" />
        <FilterPanel />
      </aside>

      <div class="flex-1 relative overflow-hidden">
        <button
          @click="leftCollapsed = !leftCollapsed"
          class="absolute top-3 left-3 z-10 p-1.5 rounded-lg"
          style="background: var(--bg-glass); border: 1px solid var(--border-glass); color: var(--text-muted)"
        >
          <PanelLeftClose v-if="!leftCollapsed" :size="16" />
          <PanelLeftOpen v-else :size="16" />
        </button>

        <GraphCanvas
          v-if="graphStore.currentMethod"
          :nodes="filteredNodes"
          :edges="filteredEdges"
          :selected-node-id="graphStore.selectedNodeId"
          :current-method="graphStore.currentMethod"
          :query-type="graphStore.queryType"
          :loading="graphStore.loading"
          @node-click="graphStore.selectNode"
          @node-dblclick="handleNodeDblClick"
          @deselect="graphStore.selectNode(null)"
        />
        <div v-else class="flex items-center justify-center h-full" style="color: var(--text-muted)">
          <div class="text-center">
            <Search :size="48" class="mx-auto mb-4 opacity-30" />
            <p class="text-lg">{{ t('explore.emptyTip') }}</p>
          </div>
        </div>
      </div>

      <aside
        v-show="!rightCollapsed"
        class="w-[460px] shrink-0 overflow-y-auto border-l"
        style="border-color: var(--divider)"
      >
        <NodeDetail
          v-if="graphStore.selectedNodeId"
          :node-id="graphStore.selectedNodeId"
        />
        <div v-else class="p-5 text-center" style="color: var(--text-muted)">
          <p>{{ t('explore.clickNodeTip') }}</p>
        </div>
      </aside>

      <button
        v-if="rightCollapsed"
        @click="rightCollapsed = false"
        class="absolute top-3 right-3 z-10 p-1.5 rounded-lg"
        style="background: var(--bg-glass); border: 1px solid var(--border-glass); color: var(--text-muted)"
      >
        <PanelRightOpen :size="16" />
      </button>
    </div>

    <StatusBar />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { PanelLeftClose, PanelLeftOpen, PanelRightOpen, Search } from 'lucide-vue-next'
import { useGraphStore } from '../stores/graph'
import { useSearchStore } from '../stores/search'
import SearchPanel from '../components/explore/SearchPanel.vue'
import EntryPointsPanel from '../components/explore/EntryPointsPanel.vue'
import FilterPanel from '../components/explore/FilterPanel.vue'
import GraphCanvas from '../components/graph/GraphCanvas.vue'
import NodeDetail from '../components/detail/NodeDetail.vue'
import StatusBar from '../components/layout/StatusBar.vue'

const route = useRoute()
const { t } = useI18n()
const graphStore = useGraphStore()
const searchStore = useSearchStore()

const leftCollapsed = ref(false)
const rightCollapsed = ref(false)

function normalizeNodeType(raw) {
  if (raw === 'annotations') return 'annotation'
  if (raw === 'marker_annotations') return 'marker_annotation'
  return raw
}

const filteredNodes = computed(() => {
  const types = graphStore.visibleNodeTypes
  if (types.length === 0) return graphStore.nodes
  return graphStore.nodes.filter(n => {
    const t = normalizeNodeType(n?.type || n?.properties?.node_type || 'unknown')
    return types.includes(t)
  })
})

const filteredEdges = computed(() => {
  const types = graphStore.visibleEdgeTypes
  const nodeIds = new Set(filteredNodes.value.map(n => n.id))
  if (types.length === 0) return graphStore.edges.filter(e => nodeIds.has(e.source) && nodeIds.has(e.target))
  return graphStore.edges.filter(e => {
    const t = e?.type || e?.properties?.type || 'unknown'
    return types.includes(t) && nodeIds.has(e.source) && nodeIds.has(e.target)
  })
})

function handleNodeDblClick(node) {
  if (node?.properties?.full_name) {
    graphStore.loadGraph(node.properties.full_name, 'both', 1)
  }
}

onMounted(() => {
  const method = route.query.method
  if (method) {
    const methodData = {
      full_name: method,
      fullName: method,
      nodeId: route.query.id,
      name: method.split('#').pop() || method.split('.').pop() || method
    }
    graphStore.setCurrentMethod(methodData)
    graphStore.loadGraph(method, 'both', 2)
  }
  const search = route.query.search
  if (search) {
    searchStore.searchQuiet(search)
  }
})

watch(() => route.query.method, (method) => {
  if (method && route.path === '/explore') {
    const methodData = {
      full_name: method,
      fullName: method,
      nodeId: route.query.id,
      name: method.split('#').pop() || method.split('.').pop() || method
    }
    graphStore.setCurrentMethod(methodData)
    graphStore.loadGraph(method, 'both', 2)
  }
})
</script>
