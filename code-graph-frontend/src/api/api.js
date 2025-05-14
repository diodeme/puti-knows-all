// API调用封装

// API基础URL
const API_BASE_URL =
  process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1'

/**
 * 发送API请求
 * @param {string} endpoint - API端点
 * @param {string} method - 请求方法(GET, POST等)
 * @param {object} data - 请求数据
 * @param {number} retryCount - 重试次数，默认为0
 * @returns {Promise<any>} 响应数据
 */
const fetchApi = async (
  endpoint,
  method = 'GET',
  data = null,
  retryCount = 0
) => {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json'
    }
  }

  // Spring Boot后端不需要传递会话ID

  if (data) {
    options.body = JSON.stringify(data)
  }

  console.log(`请求API: ${API_BASE_URL}${endpoint}`, { method, data })

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, options)

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({
        message: '请求失败'
      }))

      console.error(`API请求失败: ${API_BASE_URL}${endpoint}`, {
        status: response.status,
        statusText: response.statusText,
        error: errorData
      })

      throw new Error(
        errorData.message || errorData.detail || response.statusText || '请求失败'
      )
    }

    const responseData = await response.json()
    console.log(`API请求成功: ${API_BASE_URL}${endpoint}`, responseData)
    return responseData
  } catch (error) {
    console.error(`API请求异常: ${API_BASE_URL}${endpoint}`, error)

    // 处理"Failed to fetch"错误，可能是后端服务未启动
    if (error.message === 'Failed to fetch' && retryCount < 2) {
      console.log(`尝试重试请求(${retryCount + 1}/2): ${endpoint}`)
      // 添加延迟后重试
      await new Promise(resolve => setTimeout(resolve, 1000))
      return fetchApi(endpoint, method, data, retryCount + 1)
    }

    // 如果是其它网络故障，可能服务暂未恢复

    throw error
  }
}

/**
 * 搜索方法
 * @param {string} methodName - 方法名称
 */
export const searchMethods = methodName => {
  return fetchApi('/search', 'POST', { method_name: methodName })
}

/**
 * 获取节点关系
 * @param {string} methodFullName - 方法全名
 * @param {string} queryType - 查询类型: self, upstream, downstream
 * @param {number} pathDepth - 路径深度，默认为1
 */
export const getNodes = (methodFullName, queryType, pathDepth = 1) => {
  return fetchApi('/nodes', 'POST', {
    method_full_name: methodFullName,
    query_type: queryType,
    path_depth: pathDepth
  })
}

/**
 * 获取节点详情
 * @param {string|number} nodeId - 节点ID
 */
export const getNodeDetail = nodeId => {
  const stringNodeId = String(nodeId)
  console.log('调用getNodeDetail API, nodeId:', stringNodeId)
  return fetchApi('/node_detail', 'POST', { node_id: stringNodeId })
}
