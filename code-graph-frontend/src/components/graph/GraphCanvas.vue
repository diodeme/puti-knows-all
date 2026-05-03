<template>
  <div ref="containerRef" class="relative w-full h-full overflow-hidden cursor-grab active:cursor-grabbing">
    <svg ref="svgRef" class="block w-full h-full"></svg>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center z-30"
         style="background: rgba(0,0,0,0.3); backdrop-filter: blur(2px)">
      <div class="flex flex-col items-center gap-3">
        <Loader2 :size="32" class="animate-spin" style="color: var(--accent)" />
        <span class="text-sm" style="color: var(--text-secondary)">{{ t('graph.loading') }}</span>
      </div>
    </div>

    <!-- 节点浮层 -->
    <Transition name="fade">
      <div v-if="tooltipNode" class="absolute glass-panel p-3 pointer-events-none z-20 animate-fade-in"
           :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px', maxWidth: '280px' }">
        <div class="flex items-center gap-2 mb-1">
          <span class="text-xs px-1.5 py-0.5 rounded"
                :style="nodeTypeBadgeStyle(tooltipNodeType)">
            {{ nodeTypeLabel(tooltipNodeType) }}
          </span>
          <span v-if="tooltipNodeData?.properties?.visibility"
                class="text-xs" style="color: var(--text-muted)">
            {{ tooltipNodeData.properties.visibility }}
          </span>
        </div>
        <div class="text-sm font-medium" style="color: var(--text-primary)">
          {{ getNodeDisplayName(tooltipNodeData) }}
        </div>
        <div v-if="getNodeFullName(tooltipNodeData)" class="text-xs truncate mt-0.5"
             style="color: var(--text-secondary)">
          {{ getNodeFullName(tooltipNodeData) }}
        </div>
      </div>
    </Transition>

    <!-- 控制栏 -->
    <div class="absolute bottom-4 right-4 flex gap-1.5 z-10">
      <button @click="locateCenter" class="btn-ghost p-2" :title="t('graph.locateCenter')">
        <Crosshair :size="16" />
      </button>
      <button @click="zoomIn" class="btn-ghost p-2" :title="t('graph.zoomIn')">
        <ZoomIn :size="16" />
      </button>
      <button @click="zoomOut" class="btn-ghost p-2" :title="t('graph.zoomOut')">
        <ZoomOut :size="16" />
      </button>
      <button @click="resetView" class="btn-ghost p-2" :title="t('graph.resetView')">
        <Maximize :size="16" />
      </button>
    </div>

    <!-- 图例 -->
    <div v-if="showLegend" class="absolute bottom-4 left-4 z-10 glass-panel p-3" style="min-width: 160px; font-size: 11px">
      <div class="flex items-center justify-between mb-2">
        <span class="font-medium" style="color: var(--text-primary)">{{ t('graph.legend') }}</span>
        <button @click="showLegend = false" class="p-0.5 rounded" style="color: var(--text-muted)">
          <X :size="12" />
        </button>
      </div>
      <div class="space-y-1 mb-2">
        <div class="flex items-center gap-2" v-for="item in nodeLegend" :key="item.label">
          <span v-if="item.dashed" class="inline-block w-2.5 h-2.5 rounded-full shrink-0"
                style="background: var(--bg-glass-inner); border: 1.5px dashed var(--text-muted)" />
          <span v-else class="inline-block w-2.5 h-2.5 rounded-full shrink-0" :style="{ background: item.color }" />
          <span style="color: var(--text-secondary)">{{ item.label }}</span>
        </div>
      </div>
      <div style="border-top: 1px solid var(--divider)" class="pt-2 space-y-1">
        <div class="flex items-center gap-2" v-for="item in edgeLegend" :key="item.label">
          <span v-if="item.dashed" class="inline-block w-4 shrink-0"
                :style="{ background: `repeating-linear-gradient(90deg, ${item.color}, ${item.color} 3px, transparent 3px, transparent 5px)`, height: '2px' }" />
          <span v-else class="inline-block w-4 shrink-0" :style="{ background: item.color, height: '2px' }" />
          <span style="color: var(--text-secondary)">{{ item.label }}</span>
        </div>
      </div>
    </div>
    <button v-else @click="showLegend = true"
            class="absolute bottom-4 left-4 z-10 btn-ghost px-2.5 py-1.5 text-xs"
            :title="t('graph.showLegend')">
      {{ t('graph.legend') }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as d3 from 'd3'
import { ZoomIn, ZoomOut, Maximize, X, Loader2, Crosshair } from 'lucide-vue-next'

const { t } = useI18n()

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  selectedNodeId: { type: String, default: null },
  currentMethod: { type: Object, default: null },
  queryType: { type: String, default: 'self' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['nodeClick', 'nodeDblClick', 'deselect'])

const containerRef = ref(null)
const svgRef = ref(null)
const tooltipNode = ref(null)
const tooltipNodeData = ref(null)
const tooltipPos = ref({ x: 0, y: 0 })

let simulation = null
let zoomBehavior = null
let svgSelection = null
let gSelection = null
let nodeElements = null
let linkElements = null

const tooltipNodeType = ref('')
const showLegend = ref(true)

const nodeLegend = computed(() => [
  { label: t('nodeType.function'), color: '#3b82f6' },
  { label: t('nodeType.class'), color: '#f97316' },
  { label: t('nodeType.field'), color: '#06b6d4' },
  { label: t('nodeType.annotation'), color: '#ec4899' },
  { label: t('nodeType.marker_annotation'), color: '#ec4899' },
  { label: t('nodeType.comment'), color: '#94a3b8' },
  { label: t('nodeType.file'), color: '#64748b' },
  { label: t('nodeType.entryMethod'), color: '#fbbf24' },
  { label: t('nodeType.libraryNode'), dashed: true }
])

const edgeLegend = computed(() => [
  { label: t('edgeType.calls'), color: '#3b82f6' },
  { label: t('edgeType.out_calls'), color: '#0ea5e9' },
  { label: t('edgeType.super_calls'), color: '#7c3aed' },
  { label: t('edgeType.interface_calls'), color: '#8b5cf6' },
  { label: t('edgeType.subtype_calls'), color: '#c084fc' },
  { label: t('edgeType.implemented_by'), color: '#22c55e' },
  { label: t('edgeType.overridden_by'), color: '#ef4444' },
  { label: t('edgeType.depends_on'), color: '#eab308' },
  { label: t('edgeType.injection_calls'), color: '#ec4899' },
  { label: t('edgeType.instance_of'), color: '#f97316' },
  { label: t('edgeType.contains'), color: '#64748b', dashed: true },
  { label: t('edgeType.documented_by'), color: '#94a3b8' },
  { label: t('edgeType.reads_field'), color: '#06b6d4' },
  { label: t('edgeType.writes_field'), color: '#14b8a6' },
  { label: t('edgeType.maps_to'), color: '#2dd4bf' },
  { label: t('edgeType.passes_to'), color: '#5eead4' }
])

function getNodeType(node) {
  const raw = node?.type || node?.properties?.node_type || 'unknown'
  if (raw === 'annotations') return 'annotation'
  if (raw === 'marker_annotations') return 'marker_annotation'
  return raw
}

function getNodeFullName(node) {
  return node?.properties?.full_name || node?.properties?.qualified_name || node?.full_name || ''
}

function getNodeDisplayName(node) {
  return node?.label || node?.name || node?.properties?.name || node?.properties?.label || getNodeFullName(node)?.split(/[#.]/).pop() || String(node?.id || '')
}

function nodeTypeLabel(type) {
  return t('nodeType.' + type, type)
}

function nodeTypeBadgeStyle(type) {
  const colors = {
    function: { bg: 'rgba(59,130,246,0.2)', color: '#3b82f6' },
    class: { bg: 'rgba(249,115,22,0.2)', color: '#f97316' },
    field: { bg: 'rgba(6,182,212,0.2)', color: '#06b6d4' },
    annotation: { bg: 'rgba(236,72,153,0.2)', color: '#ec4899' },
    marker_annotation: { bg: 'rgba(236,72,153,0.2)', color: '#ec4899' },
    file: { bg: 'rgba(100,116,139,0.2)', color: '#64748b' }
  }
  const c = colors[type] || colors.function
  return { background: c.bg, color: c.color }
}

function getNodeColor(node) {
  const type = getNodeType(node)
  const colors = {
    function: '#3b82f6', class: '#f97316', field: '#06b6d4',
    annotation: '#ec4899', marker_annotation: '#ec4899',
    file: '#64748b', comment: '#94a3b8'
  }
  if (node?.properties?.source_node) return '#fbbf24'
  if (node?.properties?.dao_node) return '#34d399'
  return colors[type] || '#6366f1'
}

function getNodeRadius(node) {
  if (node?.properties?.source_node) return 14
  if (node?.properties?.is_entry_point) return 10
  const type = getNodeType(node)
  if (type === 'class') return 9
  return 7
}

function getEdgeColor(edge) {
  const type = edge?.type || edge?.properties?.type || 'unknown'
  const colors = {
    // SEMANTIC - Call chain (blue-purple family)
    calls: '#3b82f6',               // blue - direct call
    out_calls: '#0ea5e9',           // sky blue - external call
    super_calls: '#7c3aed',         // deep purple - super call
    interface_calls: '#8b5cf6',     // violet - interface call
    subtype_calls: '#c084fc',       // light purple - subtype call
    // SEMANTIC - Inheritance/Override
    implemented_by: '#22c55e',      // green - interface implementation
    overridden_by: '#ef4444',       // red - method override
    // SEMANTIC - Dependency
    depends_on: '#eab308',          // yellow - dependency
    // FRAMEWORK
    injection_calls: '#ec4899',     // pink - DI injection
    instance_of: '#f97316',         // orange - instantiation
    // STRUCTURAL
    contains: '#64748b',            // gray - containment
    documented_by: '#94a3b8',       // light gray - documentation
    // DATA_FLOW (teal family)
    reads_field: '#06b6d4',         // cyan - read field
    writes_field: '#14b8a6',        // teal - write field
    maps_to: '#2dd4bf',             // mint - object mapping
    passes_to: '#5eead4'            // light mint - parameter pass-through
  }
  return colors[type] || '#64748b'
}

function renderGraph() {
  if (!svgRef.value || !containerRef.value) return

  d3.select(svgRef.value).selectAll('*').remove()
  if (simulation) simulation.stop()

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  if (width === 0 || height === 0) return

  svgSelection = d3.select(svgRef.value)
    .attr('viewBox', `0 0 ${width} ${height}`)
    .attr('preserveAspectRatio', 'xMidYMid meet')

  // Defs
  const defs = svgSelection.append('defs')

  const glowFilter = defs.append('filter')
    .attr('id', 'glow')
    .attr('x', '-50%').attr('y', '-50%')
    .attr('width', '200%').attr('height', '200%')
  glowFilter.append('feGaussianBlur')
    .attr('stdDeviation', '3')
    .attr('result', 'blur')
  glowFilter.append('feComposite')
    .attr('in', 'SourceGraphic')
    .attr('in2', 'blur')
    .attr('operator', 'over')

  const strongGlow = defs.append('filter')
    .attr('id', 'glow-strong')
    .attr('x', '-50%').attr('y', '-50%')
    .attr('width', '200%').attr('height', '200%')
  strongGlow.append('feGaussianBlur')
    .attr('stdDeviation', '5')
    .attr('result', 'blur')
  strongGlow.append('feComposite')
    .attr('in', 'SourceGraphic')
    .attr('in2', 'blur')
    .attr('operator', 'over')

  const edgeTypes = [...new Set(props.edges.map(e => e?.type || e?.properties?.type || 'default'))]
  edgeTypes.forEach(type => {
    const color = getEdgeColor({ type })
    defs.append('marker')
      .attr('id', `arrow-${type}`)
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 8)
      .attr('refY', 0)
      .attr('markerWidth', 5)
      .attr('markerHeight', 5)
      .attr('orient', 'auto')
      .append('path')
      .attr('d', 'M0,-3L6,0L0,3')
      .attr('fill', color)
  })

  gSelection = svgSelection.append('g')

  // Click on background → deselect
  svgSelection.on('click', () => {
    emit('deselect')
  })

  // Prepare data
  const nodeMap = new Map()
  props.nodes.forEach(n => {
    if (!nodeMap.has(n.id)) {
      nodeMap.set(n.id, {
        ...n,
        x: width / 2 + (Math.random() - 0.5) * 200,
        y: height / 2 + (Math.random() - 0.5) * 200
      })
    }
  })

  const simNodes = Array.from(nodeMap.values())
  const simLinks = props.edges
    .filter(e => nodeMap.has(e.source) && nodeMap.has(e.target))
    .map(e => ({
      source: nodeMap.get(e.source),
      target: nodeMap.get(e.target),
      ...e
    }))

  // Force simulation
  simulation = d3.forceSimulation(simNodes)
    .force('link', d3.forceLink(simLinks).id(d => d.id).distance(100).strength(0.4))
    .force('charge', d3.forceManyBody().strength(-300).distanceMax(400))
    .force('center', d3.forceCenter(width / 2, height / 2).strength(0.05))
    .force('collision', d3.forceCollide().radius(d => getNodeRadius(d) + 6))
    .force('x', d3.forceX(width / 2).strength(0.02))
    .force('y', d3.forceY(height / 2).strength(0.02))
    .alphaDecay(0.02)
    .velocityDecay(0.3)

  // Zoom
  zoomBehavior = d3.zoom()
    .scaleExtent([0.1, 8])
    .on('zoom', (event) => {
      gSelection.attr('transform', event.transform)
    })
  svgSelection.call(zoomBehavior)

  // Draw edges
  const linkGroup = gSelection.append('g')
  linkElements = linkGroup.selectAll('line')
    .data(simLinks)
    .enter()
    .append('line')
    .attr('stroke', d => getEdgeColor(d))
    .attr('stroke-opacity', 0)
    .attr('stroke-width', d => {
      const type = d.type || d?.properties?.type
      return (type === 'contains' || type === 'documented_by') ? 1 : 1.5
    })
    .attr('stroke-dasharray', d => {
      const type = d.type || d?.properties?.type
      return (type === 'contains') ? '4,3' : 'none'
    })
    .attr('marker-end', d => `url(#arrow-${d.type || d?.properties?.type || 'default'})`)

  // Draw nodes
  const nodeGroup = gSelection.append('g')
  nodeElements = nodeGroup.selectAll('g')
    .data(simNodes)
    .enter()
    .append('g')
    .attr('cursor', 'pointer')
    .call(d3.drag()
      .on('start', dragStarted)
      .on('drag', dragged)
      .on('end', dragEnded)
    )

  // Glow layer
  nodeElements.append('circle')
    .attr('class', 'node-glow')
    .attr('r', d => getNodeRadius(d) + 4)
    .attr('fill', d => getNodeColor(d))
    .attr('opacity', 0)
    .attr('filter', d => d?.properties?.source_node ? 'url(#glow-strong)' : 'url(#glow)')

  // Main circle
  nodeElements.append('circle')
    .attr('class', 'node-main')
    .attr('r', d => getNodeRadius(d))
    .attr('fill', d => getNodeColor(d))
    .attr('stroke', d => getNodeStroke(d))
    .attr('stroke-width', d => getNodeStrokeWidth(d))
    .attr('stroke-dasharray', d => getNodeStrokeDash(d) || 'none')
    .attr('opacity', d => {
      const isLib = d?.properties?.is_library === true || d?.properties?.is_library === 'true'
      return isLib ? 0.6 : 0.95
    })

  // Labels
  nodeElements.append('text')
    .text(d => {
      const name = getNodeDisplayName(d)
      return name.length > 15 ? name.substring(0, 13) + '...' : name
    })
    .attr('dx', d => getNodeRadius(d) + 6)
    .attr('dy', 4)
    .attr('font-size', '10px')
    .attr('font-family', 'Inter, sans-serif')
    .attr('font-weight', d => d?.properties?.source_node ? '600' : '400')
    .attr('fill', d => d?.properties?.source_node ? 'var(--text-primary)' : 'var(--text-secondary)')
    .attr('pointer-events', 'none')

  // Entrance animation
  nodeElements.attr('opacity', 0)
    .transition()
    .duration(400)
    .delay((d, i) => i * 15)
    .attr('opacity', 1)

  linkElements.transition()
    .duration(600)
    .delay((d, i) => 200 + i * 10)
    .attr('stroke-opacity', d => {
      const type = d.type || d?.properties?.type
      if (type === 'contains') return 0.45
      if (type === 'documented_by') return 0.2
      return 0.35
    })

  // Interactions
  nodeElements
    .on('click', (event, d) => {
      event.stopPropagation()
      emit('nodeClick', d.id)
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation()
      emit('nodeDblClick', d)
    })
    .on('mouseenter', (event, d) => {
      const rect = containerRef.value.getBoundingClientRect()
      tooltipNode.value = d.id
      tooltipNodeData.value = d
      tooltipNodeType.value = getNodeType(d)
      tooltipPos.value = { x: event.clientX - rect.left + 16, y: event.clientY - rect.top + 16 }

      d3.select(event.currentTarget)
        .select('.node-main')
        .transition().duration(150)
        .attr('r', getNodeRadius(d) + 4)
        .attr('stroke', '#ffffff')
        .attr('stroke-width', 2)
        .attr('stroke-opacity', 0.8)

      d3.select(event.currentTarget)
        .select('.node-glow')
        .transition().duration(200)
        .attr('opacity', 0.4)
    })
    .on('mouseleave', (event, d) => {
      tooltipNode.value = null
      updateNodeStyle(event.currentTarget, d)
    })

  // Tick — adjust line endpoints to stop at node boundaries
  simulation.on('tick', () => {
    linkElements.each(function(d) {
      const dx = d.target.x - d.source.x
      const dy = d.target.y - d.source.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 1) {
        d3.select(this).attr('x1', d.source.x).attr('y1', d.source.y)
          .attr('x2', d.target.x).attr('y2', d.target.y)
        return
      }
      const ux = dx / dist
      const uy = dy / dist
      const sourceR = getNodeRadius(d.source) + 2
      const targetR = getNodeRadius(d.target) + 5
      d3.select(this)
        .attr('x1', d.source.x + ux * sourceR)
        .attr('y1', d.source.y + uy * sourceR)
        .attr('x2', d.target.x - ux * targetR)
        .attr('y2', d.target.y - uy * targetR)
    })

    nodeElements.attr('transform', d => `translate(${d.x},${d.y})`)
  })
}

function getNodeStroke(d) {
  if (props.selectedNodeId === d.id) return '#ffffff'
  if (d?.properties?.source_node) return '#fbbf24'
  if (props.currentMethod && getNodeFullName(d) === props.currentMethod.full_name) return '#a78bfa'
  if (d?.properties?.is_library === true || d?.properties?.is_library === 'true') return 'var(--text-muted)'
  return 'rgba(255,255,255,0.15)'
}

function getNodeStrokeWidth(d) {
  if (props.selectedNodeId === d.id) return 3
  if (d?.properties?.source_node) return 2.5
  if (props.currentMethod && getNodeFullName(d) === props.currentMethod.full_name) return 2.5
  if (d?.properties?.is_library === true || d?.properties?.is_library === 'true') return 1.5
  return 1
}

function getNodeStrokeDash(d) {
  if (d?.properties?.is_library === true || d?.properties?.is_library === 'true') return '4,3'
  return null
}

function updateNodeStyle(element, d) {
  const isSelected = props.selectedNodeId === d.id
  const isSource = d?.properties?.source_node
  const isCurrentMethod = props.currentMethod && getNodeFullName(d) === props.currentMethod.full_name
  const isLib = d?.properties?.is_library === true || d?.properties?.is_library === 'true'
  let stroke = 'rgba(255,255,255,0.15)'
  let strokeW = 1
  let dashArr = 'none'
  if (isSelected) { stroke = '#ffffff'; strokeW = 3 }
  else if (isSource) { stroke = '#fbbf24'; strokeW = 2.5 }
  else if (isCurrentMethod) { stroke = '#a78bfa'; strokeW = 2.5 }
  else if (isLib) { stroke = 'var(--text-muted)'; strokeW = 1.5; dashArr = '4,3' }
  d3.select(element)
    .select('.node-main')
    .transition().duration(200)
    .attr('r', getNodeRadius(d))
    .attr('stroke', stroke)
    .attr('stroke-width', strokeW)
    .attr('stroke-dasharray', dashArr)
    .attr('stroke-opacity', 1)

  d3.select(element)
    .select('.node-glow')
    .transition().duration(300)
    .attr('opacity', 0)
}

function dragStarted(event, d) {
  if (!event.active) simulation.alphaTarget(0.3).restart()
  d.fx = d.x
  d.fy = d.y
}

function dragged(event, d) {
  d.fx = event.x
  d.fy = event.y
}

function dragEnded(event, d) {
  if (!event.active) simulation.alphaTarget(0)
  d.fx = null
  d.fy = null
}

function zoomIn() {
  svgSelection.transition().duration(300).call(zoomBehavior.scaleBy, 1.5)
}

function zoomOut() {
  svgSelection.transition().duration(300).call(zoomBehavior.scaleBy, 0.67)
}

function resetView() {
  svgSelection.transition().duration(500).call(zoomBehavior.transform, d3.zoomIdentity)
}

function locateCenter() {
  if (!nodeElements || !containerRef.value) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  let centerNode = null
  nodeElements.each(function(d) {
    if (d?.properties?.source_node) centerNode = d
  })
  if (!centerNode && props.currentMethod) {
    nodeElements.each(function(d) {
      if (getNodeFullName(d) === props.currentMethod.full_name) centerNode = d
    })
  }
  if (!centerNode) return

  const scale = 1.2
  const transform = d3.zoomIdentity
    .translate(width / 2 - centerNode.x * scale, height / 2 - centerNode.y * scale)
    .scale(scale)
  svgSelection.transition().duration(600).call(zoomBehavior.transform, transform)
}

// Re-render when data changes
watch(() => [props.nodes, props.edges], () => {
  nextTick(renderGraph)
}, { deep: true })

// Update selection strokes without full re-render
watch(() => props.selectedNodeId, () => {
  if (!nodeElements) return
  nodeElements.each(function(d) {
    d3.select(this).select('.node-main')
      .attr('stroke', getNodeStroke(d))
      .attr('stroke-width', getNodeStrokeWidth(d))
  })
})

onMounted(() => {
  nextTick(renderGraph)
})

onUnmounted(() => {
  if (simulation) simulation.stop()
})
</script>

<style scoped>
svg {
  display: block;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
