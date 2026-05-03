import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listProjects } from '../api/project'

export const useProjectStore = defineStore('project', () => {
  const projects = ref([])
  const selectedProjectId = ref(null)
  const selectedBranch = ref(null)
  const loading = ref(false)

  const currentProject = computed(() =>
    projects.value.find(p => p.projectId === selectedProjectId.value) || null
  )

  async function fetchProjects() {
    loading.value = true
    try {
      const result = await listProjects()
      projects.value = Array.isArray(result) ? result : []
      // Restore from localStorage or select first project
      const saved = localStorage.getItem('selectedProjectId')
      if (saved && projects.value.some(p => p.projectId === saved)) {
        selectProject(saved)
      } else if (projects.value.length > 0 && !selectedProjectId.value) {
        selectProject(projects.value[0].projectId)
      }
    } catch (e) {
      console.error('Failed to fetch projects:', e)
    } finally {
      loading.value = false
    }
  }

  function selectProject(projectId) {
    selectedProjectId.value = projectId
    const proj = projects.value.find(p => p.projectId === projectId)
    selectedBranch.value = proj?.branchName || null
    localStorage.setItem('selectedProjectId', projectId)
  }

  return {
    projects, selectedProjectId, selectedBranch, loading, currentProject,
    fetchProjects, selectProject
  }
})
