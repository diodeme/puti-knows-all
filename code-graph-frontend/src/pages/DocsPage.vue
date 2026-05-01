<template>
  <div class="p-6 max-w-6xl mx-auto">
    <div class="glass-panel p-8">
      <h1 class="text-2xl font-bold mb-4" style="color: var(--text-primary)">{{ t('docs.title') }}</h1>
      <p class="text-sm mb-6" style="color: var(--text-secondary)">
        {{ t('docs.description') }}
      </p>

      <div class="mb-6">
        <label class="text-xs font-medium block mb-2" style="color: var(--text-secondary)">{{ t('docs.selectProject') }}</label>
        <select v-model="selectedProjectId" @change="loadDocs" class="input-field max-w-xs">
          <option value="">{{ t('docs.selectPlaceholder') }}</option>
          <option v-for="p in projects" :key="p.projectId" :value="p.projectId">
            {{ p.projectName || p.projectId }}
          </option>
        </select>
      </div>

      <div v-if="loading" class="text-center py-8" style="color: var(--text-muted)">{{ t('common.loading') }}</div>
      <div v-else-if="aggregatedDocs.length === 0 && selectedProjectId" class="text-center py-8" style="color: var(--text-muted)">
        {{ t('docs.noDocs') }}
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="doc in aggregatedDocs" :key="doc.id"
          @click="selectDoc(doc)"
          class="p-4 rounded-lg cursor-pointer transition-colors"
          :style="doc === selectedDoc ? { background: 'var(--accent-glow)' } : {}"
          @mouseenter="onDocEnter($event, doc)"
          @mouseleave="onDocLeave($event, doc)"
        >
          <div class="font-medium text-sm" style="color: var(--text-primary)">{{ doc.title }}</div>
          <div v-if="doc.description" class="text-xs mt-1" style="color: var(--text-secondary)">{{ doc.description }}</div>
        </div>
      </div>

      <div v-if="selectedDoc && docContent" class="mt-6">
        <div style="border-top: 1px solid var(--divider)" class="mb-4" />
        <div class="prose prose-invert max-w-none" v-html="docContent" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  getProjects,
  getAggregatedDocumentations,
  getAggregatedDocumentationContent
} from '../api/documentation'

const { t } = useI18n()

const projects = ref([])
const selectedProjectId = ref('')
const aggregatedDocs = ref([])
const selectedDoc = ref(null)
const docContent = ref(null)
const loading = ref(false)

onMounted(async () => {
  try {
    projects.value = await getProjects()
  } catch (e) {
    console.error('Failed to load projects:', e)
  }
})

async function loadDocs() {
  if (!selectedProjectId.value) return
  loading.value = true
  try {
    aggregatedDocs.value = await getAggregatedDocumentations(selectedProjectId.value)
  } catch (e) {
    console.error('Failed to load docs:', e)
  } finally {
    loading.value = false
  }
}

function onDocEnter(event, doc) {
  if (doc !== selectedDoc.value) event.currentTarget.style.background = 'var(--bg-glass-hover)'
}

function onDocLeave(event, doc) {
  if (doc !== selectedDoc.value) event.currentTarget.style.background = 'transparent'
}

async function selectDoc(doc) {
  selectedDoc.value = doc
  try {
    const content = await getAggregatedDocumentationContent(doc.id)
    docContent.value = content
  } catch (e) {
    console.error('Failed to load doc content:', e)
  }
}
</script>
