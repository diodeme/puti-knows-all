const BASE_URL = '/documentation/api/documentation/query'

async function handleResponse(response) {
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  const data = await response.json()
  if (!data.success) {
    throw new Error(data.message || 'Request failed')
  }
  return data.data
}

async function get(endpoint) {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' }
  })
  return handleResponse(response)
}

async function post(endpoint, data) {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  return handleResponse(response)
}

export function getProjects() {
  return get('/projects')
}

export function getAggregatedDocumentations(projectId) {
  return post('/projects/aggregated-documentations', { projectId })
}

export function getProcessDocumentations(aggregatedDocumentationId) {
  return get(`/aggregated-documentations/${aggregatedDocumentationId}/process-documentations`)
}

export function getAggregatedDocumentationContent(aggregatedDocumentationId) {
  return get(`/aggregated-documentations/${aggregatedDocumentationId}/content`)
}

export function getProcessDocumentationContent(documentationId) {
  return get(`/process-documentations/${documentationId}/content`)
}

export function getProcessDocumentationMethods(documentationId, page = 1, size = 10) {
  return get(`/process-documentations/${documentationId}/methods?page=${page}&size=${size}`)
}

export function getMethodContent(methodId) {
  return get(`/methods/${methodId}/content`)
}
