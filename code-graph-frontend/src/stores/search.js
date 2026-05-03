import { defineStore } from 'pinia'
import { ref } from 'vue'
import { searchMethods } from '../api/graph'

const HISTORY_KEY = 'puti_search_history'
const MAX_HISTORY = 20

export const useSearchStore = defineStore('search', () => {
  const query = ref('')
  const results = ref([])
  const selectedMethod = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const history = ref(loadHistory())

  function loadHistory() {
    try {
      return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    } catch {
      return []
    }
  }

  function saveHistory() {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value.slice(0, MAX_HISTORY)))
  }

  function addHistory(methodName) {
    const existing = history.value.findIndex(h => h.query === methodName)
    if (existing >= 0) history.value.splice(existing, 1)
    history.value.unshift({ query: methodName, timestamp: Date.now() })
    history.value = history.value.slice(0, MAX_HISTORY)
    saveHistory()
  }

  // 搜索并记录历史（回车/按钮触发）
  async function search(methodName) {
    if (!methodName?.trim()) return
    loading.value = true
    error.value = null
    try {
      const response = await searchMethods(methodName)
      results.value = response?.data || response || []
      query.value = methodName
      addHistory(methodName)
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  // 仅搜索，不记录历史（输入时实时搜索）
  async function searchQuiet(methodName) {
    if (!methodName?.trim()) return
    loading.value = true
    error.value = null
    results.value = []
    try {
      const response = await searchMethods(methodName)
      results.value = response?.data || response || []
      query.value = methodName
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  function selectMethod(method) {
    selectedMethod.value = method
  }

  function clearHistory() {
    history.value = []
    localStorage.removeItem(HISTORY_KEY)
  }

  return { query, results, selectedMethod, loading, error, history, search, searchQuiet, selectMethod, clearHistory }
})
