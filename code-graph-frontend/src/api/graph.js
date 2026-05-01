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

export function searchMethods(methodName) {
  return fetchApi('/search', 'POST', { method_name: methodName })
}

export function getNodes(methodFullName, queryType, pathDepth = 1) {
  return fetchApi('/nodes', 'POST', {
    method_full_name: methodFullName,
    query_type: queryType,
    path_depth: pathDepth
  })
}

export function getNodeDetail(nodeId) {
  return fetchApi('/node_detail', 'POST', { node_id: String(nodeId) })
}

export function getEntryPoints(repoId = null, branchName = null) {
  return fetchApi('/get_entry_points', 'POST', {
    repo_id: repoId,
    branch_name: branchName
  })
}

export function getGlobalStats() {
  return fetchApi('/stats', 'GET')
}
