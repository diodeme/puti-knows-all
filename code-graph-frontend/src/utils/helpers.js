/**
 * 工具函数集合
 */

/**
 * 截断文本，超过指定长度添加省略号
 * @param {string} text - 要截断的文本
 * @param {number} length - 最大长度
 * @returns {string} 截断后的文本
 */
export const truncateText = (text, length = 30) => {
  if (!text) return '';
  
  return text.length > length 
    ? `${text.substring(0, length)}...`
    : text;
};

/**
 * 格式化时间戳
 * @param {number} timestamp - 时间戳
 * @returns {string} 格式化后的日期时间
 */
export const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-';
  
  const date = new Date(timestamp);
  return date.toLocaleString();
};

/**
 * 获取方法简短名称
 * @param {string} fullName - 完整方法名
 * @returns {string} 简短名称
 */
export const getShortMethodName = (fullName) => {
  if (!fullName) return '';
  
  // 如果包含#，取#后面的部分
  if (fullName.includes('#')) {
    return fullName.split('#').pop();
  }
  
  // 如果包含.，取最后一个.后面的部分
  if (fullName.includes('.')) {
    return fullName.split('.').pop();
  }
  
  return fullName;
};

/**
 * 格式化查询类型名称
 * @param {string} queryType - 查询类型
 * @returns {string} 格式化后的名称
 */
export const formatQueryType = (queryType) => {
  switch (queryType) {
    case 'self': return '当前节点';
    case 'upstream': return '上游节点';
    case 'downstream': return '下游树';
    default: return queryType;
  }
}; 