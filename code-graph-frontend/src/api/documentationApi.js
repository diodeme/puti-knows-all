// 说明书查询API服务模块
const BASE_URL = '/documentation/api/documentation/query';

// 通用请求处理函数
const handleResponse = async (response) => {
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  
  const data = await response.json();
  
  if (!data.success) {
    throw new Error(data.message || '请求失败');
  }
  
  return data.data;
};

// 通用GET请求
const get = async (endpoint) => {
  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return await handleResponse(response);
  } catch (error) {
    console.error(`GET ${endpoint} 失败:`, error);
    throw error;
  }
};

// 通用POST请求
const post = async (endpoint, data) => {
  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return await handleResponse(response);
  } catch (error) {
    console.error(`POST ${endpoint} 失败:`, error);
    throw error;
  }
};

// API接口函数

/**
 * 1. 查询所有已接入的子系统
 */
export const getProjects = async () => {
  return await get('/projects');
};

/**
 * 2. 查询子系统下所有聚合说明书
 * @param {string} projectId - 项目ID
 */
export const getAggregatedDocumentations = async (projectId) => {
  return await post('/projects/aggregated-documentations', { projectId });
};

/**
 * 3. 查询指定聚合说明书下所有流程说明书
 * @param {string} aggregatedDocumentationId - 聚合说明书ID
 */
export const getProcessDocumentations = async (aggregatedDocumentationId) => {
  return await get(`/aggregated-documentations/${aggregatedDocumentationId}/process-documentations`);
};

/**
 * 4. 根据聚合说明书ID查询说明书内容
 * @param {string} aggregatedDocumentationId - 聚合说明书ID
 */
export const getAggregatedDocumentationContent = async (aggregatedDocumentationId) => {
  return await get(`/aggregated-documentations/${aggregatedDocumentationId}/content`);
};

/**
 * 5. 根据流程说明书ID查询说明书内容
 * @param {string} documentationId - 流程说明书ID
 */
export const getProcessDocumentationContent = async (documentationId) => {
  return await get(`/process-documentations/${documentationId}/content`);
};

/**
 * 6. 根据流程说明书ID查询关联的所有方法ID（分页）
 * @param {string} documentationId - 流程说明书ID
 * @param {number} page - 页码，从1开始
 * @param {number} size - 每页大小
 */
export const getProcessDocumentationMethods = async (documentationId, page = 1, size = 10) => {
  return await get(`/process-documentations/${documentationId}/methods?page=${page}&size=${size}`);
};

/**
 * 7. 根据方法ID查询方法内容
 * @param {string} methodId - 方法ID
 */
export const getMethodContent = async (methodId) => {
  return await get(`/methods/${methodId}/content`);
};

// 错误类型定义
export class DocumentationApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'DocumentationApiError';
    this.status = status;
  }
}

// 导出所有API函数
const documentationApi = {
  getProjects,
  getAggregatedDocumentations,
  getProcessDocumentations,
  getAggregatedDocumentationContent,
  getProcessDocumentationContent,
  getProcessDocumentationMethods,
  getMethodContent,
};

export default documentationApi;
