<template>
  <div class="p-5">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-sm font-bold" style="color: var(--text-primary)">{{ t('detail.title') }}</h3>
      <button @click="$emit('close')" class="p-1 rounded-lg"
              style="color: var(--text-muted)">
        <X :size="16" />
      </button>
    </div>

    <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
    <div v-else-if="error" class="text-center py-8" style="color: var(--error)">{{ error }}</div>
    <div v-else-if="detail">
      <div class="flex flex-wrap gap-1.5 mb-3">
        <span class="text-xs px-2 py-0.5 rounded" :style="typeBadgeStyle">
          {{ nodeType }}
        </span>
        <span v-if="detail.raw_properties?.visibility" class="text-xs px-2 py-0.5 rounded"
              style="background: rgba(52,211,153,0.15); color: #34d399">
          {{ detail.raw_properties.visibility }}
        </span>
        <span v-if="detail.raw_properties?.is_static" class="text-xs px-2 py-0.5 rounded"
              style="background: rgba(248,113,113,0.15); color: #f87171">
          static
        </span>
        <span v-if="detail.raw_properties?.is_constructor" class="text-xs px-2 py-0.5 rounded"
              style="background: rgba(6,182,212,0.15); color: #06b6d4">
          constructor
        </span>
        <span v-if="isLibrary" class="text-xs px-2 py-0.5 rounded"
              style="background: rgba(100,116,139,0.15); color: #64748b">
          {{ t('detail.library') }}
        </span>
      </div>

      <div class="text-lg font-bold mb-1" style="color: var(--text-primary)">{{ detail.name }}</div>
      <div class="text-xs mb-3 break-all" style="color: var(--text-secondary)">{{ detail.full_name }}</div>
      <div class="text-xs mb-4" style="color: var(--text-muted)">ID: {{ nodeId }}</div>

      <div style="border-top: 1px solid var(--divider)" class="mb-4" />

      <div class="mb-4">
        <h4 class="text-xs font-bold mb-2" style="color: var(--text-secondary)">{{ t('detail.sourceCode') }}</h4>
        <div v-if="detail.content" class="glass-panel-inner p-3 overflow-auto max-h-72">
          <pre class="text-xs leading-relaxed" style="color: var(--text-primary); font-family: 'JetBrains Mono', monospace">{{ detail.content }}</pre>
        </div>
        <div v-else class="text-xs" style="color: var(--text-muted)">{{ t('detail.noSourceCode') }}</div>
      </div>

      <div>
        <h4 class="text-xs font-bold mb-2" style="color: var(--text-secondary)">{{ t('detail.properties') }}</h4>
        <div class="glass-panel-inner p-3">
          <div class="grid grid-cols-2 gap-2 text-xs">
            <div v-if="detail.raw_properties?.line_start">
              <span style="color: var(--text-muted)">{{ t('detail.lineRange') }}</span>
              <span class="ml-2" style="color: var(--text-primary)">
                {{ detail.raw_properties.line_start }} - {{ detail.raw_properties.line_end }}
              </span>
            </div>
            <div v-if="detail.raw_properties?.complexity != null">
              <span style="color: var(--text-muted)">{{ t('detail.complexity') }}</span>
              <span class="ml-2" style="color: var(--text-primary)">{{ detail.raw_properties.complexity }}</span>
            </div>
            <div v-if="detail.raw_properties?.repo_id">
              <span style="color: var(--text-muted)">{{ t('detail.repo') }}</span>
              <span class="ml-2" style="color: var(--text-primary)">{{ detail.raw_properties.repo_id }}</span>
            </div>
            <div v-if="detail.raw_properties?.branch_name">
              <span style="color: var(--text-muted)">{{ t('detail.branch') }}</span>
              <span class="ml-2" style="color: var(--text-primary)">{{ detail.raw_properties.branch_name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { X } from 'lucide-vue-next'
import { getNodeDetail } from '../../api/graph'

const { t } = useI18n()

const props = defineProps({
  nodeId: { type: String, required: true }
})

defineEmits(['close'])

const detail = ref(null)
const loading = ref(false)
const error = ref(null)

const nodeType = computed(() =>
  detail.value?.raw_properties?.node_type || detail.value?.raw_properties?.type || 'unknown'
)

const isLibrary = computed(() =>
  detail.value?.raw_properties?.is_library === true || detail.value?.raw_properties?.is_library === 'true'
)

const typeBadgeStyle = computed(() => {
  const colors = {
    function: { bg: 'rgba(59,130,246,0.2)', color: '#3b82f6' },
    class: { bg: 'rgba(249,115,22,0.2)', color: '#f97316' },
    field: { bg: 'rgba(6,182,212,0.2)', color: '#06b6d4' },
    annotation: { bg: 'rgba(236,72,153,0.2)', color: '#ec4899' },
    marker_annotation: { bg: 'rgba(236,72,153,0.2)', color: '#ec4899' },
    file: { bg: 'rgba(100,116,139,0.2)', color: '#64748b' },
    comment: { bg: 'rgba(148,163,184,0.2)', color: '#94a3b8' }
  }
  const nt = nodeType.value
  if (nt === 'annotations') return colors.annotation
  if (nt === 'marker_annotations') return colors.marker_annotation
  return colors[nt] || colors.function
})

async function fetchDetail() {
  if (!props.nodeId) return
  loading.value = true
  error.value = null
  try {
    detail.value = await getNodeDetail(props.nodeId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch(() => props.nodeId, fetchDetail, { immediate: true })
</script>
