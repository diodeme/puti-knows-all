<template>
  <footer class="h-8 flex items-center px-5 gap-4 text-xs shrink-0"
          style="color: var(--text-muted); border-top: 1px solid var(--divider)">
    <span v-if="graphStore.nodes.length">{{ t('status.nodes') }} {{ graphStore.nodes.length }}</span>
    <span v-if="graphStore.edges.length">{{ t('status.edges') }} {{ graphStore.edges.length }}</span>
    <span v-if="elapsedMs">{{ t('status.query') }} {{ elapsedMs }}ms</span>
    <span v-if="zoomLevel">{{ t('status.zoom') }} {{ zoomLevel }}%</span>
    <span class="ml-auto">{{ t('status.hint') }}</span>
  </footer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { useGraphStore } from '../../stores/graph'

const { t } = useI18n()
const graphStore = useGraphStore()

const props = defineProps({
  elapsedMs: { type: Number, default: null },
  zoomLevel: { type: Number, default: null }
})
</script>
