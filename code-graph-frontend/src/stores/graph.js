import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNodes as fetchNodes } from '../api/graph'

export const useGraphStore = defineStore('graph', () => {
  const nodes = ref([])
  const edges = ref([])
  const meta = ref({})
  const selectedNodeId = ref(null)
  const hoveredNodeId = ref(null)
  const hoveredEdgeId = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const queryType = ref('both')
  const pathDepth = ref(2)
  const currentMethod = ref(null)

  const visibleNodeTypes = ref([])
  const visibleEdgeTypes = ref([])

  const nodeTypeStats = computed(() => meta.value?.nodeTypeStats || {})
  const edgeTypeStats = computed(() => meta.value?.edgeTypeStats || {})

  async function loadGraph(methodFullName, type, depth) {
    if (!methodFullName) return
    loading.value = true
    error.value = null
    try {
      const result = await fetchNodes(methodFullName, type || queryType.value, depth || pathDepth.value)
      const normalizedNodes = (result?.nodes || []).map(node => {
        if (node?.id !== undefined && node?.id !== null) return node
        const fallbackId = node?.properties?.id
        return fallbackId != null ? { ...node, id: fallbackId } : node
      })

      nodes.value = normalizedNodes
      edges.value = result?.edges || []
      meta.value = result?.meta || {}

      const nodeTypes = [...new Set(normalizedNodes.map(n => {
        const raw = n?.type || n?.properties?.node_type || 'unknown'
        if (raw === 'annotations') return 'annotation'
        if (raw === 'marker_annotations') return 'marker_annotation'
        return raw
      }))]
      const edgeTypes = [...new Set((edges.value).map(e => e?.type || e?.properties?.type || 'unknown'))]

      visibleNodeTypes.value = nodeTypes
      visibleEdgeTypes.value = edgeTypes

      selectedNodeId.value = null
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  function selectNode(nodeId) {
    selectedNodeId.value = nodeId
  }

  function setQueryType(type) {
    queryType.value = type
  }

  function setPathDepth(depth) {
    pathDepth.value = depth
  }

  function setCurrentMethod(method) {
    currentMethod.value = method
  }

  function toggleNodeType(type) {
    const idx = visibleNodeTypes.value.indexOf(type)
    if (idx >= 0) visibleNodeTypes.value.splice(idx, 1)
    else visibleNodeTypes.value.push(type)
  }

  function toggleEdgeType(type) {
    const idx = visibleEdgeTypes.value.indexOf(type)
    if (idx >= 0) visibleEdgeTypes.value.splice(idx, 1)
    else visibleEdgeTypes.value.push(type)
  }

  return {
    nodes, edges, meta, selectedNodeId, hoveredNodeId, hoveredEdgeId,
    loading, error, queryType, pathDepth, currentMethod,
    visibleNodeTypes, visibleEdgeTypes,
    nodeTypeStats, edgeTypeStats,
    loadGraph, selectNode, setQueryType, setPathDepth, setCurrentMethod,
    toggleNodeType, toggleEdgeType
  }
})
