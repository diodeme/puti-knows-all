const API_BASE_URL = '/api/v1'

async function fetchApi(endpoint, method = 'GET', data = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  }
  if (data) {
    options.body = JSON.stringify(data)
  }
  const response = await fetch(`${API_BASE_URL}${endpoint}`, options)
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(errorData.message || errorData.detail || response.statusText)
  }
  return response.json()
}

export function listProjects() {
  return fetchApi('/projects', 'GET')
}
