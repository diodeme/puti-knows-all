import React, { useEffect, useRef, useState } from 'react';
import { Box, Heading, Spinner, Text, useToast, HStack, Divider, Switch, FormControl, FormLabel, Tooltip, Icon, Button, Checkbox, Wrap, WrapItem, IconButton } from '@chakra-ui/react';
import { InfoIcon, ChevronUpIcon, ChevronDownIcon, SearchIcon, CloseIcon } from '@chakra-ui/icons';
import * as d3 from 'd3';
import { getNodes } from '../api/api';
import { defaultEdgePalette, graphStyleConfig } from '../config/graphStyleConfig';

const treeConfig = graphStyleConfig;

const getEdgeType = edge => edge?.type || edge?.properties?.type || 'unknown';

const getEdgeCategory = edge => edge?.category || edge?.properties?.category || 'SEMANTIC';

const getNodeType = node => node?.type || node?.properties?.node_type || 'unknown';

const getNodeFullName = node =>
  node?.properties?.full_name ||
  node?.properties?.qualified_name ||
  node?.full_name ||
  node?.qualified_name ||
  '';

const getNodeDisplayName = node => {
  const displayName =
    node?.label ||
    node?.name ||
    node?.properties?.name ||
    node?.properties?.label ||
    node?.properties?.simple_name ||
    getNodeFullName(node) ||
    node?.id;

  if (displayName === undefined || displayName === null || displayName === '') {
    return 'unknown';
  }

  return String(displayName);
};

const isMethodRootNode = (node, methodFullName) =>
  Boolean(methodFullName) && getNodeFullName(node) === methodFullName;

const getEdgeColor = (edgeType, dynamicEdgeColors) => dynamicEdgeColors[edgeType] || treeConfig.edgeColors[edgeType] || '#718096';

const getEdgeLabel = (edgeType, edgeTypeLabels = {}) => {
  if (edgeTypeLabels?.[edgeType]) {
    return edgeTypeLabels[edgeType];
  }
  const labels = {
    calls: '调用关系',
    implemented_by: '实现关系',
    overridden_by: '重写关系',
    out_calls: '外部调用',
    super_calls: '父类调用',
    interface_calls: '接口调用',
    subtype_calls: '子类调用',
    injection_calls: '注入调用',
    contains: '包含关系',
    depends_on: '依赖关系',
    documented_by: '文档关系',
    instance_of: '实例关系',
    reads_field: '读取字段',
    writes_field: '写入字段',
    maps_to: '映射关系',
    passes_to: '参数透传'
  };
  return labels[edgeType] || edgeType;
};

const getNodeTypeLabel = nodeType => {
  const labels = {
    file: '文件',
    function: '方法',
    class: '类',
    field: '字段',
    annotation: '注解',
    annotations: '注解',
    marker_annotation: '标记注解',
    marker_annotations: '标记注解',
    comment: '注释'
  };
  return labels[nodeType] || nodeType;
};

const getCategoryLabel = (category, edgeCategoryLabels = {}) => {
  if (edgeCategoryLabels?.[category]) {
    return edgeCategoryLabels[category];
  }
  const labels = {
    STRUCTURAL: '结构',
    SEMANTIC: '语义',
    FRAMEWORK: '框架',
    DATA_FLOW: '数据流'
  };
  return labels[category] || category;
};

const normalizeGraphResult = result => {
  const normalizedNodes = (result?.nodes || []).map(node => {
    if (node?.id !== undefined && node?.id !== null) {
      return node;
    }
    const fallbackId = node?.properties?.id;
    if (fallbackId === undefined || fallbackId === null) {
      return node;
    }
    return {
      ...node,
      id: fallbackId
    };
  });

  return {
    nodes: normalizedNodes,
    edges: result?.edges || [],
    meta: result?.meta || {}
  };
};

const NodeTree = ({ methodData, onNodeClick }) => {
  const svgRef = useRef(null);
  const transformRef = useRef(null); // 使用ref保存变换状态，避免重新渲染
  const zoomRef = useRef(null); // 保存zoom对象的引用
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState({ nodes: [], edges: [], meta: {} });
  const [showLabels, setShowLabels] = useState(false); // 控制是否显示标签
  const [edgeDisplayMode, setEdgeDisplayMode] = useState('tree'); // 边显示模式：'none', 'tree', 'all', 'dynamic'
  const [selectedNodeId, setSelectedNodeId] = useState(null); // 动态模式下选中的节点
  const [visibleNodeTypes, setVisibleNodeTypes] = useState([]);
  const [visibleEdgeTypes, setVisibleEdgeTypes] = useState([]); // 可见的边类型
  const [visibleEdgeCategories, setVisibleEdgeCategories] = useState([]);
  const [dynamicEdgeColors, setDynamicEdgeColors] = useState({});
  const [isFilterExpanded, setIsFilterExpanded] = useState(false); // 控制筛选器是否展开
  const [isCanvasExpanded, setIsCanvasExpanded] = useState(false);
  const toast = useToast();
  const edgeTypeLabels = treeData.meta?.edgeTypeLabels || {};
  const edgeCategoryLabels = treeData.meta?.edgeCategoryLabels || {};

  const isLibraryNode = node =>
    node?.properties?.is_library === true ||
    node?.properties?.is_library === 'true'

  // 获取节点数据
  useEffect(() => {
    const fetchData = async () => {
      if (!methodData || !methodData.fullName) return;
      
      setLoading(true);
      try {
        const result = await getNodes(
          methodData.fullName, 
          methodData.queryType,
          methodData.pathDepth // 使用传入的路径深度
        );
        console.log("获取节点数据成功:", result);
        
        // 验证并确保数据结构正确
        if (result && result.nodes && Array.isArray(result.nodes)) {
          const normalizedGraph = normalizeGraphResult(result);
          const edgeTypes = [...new Set(normalizedGraph.edges.map(getEdgeType))];
          const edgeCategories = [...new Set(normalizedGraph.edges.map(getEdgeCategory))];
          const generatedColors = {};
          edgeTypes.forEach((edgeType, index) => {
            generatedColors[edgeType] = treeConfig.edgeColors[edgeType] || defaultEdgePalette[index % defaultEdgePalette.length];
          });
          const nodeTypes = [...new Set(normalizedGraph.nodes.map(getNodeType))];

          setDynamicEdgeColors(generatedColors);
          setVisibleNodeTypes(nodeTypes);
          setVisibleEdgeTypes(edgeTypes);
          setVisibleEdgeCategories(edgeCategories);
          
          setTreeData(normalizedGraph);
          
          // 重置变换状态，确保新树从初始状态开始
          transformRef.current = null;
          
          // 如果有缩放对象，重置视图
          if (svgRef.current && zoomRef.current) {
            const svgElement = d3.select(svgRef.current);
            const zoom = zoomRef.current;
            
            // 静默重置缩放和平移
            svgElement
              .call(zoom.transform, d3.zoomIdentity.translate(treeConfig.margin.left, treeConfig.margin.top));
          }
          
          // 重置选中节点
          setSelectedNodeId(null);
          
        } else {
          console.error("获取的节点数据格式不正确:", result);
          setTreeData({ nodes: [], edges: [], meta: {} });
        }
      } catch (error) {
        console.error("获取节点数据失败:", error);
        toast({
          title: '获取节点数据失败',
          description: error.message,
          status: 'error',
          duration: 5000,
          isClosable: true,
        });
        setTreeData({ nodes: [], edges: [], meta: {} });
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [methodData, toast]);

  const availableNodeTypes = [...new Set(treeData.nodes.map(getNodeType))];
  const availableEdgeTypes = Object.keys(dynamicEdgeColors);
  const availableEdgeCategories = [...new Set(treeData.edges.map(getEdgeCategory))];

  // 渲染树图
  useEffect(() => {
    if (!svgRef.current || loading || !treeData.nodes.length) return;

    // 清除之前的图形
    d3.select(svgRef.current).selectAll('*').remove();

    // 获取容器宽度 - 使用父容器的可见宽度
    const container = svgRef.current.parentNode;
    const containerWidth = container.clientWidth || treeConfig.width;
    
    // 设置树的布局
    // 构建层次化的树结构
    const buildHierarchy = (nodes, edges, rootNodeId) => {
      const nodeMap = new Map(nodes.map(node => [node.id, {...node, children: []}]));
      
      // 找到根节点
      let root = nodeMap.get(rootNodeId);
      if (!root) {
        // 如果找不到指定的根节点，使用第一个节点作为根节点
        root = nodeMap.get(nodes[0]?.id);
      }
      
      // 构建父子关系
      const visited = new Set();
      
      const buildTree = (currentNode, depth = 0, maxDepth = 50) => { // 大幅增加最大深度限制
        if (!currentNode || visited.has(currentNode.id) || depth > maxDepth) {
          return;
        }
        
        visited.add(currentNode.id);
        
        // 根据查询类型确定边的方向
        const relevantEdges = methodData.queryType === 'upstream' 
          ? edges.filter(edge => edge.target === currentNode.id)
          : edges.filter(edge => edge.source === currentNode.id);
        
        // 移除子节点数量限制，显示所有相关节点
        // 优先添加重要的节点（如DAO节点等）
        const sortedEdges = relevantEdges.sort((a, b) => {
          const aId = methodData.queryType === 'upstream' ? a.source : a.target;
          const bId = methodData.queryType === 'upstream' ? b.source : b.target;
          const aNode = nodeMap.get(aId);
          const bNode = nodeMap.get(bId);
          
          // 优先级：DAO节点 > 当前查询节点 > 其他
          const aPriority = (aNode?.properties?.dao_node ? 2 : 0) + (aNode?.properties?.source_node ? 1 : 0);
          const bPriority = (bNode?.properties?.dao_node ? 2 : 0) + (bNode?.properties?.source_node ? 1 : 0);
          
          return bPriority - aPriority;
        });
        
        // 显示所有相关节点，不做数量限制
        sortedEdges.forEach(edge => {
          const childId = methodData.queryType === 'upstream' ? edge.source : edge.target;
          const childNode = nodeMap.get(childId);
          
          if (childNode && !visited.has(childId)) {
            currentNode.children.push(childNode);
            buildTree(childNode, depth + 1, maxDepth);
          }
        });
      };
      
      buildTree(root);
      return root;
    };

    // 找到当前查询的方法作为根节点
    // 优先使用传入的节点ID，如果没有则根据full_name查找
    let rootNodeId = null;
    if (methodData.nodeId) {
      // 如果有指定的节点ID，优先使用
      rootNodeId = methodData.nodeId;
      console.log("使用指定的节点ID作为根节点:", rootNodeId);
    } else {
      // 否则根据full_name查找
      rootNodeId = treeData.nodes.find(node =>
        isMethodRootNode(node, methodData.fullName)
      )?.id || treeData.nodes[0]?.id;
      console.log("根据full_name查找到的根节点ID:", rootNodeId);
    }

    const filteredNodes = treeData.nodes.filter(node =>
      node.id === rootNodeId || visibleNodeTypes.includes(getNodeType(node))
    );
    const filteredNodeIds = new Set(filteredNodes.map(node => node.id));
    const isEdgeVisible = edge =>
      visibleEdgeTypes.includes(getEdgeType(edge)) &&
      visibleEdgeCategories.includes(getEdgeCategory(edge));
    const filteredEdges = treeData.edges.filter(edge =>
      filteredNodeIds.has(edge.source) &&
      filteredNodeIds.has(edge.target) &&
      isEdgeVisible(edge)
    );

    // 构建层次结构
    const hierarchyRoot = buildHierarchy(filteredNodes, filteredEdges, rootNodeId);
    
    if (!hierarchyRoot) {
      console.warn("无法构建层次结构");
      return;
    }

    // 计算层级数和节点数，动态调整布局参数
    const root = d3.hierarchy(hierarchyRoot);
    const totalNodes = root.descendants().length;
    const maxDepth = root.height;
    
    // 根据数据规模调整间距设置
    let nodeSpacing = 50;
    let levelSpacing = 180;
    
    if (totalNodes > 500) {
      nodeSpacing = 25;
      levelSpacing = 120;
    } else if (totalNodes > 200) {
      nodeSpacing = 30;
      levelSpacing = 140;
    } else if (totalNodes > 80) {
      nodeSpacing = 35;
      levelSpacing = 150;
    } else if (totalNodes > 40) {
      nodeSpacing = 40;
      levelSpacing = 160;
    }
    
    // 层级间距调整
    if (maxDepth > 10) {
      levelSpacing = Math.max(100, levelSpacing - (maxDepth - 10) * 5);
    } else if (maxDepth > 5) {
      levelSpacing = Math.max(120, levelSpacing - (maxDepth - 5) * 10);
    }

    // 准备画布，根据实际数据动态调整尺寸
    // 限制宽度不超过容器宽度
    const actualWidth = Math.max(
      containerWidth,
      treeConfig.margin.left + treeConfig.margin.right + Math.max(1, maxDepth) * levelSpacing + 240
    );
    const actualHeight = Math.max(treeConfig.height, totalNodes * nodeSpacing / 2 + 400);
    
    const svg = d3.select(svgRef.current)
      .attr('width', actualWidth)
      .attr('height', actualHeight)
      .attr('viewBox', `0 0 ${actualWidth} ${actualHeight}`)
      .attr('preserveAspectRatio', 'xMidYMid meet')
      .append('g')
      .attr('transform', `translate(${treeConfig.margin.left},${treeConfig.margin.top})`);

    // 定义箭头标记
    const defs = svg.append("defs");
    
    // 为每种边关系类型创建不同颜色的箭头
    Object.entries(dynamicEdgeColors).forEach(([edgeType, color]) => {
      // 正常方向的箭头（从左到右）
      defs.append("marker")
        .attr("id", `arrowhead-${edgeType}`)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 8) // 调整箭头位置，确保箭头尖端正好在路径终点
        .attr("refY", 0)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .attr("markerUnits", "userSpaceOnUse")
        .append("path")
        .attr("d", "M0,-3L6,0L0,3") // 调整箭头形状
        .attr("fill", color)
        .attr("stroke", "none");
      
      // 反向箭头（从右到左，用于上游查询）
      defs.append("marker")
        .attr("id", `arrowhead-reverse-${edgeType}`)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 8)
        .attr("refY", 0)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto-start-reverse") // 反向箭头
        .attr("markerUnits", "userSpaceOnUse")
        .append("path")
        .attr("d", "M0,-3L6,0L0,3")
        .attr("fill", color)
        .attr("stroke", "none");
    });
    
    // 默认箭头标记
    defs.append("marker")
      .attr("id", "arrowhead")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 8)
      .attr("refY", 0)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto")
      .attr("markerUnits", "userSpaceOnUse")
      .append("path")
      .attr("d", "M0,-3L6,0L0,3")
      .attr("fill", treeConfig.linkColor)
      .attr("stroke", "none");
    
    // 默认反向箭头标记
    defs.append("marker")
      .attr("id", "arrowhead-reverse")
      .attr("viewBox", "0 -5 10 10")
      .attr("refX", 8)
      .attr("refY", 0)
      .attr("markerWidth", 6)
      .attr("markerHeight", 6)
      .attr("orient", "auto-start-reverse")
      .attr("markerUnits", "userSpaceOnUse")
      .append("path")
      .attr("d", "M0,-3L6,0L0,3")
      .attr("fill", treeConfig.linkColor)
      .attr("stroke", "none");

    // 创建树布局
    const treeLayout = d3.tree()
      .size([actualHeight - treeConfig.margin.top - treeConfig.margin.bottom, actualWidth - treeConfig.margin.left - treeConfig.margin.right])
      .separation((a, b) => {
        // 动态间距：根据节点数量调整
        const baseSpacing = (a.parent === b.parent ? 1 : 1.2);
        return baseSpacing * nodeSpacing / 30; // 标准化间距
      });
    
    // 应用树布局
    treeLayout(root);

    // 调整坐标，使根节点在左侧（对于从左到右的布局）
    const descendants = root.descendants();
    descendants.forEach(d => {
      // 交换x和y坐标，实现从左到右的布局
      const temp = d.x;
      d.x = d.y;
      d.y = temp;
    });

    // 根据显示模式决定显示哪些边
    const getEdgesToDisplay = () => {
      const allEdges = [];
      const displayedNodeIds = new Set(descendants.map(d => d.data.id));
      
      if (edgeDisplayMode === 'none') {
        return []; // 不显示任何边
      }
      
      // 树形结构边（总是需要，除非是none模式）
      const links = root.links();
      const treeEdges = new Set();
      
      if (edgeDisplayMode !== 'none') {
        links.forEach(link => {
          const sourceId = link.source.data.id;
          const targetId = link.target.data.id;
          const edgeKey = `${sourceId}-${targetId}`;
          treeEdges.add(edgeKey);
          treeEdges.add(`${targetId}-${sourceId}`);
          
          const originalEdge = filteredEdges.find(e => 
            (e.source === sourceId && e.target === targetId) ||
            (e.source === targetId && e.target === sourceId)
          );
          
          const edgeType = getEdgeType(originalEdge || { type: 'calls' });
          allEdges.push({
            source: link.source,
            target: link.target,
            type: edgeType,
            category: getEdgeCategory(originalEdge),
            properties: originalEdge ? originalEdge.properties : { type: 'calls', category: 'SEMANTIC' },
            isTreeEdge: true,
            isHighlighted: false
          });
        });
      }
      
      if (edgeDisplayMode === 'tree') {
        return allEdges; // 只返回树形边
      }
      
      if (edgeDisplayMode === 'all') {
        // 显示所有边
        filteredEdges.forEach(edge => {
          if (displayedNodeIds.has(edge.source) && displayedNodeIds.has(edge.target)) {
            const edgeKey = `${edge.source}-${edge.target}`;
            const edgeType = getEdgeType(edge);
            if (!treeEdges.has(edgeKey)) {
              const sourceNode = descendants.find(d => d.data.id === edge.source);
              const targetNode = descendants.find(d => d.data.id === edge.target);
              
              if (sourceNode && targetNode) {
                  allEdges.push({
                    source: sourceNode,
                    target: targetNode,
                    type: edgeType,
                    category: getEdgeCategory(edge),
                    properties: edge.properties || { type: edgeType, category: getEdgeCategory(edge) },
                    isTreeEdge: false,
                    isHighlighted: false
                  });
              }
            }
          }
        });
      }
      
      if (edgeDisplayMode === 'dynamic') {
        // 只显示与选中节点相关的边
        if (selectedNodeId) {
          filteredEdges.forEach(edge => {
            if (displayedNodeIds.has(edge.source) && displayedNodeIds.has(edge.target)) {
              const isRelatedToSelected = edge.source === selectedNodeId || edge.target === selectedNodeId;
              const edgeKey = `${edge.source}-${edge.target}`;
              const edgeType = getEdgeType(edge);
              if (isRelatedToSelected && !treeEdges.has(edgeKey)) {
                const sourceNode = descendants.find(d => d.data.id === edge.source);
                const targetNode = descendants.find(d => d.data.id === edge.target);
                
                if (sourceNode && targetNode) {
                  allEdges.push({
                    source: sourceNode,
                    target: targetNode,
                    type: edgeType,
                    category: getEdgeCategory(edge),
                    properties: edge.properties || { type: edgeType, category: getEdgeCategory(edge) },
                    isTreeEdge: false,
                    isHighlighted: true
                  });
                }
              }
            }
          });
        }
      }
      
      return allEdges;
    };

    const edgesToDisplay = getEdgesToDisplay();
    console.log(`显示模式: ${edgeDisplayMode}, 显示 ${descendants.length} 个节点, ${edgesToDisplay.length} 条边`);

    // 添加连接线
    svg.selectAll('.link')
      .data(edgesToDisplay)
      .enter()
      .append('path')
      .attr('class', 'link')
      .attr('stroke', d => {
        if (d.isHighlighted) {
          // 高亮显示动态边
          return getEdgeType(d) ? 
            d3.color(getEdgeColor(getEdgeType(d), dynamicEdgeColors) || treeConfig.linkColor).brighter(0.5) :
            d3.color(treeConfig.linkColor).brighter(0.5);
        }
        if (getEdgeType(d)) {
          return getEdgeColor(getEdgeType(d), dynamicEdgeColors) || treeConfig.linkColor;
        }
        return treeConfig.linkColor;
      })
      .attr('stroke-width', d => {
        if (d.isHighlighted) return 3; // 高亮边更粗
        return d.isTreeEdge ? 1.5 : 1;
      })
      .attr('fill', 'none')
      .attr('stroke-opacity', d => {
        if (d.isHighlighted) return 1;
        return d.isTreeEdge ? 1 : 0.6;
      })
      .attr('marker-end', d => {
        // 不再需要反向箭头，因为我们已经交换了源节点和目标节点
        if (getEdgeType(d) && getEdgeColor(getEdgeType(d), dynamicEdgeColors)) {
          return `url(#arrowhead-${getEdgeType(d)})`;
        }
        return 'url(#arrowhead)';
      })
      .attr('d', d => {
        // 使用贝塞尔曲线连接，更加自然
        // 对于上游查询，交换源节点和目标节点的位置
        const isUpstream = methodData.queryType === 'upstream';
        
        // 如果是上游查询，交换源和目标
        let sourceNode = d.source;
        let targetNode = d.target;
        
        if (isUpstream) {
          sourceNode = d.target;
          targetNode = d.source;
        }
        
        const sourceX = sourceNode.x;
        const sourceY = sourceNode.y;
        let targetX = targetNode.x;
        let targetY = targetNode.y;
        
        // 检查是否存在反向边，如果存在则添加曲线偏移
        const reverseEdge = edgesToDisplay.find(edge => 
          edge.source.data.id === d.target.data.id && edge.target.data.id === d.source.data.id
        );
        
        if (reverseEdge && getEdgeType(reverseEdge) !== getEdgeType(d)) {
          // 存在不同类型的反向边，使用曲线路径避免重叠
          const midX = (sourceX + targetX) / 2;
          const midY = (sourceY + targetY) / 2;
          
          // 计算垂直于连线的偏移向量
          const dx = targetX - sourceX;
          const dy = targetY - sourceY;
          const dr = Math.sqrt(dx * dx + dy * dy);
          
          // 避免除零错误
          if (dr > 0) {
            const perpX = -dy / dr * 20; // 偏移距离
            const perpY = dx / dr * 20;
            
            // 计算曲线终点，确保箭头在节点边缘
            const curveEndX = targetX - (dx / dr) * (treeConfig.nodeRadius + 2);
            const curveEndY = targetY - (dy / dr) * (treeConfig.nodeRadius + 2);
            
            // 使用二次贝塞尔曲线
            return `M${sourceX},${sourceY}Q${midX + perpX},${midY + perpY} ${curveEndX},${curveEndY}`;
          }
        }
        
        // 计算直线终点，确保箭头在节点边缘
        const dx = targetX - sourceX;
        const dy = targetY - sourceY;
        const dr = Math.sqrt(dx * dx + dy * dy);
        
        if (dr > 0) {
          targetX = targetX - (dx / dr) * (treeConfig.nodeRadius + 2);
          targetY = targetY - (dy / dr) * (treeConfig.nodeRadius + 2);
        }
        
        // 使用标准贝塞尔曲线
        const controlX = sourceX + (targetX - sourceX) * 0.6;
        return `M${sourceX},${sourceY}C${controlX},${sourceY} ${controlX},${targetY} ${targetX},${targetY}`;
      });

    // 创建节点容器
    const nodeElements = svg.selectAll('.node')
      .data(descendants)
      .enter()
      .append('g')
      .attr('class', 'node')
      .attr('transform', d => `translate(${d.x},${d.y})`)
      .on('click', function(event, d) {
        // 防止事件冒泡
        event.stopPropagation();
        const nodeId = d.data.id;
        const nodeData = d.data;
        console.log("点击节点:", nodeId);
        
        // 如果是动态显示模式，更新选中节点并同时刷新详情
        if (edgeDisplayMode === 'dynamic') {
          // 总是刷新节点详情
          onNodeClick(nodeData);
          // 更新选中状态（如果点击的是已选中的节点，则取消选中；否则选中新节点）
          setSelectedNodeId(prevSelected => prevSelected === nodeId ? null : nodeId);
        } else {
          // 正常的节点详情查看
          onNodeClick(nodeData);
        }
      });

    // 添加节点圆形
    nodeElements.append('circle')
      .attr('r', treeConfig.nodeRadius)
      .attr('fill', d => {
        // 在动态模式下，检查是否是相关节点
        let isRelatedNode = false;
        if (edgeDisplayMode === 'dynamic' && selectedNodeId) {
          // 检查是否是与选中节点相关的节点
          isRelatedNode = edgesToDisplay.some(edge => 
            (edge.source.data.id === selectedNodeId && edge.target.data.id === d.data.id) ||
            (edge.target.data.id === selectedNodeId && edge.source.data.id === d.data.id)
          );
        }
        
        // 选中节点用红色高亮
        if (edgeDisplayMode === 'dynamic' && selectedNodeId === d.data.id) {
          return '#E53E3E'; // 选中节点用深红色高亮，避免与橙色冲突
        }
        
        // 相关节点用橙色强调
        if (edgeDisplayMode === 'dynamic' && isRelatedNode) {
          return '#9F7AEA'; // 相关节点用紫色强调，避免与橙色冲突
        }
        
        // 优先级顺序：1. 当前查询节点 2. 源节点 3. DAO节点 4. 库节点 5. 普通节点
        if (isMethodRootNode(d.data, methodData.fullName)) {
          return treeConfig.currentNodeColor; // 橙色 - 当前查询的方法节点
        } else if (isLibraryNode(d.data)) {
          return treeConfig.libraryNodeColor; // 灰色 - 库节点
        } else if (d.data.properties && d.data.properties.source_node) {
          return treeConfig.sourceNodeColor; // 黄色 - 源节点
        } else if (d.data.properties && d.data.properties.dao_node) {
          return treeConfig.daoNodeColor; // 绿色 - DAO节点
        }
        return treeConfig.nodeColor; // 蓝色 - 普通节点
      })
      .attr('stroke', d => {
        // 为选中节点和相关节点添加边框
        if (edgeDisplayMode === 'dynamic' && selectedNodeId) {
          if (selectedNodeId === d.data.id) {
            return '#FF0000'; // 选中节点红色边框
          }
          // 检查是否是相关节点
          const isRelatedNode = edgesToDisplay.some(edge => 
            (edge.source.data.id === selectedNodeId && edge.target.data.id === d.data.id) ||
            (edge.target.data.id === selectedNodeId && edge.source.data.id === d.data.id)
          );
          if (isRelatedNode) {
            return '#805AD5'; // 相关节点紫色边框
          }
        }
        return 'none';
      })
      .attr('stroke-width', d => {
        if (edgeDisplayMode === 'dynamic' && selectedNodeId) {
          if (selectedNodeId === d.data.id) {
            return 3; // 选中节点粗边框
          }
          // 检查是否是相关节点
          const isRelatedNode = edgesToDisplay.some(edge => 
            (edge.source.data.id === selectedNodeId && edge.target.data.id === d.data.id) ||
            (edge.target.data.id === selectedNodeId && edge.source.data.id === d.data.id)
          );
          if (isRelatedNode) {
            return 2; // 相关节点中等边框
          }
        }
        return 0;
      });

    // 添加方法名称标签
    nodeElements.append('text')
      .attr('dx', 12)
      .attr('dy', '.35em')
      .text(d => {
        const name = getNodeDisplayName(d.data);
        // 根据节点数量智能截断标签
        if (totalNodes > 500) {
          return name.length > 8 ? name.substring(0, 6) + '...' : name;
        } else if (totalNodes > 200) {
          return name.length > 10 ? name.substring(0, 8) + '...' : name;
        } else if (totalNodes > 50) {
          return name.length > 12 ? name.substring(0, 9) + '...' : name;
        } else if (totalNodes > 30) {
          return name.length > 18 ? name.substring(0, 15) + '...' : name;
        }
        return name; // 节点少时显示完整名称
      })
      .attr('font-size', totalNodes > 500 ? '6px' : (totalNodes > 200 ? '7px' : (totalNodes > 50 ? '8px' : '9px'))) // 根据节点数调整字体大小
      .attr('font-family', 'sans-serif')
      .attr('opacity', d => {
        // 根据节点重要性和显示设置决定默认透明度
        if (showLabels) return 1;
        if (d.data.properties?.source_node || d.data.properties?.dao_node || d.depth <= 1) {
          return 0.8; // 重要节点默认显示标签
        }
        return 0;
      })
      .attr('fill', '#333')
      .attr('pointer-events', 'none'); // 防止标签阻挡鼠标事件

    // 添加关联边数量标识（小数字）- 根据图谱大小智能显示
    if (totalNodes > 50 || treeData.edges.length > 100) {
      nodeElements.append('text')
        .attr('dx', -8)
        .attr('dy', -8)
        .text(d => {
          // 计算当前节点的关联边数量
          const nodeId = d.data.id;
          const relatedEdgesCount = treeData.edges.filter(edge => 
            edge.source === nodeId || edge.target === nodeId
          ).length;
          return relatedEdgesCount > 0 ? relatedEdgesCount : '';
        })
        .attr('font-size', totalNodes > 200 ? '6px' : '7px') // 节点很多时字体更小
        .attr('font-family', 'sans-serif')
        .attr('fill', '#666')
        .attr('opacity', 0.7)
        .attr('pointer-events', 'none');
    }

    // 添加悬停效果
    nodeElements.on('mouseover', function(event, d) {
      // 显示节点名称
      if (!showLabels) {
        d3.select(this).select('text')
          .transition()
          .duration(200)
          .attr('opacity', 1);
      }
      
      // 高亮节点
      d3.select(this).select('circle')
        .transition()
        .duration(200)
        .attr('r', treeConfig.nodeRadius * 1.5);
    })
    .on('mouseout', function(event, d) {
      // 隐藏节点名称
      if (!showLabels) {
        d3.select(this).select('text')
          .transition()
          .duration(200)
          .attr('opacity', 0);
      }
      
      // 恢复节点大小
      d3.select(this).select('circle')
        .transition()
        .duration(200)
        .attr('r', treeConfig.nodeRadius);
    });

    // 添加缩放功能
    const zoom = d3.zoom()
      .scaleExtent([0.3, 5])
      .on('zoom', (event) => {
        // 保存当前变换状态
        transformRef.current = event.transform;
        svg.attr('transform', event.transform);
      });

    // 保存zoom对象的引用，以便后续使用
    zoomRef.current = zoom;

    // 初始缩放适应画布大小
    const svgElement = d3.select(svgRef.current);
    svgElement.call(zoom);
    
    // 如果有保存的变换状态，静默恢复它
    if (transformRef.current) {
      // 直接设置变换，不触发事件
      svg.attr('transform', transformRef.current);
      // 静默更新zoom的内部状态，避免触发zoom事件
      zoom.transform(svgElement, transformRef.current);
    }

  }, [treeData, loading, methodData, onNodeClick, showLabels, edgeDisplayMode, selectedNodeId, visibleNodeTypes, visibleEdgeTypes, visibleEdgeCategories, dynamicEdgeColors, isCanvasExpanded]);

  // 处理标签显示开关变化
  const handleToggleLabels = () => {
    setShowLabels(!showLabels);
  };

  const toggleFilterValue = (setter, value) => {
    setter(prev => {
      if (prev.includes(value)) {
        return prev.filter(item => item !== value);
      }
      return [...prev, value];
    });
  };

  const handleNodeTypeFilterChange = nodeType => toggleFilterValue(setVisibleNodeTypes, nodeType);

  const handleEdgeTypeFilterChange = edgeType => toggleFilterValue(setVisibleEdgeTypes, edgeType);

  const handleEdgeCategoryFilterChange = category => toggleFilterValue(setVisibleEdgeCategories, category);

  const handleSelectAllFilters = (setter, values, selectAll) => {
    setter(selectAll ? values : []);
  };

  // 切换筛选器展开/收起状态
  const toggleFilterExpanded = () => {
    setIsFilterExpanded(!isFilterExpanded);
  };

  // 重置视图，居中图谱并恢复缩放
  const resetView = () => {
    if (!svgRef.current || !zoomRef.current) return;
    
    const svgElement = d3.select(svgRef.current);
    const zoom = zoomRef.current;
    
    // 创建恢复到初始状态的变换
    svgElement
      .transition()
      .duration(750)
      .call(zoom.transform, d3.zoomIdentity.translate(treeConfig.margin.left, treeConfig.margin.top));
    
    // 更新保存的变换状态
    transformRef.current = d3.zoomIdentity.translate(treeConfig.margin.left, treeConfig.margin.top);
    
    toast({
      title: "视图已重置",
      status: "success",
      duration: 2000,
      isClosable: true,
      position: "top"
    });
  };

  const toggleCanvasExpanded = () => {
    setIsCanvasExpanded(prev => !prev);
  };

  return (
    <Box p={5} shadow="md" borderWidth="1px" borderRadius="md" bg="white" width="100%" overflowX="hidden">
      <HStack justifyContent="space-between" mb={4} flexWrap="wrap">
        <Heading size="md">方法关系树</Heading>
        <HStack spacing={4} flexWrap="wrap">
          <FormControl display="flex" alignItems="center" width="auto">
            <FormLabel htmlFor="label-toggle" mb="0" fontSize="sm">
              显示所有标签
            </FormLabel>
            <Switch id="label-toggle" isChecked={showLabels} onChange={handleToggleLabels} />
          </FormControl>
          
          <FormControl display="flex" alignItems="center" width="auto">
            <FormLabel mb="0" fontSize="sm" mr={2}>
              边显示模式:
            </FormLabel>
            <HStack spacing={1}>
              <select 
                value={edgeDisplayMode} 
                onChange={(e) => {
                  setEdgeDisplayMode(e.target.value);
                  setSelectedNodeId(null); // 切换模式时清除选中状态
                }}
                style={{
                  fontSize: '12px',
                  padding: '4px 8px',
                  borderRadius: '4px',
                  border: '1px solid #E2E8F0',
                  backgroundColor: 'white'
                }}
              >
                <option value="none">不显示边</option>
                <option value="tree">仅树形结构</option>
                <option value="all">显示所有边</option>
                <option value="dynamic">动态显示</option>
              </select>
              
              <Tooltip 
                label="不显示边：只看节点分布；仅树形结构：显示层次关系；显示所有边：完整关系图；动态显示：点击节点查看相关边"
                fontSize="xs"
                placement="top"
                hasArrow
              >
                <InfoIcon 
                  w={3} 
                  h={3} 
                  color="gray.400" 
                  cursor="help"
                  _hover={{ color: "gray.600" }}
                />
              </Tooltip>
            </HStack>
          </FormControl>
        </HStack>
      </HStack>
      
      {loading ? (
        <Box textAlign="center" py={10}>
          <Spinner size="xl" />
          <Text mt={3}>加载节点数据...</Text>
        </Box>
      ) : treeData.nodes.length === 0 ? (
        <Box textAlign="center" py={10}>
          <Text>没有找到相关的节点数据</Text>
        </Box>
      ) : (
        <Box>
          <Text mb={2} fontSize="sm">
            {methodData.queryType === 'self' && '当前节点'}
            {methodData.queryType === 'upstream' && '上游节点'}
            {methodData.queryType === 'downstream' && '下游节点树'}
            : {methodData.fullName}
          </Text>
          
          {/* 添加统计信息 */}
          <Text mb={2} fontSize="xs" color="gray.600">
            {treeData.nodes.length} 个节点，{treeData.edges.length} 条边关系
          </Text>
          
          {/* 添加操作提示 */}
          <Text mb={3} fontSize="xs" color="gray.500">
            提示: 点击节点查看详情，滚轮缩放。树状结构化展示。节点左上角数字表示关联边数量。
            {edgeDisplayMode === 'dynamic' && selectedNodeId && ' 当前显示选中节点的相关边。'}
            {edgeDisplayMode === 'dynamic' && !selectedNodeId && ' 动态模式：点击节点显示其相关边。'}
          </Text>
          
          {/* 通用筛选器 */}
          <Box mb={3} p={2} bg="gray.50" borderRadius="md">
            <HStack justifyContent="space-between" mb={1} cursor="pointer" onClick={toggleFilterExpanded}>
              <HStack>
                <Text fontSize="xs" fontWeight="bold">图谱筛选:</Text>
                <Text fontSize="xs" color="gray.500">
                  {isFilterExpanded ? '点击收起' : '点击展开'} 
                  （节点类型 {visibleNodeTypes.length}/{availableNodeTypes.length}，边类型 {visibleEdgeTypes.length}/{availableEdgeTypes.length}，边分类 {visibleEdgeCategories.length}/{availableEdgeCategories.length}）
                </Text>
              </HStack>
              <Icon 
                as={isFilterExpanded ? ChevronUpIcon : ChevronDownIcon} 
                w={4} 
                h={4} 
                color="gray.500"
              />
            </HStack>
            
            {isFilterExpanded && (
              <>
                <Box mb={3}>
                  <HStack justifyContent="space-between" mb={1}>
                    <Text fontSize="xs" fontWeight="bold">节点类型</Text>
                    <HStack>
                      <Button
                        size="xs"
                        colorScheme="blue"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleNodeTypes, availableNodeTypes, true);
                        }}
                      >
                        全选
                      </Button>
                      <Button
                        size="xs"
                        colorScheme="gray"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleNodeTypes, availableNodeTypes, false);
                        }}
                      >
                        全不选
                      </Button>
                    </HStack>
                  </HStack>
                  <Wrap spacing={2}>
                    {availableNodeTypes.map(nodeType => (
                      <WrapItem key={nodeType}>
                        <Checkbox
                          isChecked={visibleNodeTypes.includes(nodeType)}
                          onChange={e => {
                            e.stopPropagation();
                            handleNodeTypeFilterChange(nodeType);
                          }}
                          size="sm"
                          colorScheme="blue"
                        >
                          <Text fontSize="xs">{getNodeTypeLabel(nodeType)}</Text>
                        </Checkbox>
                      </WrapItem>
                    ))}
                  </Wrap>
                </Box>

                <Box mb={3}>
                  <HStack justifyContent="space-between" mb={1}>
                    <Text fontSize="xs" fontWeight="bold">边类型</Text>
                    <HStack>
                      <Button
                        size="xs"
                        colorScheme="blue"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleEdgeTypes, availableEdgeTypes, true);
                        }}
                      >
                        全选
                      </Button>
                      <Button
                        size="xs"
                        colorScheme="gray"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleEdgeTypes, availableEdgeTypes, false);
                        }}
                      >
                        全不选
                      </Button>
                    </HStack>
                  </HStack>
                  <Wrap spacing={2}>
                    {Object.entries(dynamicEdgeColors).map(([edgeType, color]) => (
                      <WrapItem key={edgeType}>
                        <Checkbox 
                          isChecked={visibleEdgeTypes.includes(edgeType)}
                          onChange={e => {
                            e.stopPropagation();
                            handleEdgeTypeFilterChange(edgeType);
                          }}
                          size="sm"
                          colorScheme={edgeType === 'calls' ? 'blue' : 
                                      edgeType === 'implemented_by' ? 'green' : 
                                      edgeType === 'overridden_by' ? 'red' : 
                                      edgeType === 'super_calls' ? 'purple' :
                                      edgeType === 'interface_calls' ? 'orange' :
                                      edgeType === 'subtype_calls' ? 'cyan' :
                                      edgeType === 'injection_calls' ? 'pink' : 'gray'}
                        >
                          <HStack spacing={1} alignItems="center">
                            <Box 
                              w="12px" 
                              h="2px" 
                              bg={color} 
                              position="relative"
                            >
                              <Box 
                                position="absolute" 
                                right="-3px" 
                                top="-2px" 
                                borderLeft="4px solid" 
                                borderLeftColor={color}
                                borderTop="2px solid transparent"
                                borderBottom="2px solid transparent"
                              />
                            </Box>
                            <Text fontSize="xs">
                              {getEdgeLabel(edgeType, edgeTypeLabels)}
                            </Text>
                          </HStack>
                        </Checkbox>
                      </WrapItem>
                    ))}
                  </Wrap>
                </Box>

                <Box>
                  <HStack justifyContent="space-between" mb={1}>
                    <Text fontSize="xs" fontWeight="bold">边分类</Text>
                    <HStack>
                      <Button
                        size="xs"
                        colorScheme="blue"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleEdgeCategories, availableEdgeCategories, true);
                        }}
                      >
                        全选
                      </Button>
                      <Button
                        size="xs"
                        colorScheme="gray"
                        variant="outline"
                        onClick={e => {
                          e.stopPropagation();
                          handleSelectAllFilters(setVisibleEdgeCategories, availableEdgeCategories, false);
                        }}
                      >
                        全不选
                      </Button>
                    </HStack>
                  </HStack>
                  <Wrap spacing={2}>
                    {availableEdgeCategories.map(category => (
                      <WrapItem key={category}>
                        <Checkbox
                          isChecked={visibleEdgeCategories.includes(category)}
                          onChange={e => {
                            e.stopPropagation();
                            handleEdgeCategoryFilterChange(category);
                          }}
                          size="sm"
                          colorScheme="purple"
                        >
                          <Text fontSize="xs">{getCategoryLabel(category, edgeCategoryLabels)}</Text>
                        </Checkbox>
                      </WrapItem>
                    ))}
                  </Wrap>
                </Box>
              </>
            )}
          </Box>
          
          {/* 节点图例 */}
          <HStack spacing={4} mb={2} px={2} py={1} bg="gray.50" borderRadius="md" fontSize="xs" wrap="wrap">
            <HStack>
              <Box w="10px" h="10px" borderRadius="full" bg={treeConfig.nodeColor} />
              <Text>普通方法</Text>
            </HStack>
            
            <HStack>
              <Box w="10px" h="10px" borderRadius="full" bg={treeConfig.daoNodeColor} />
              <Text>DAO方法</Text>
            </HStack>
            
            <HStack>
              <Box w="10px" h="10px" borderRadius="full" bg={treeConfig.currentNodeColor} />
              <Text>当前查询方法</Text>
            </HStack>
            
            <HStack>
              <Box w="10px" h="10px" borderRadius="full" bg={treeConfig.libraryNodeColor} />
              <Text>库方法</Text>
            </HStack>
            
            {edgeDisplayMode === 'dynamic' && (
              <>
                <Divider orientation="vertical" height="20px" />
                <HStack>
                  <Box w="10px" h="10px" borderRadius="full" bg="#E53E3E" border="2px solid #FF0000" />
                  <Text>选中节点</Text>
                </HStack>
                
                <HStack>
                  <Box w="10px" h="10px" borderRadius="full" bg="#9F7AEA" border="2px solid #805AD5" />
                  <Text>相关节点</Text>
                </HStack>
              </>
            )}
          </HStack>
          
          {isCanvasExpanded && (
            <Box
              position="fixed"
              inset={0}
              bg="blackAlpha.600"
              zIndex={1200}
              onClick={toggleCanvasExpanded}
            />
          )}

          <Box 
            border="1px" 
            borderColor="gray.200" 
            borderRadius="md" 
            overflow="hidden"
            position={isCanvasExpanded ? "fixed" : "relative"}
            top={isCanvasExpanded ? "50%" : "auto"}
            left={isCanvasExpanded ? "50%" : "auto"}
            transform={isCanvasExpanded ? "translate(-50%, -50%)" : "none"}
            zIndex={isCanvasExpanded ? 1300 : "auto"}
            bg="white"
            boxShadow={isCanvasExpanded ? "0 28px 80px rgba(15, 23, 42, 0.35)" : "none"}
            width={isCanvasExpanded ? "min(92vw, 1440px)" : "100%"}
            maxWidth={isCanvasExpanded ? "92vw" : "100%"}
          >
            <IconButton
              aria-label="重置视图"
              icon={<Icon viewBox="0 0 24 24" boxSize={5}>
                <path
                  fill="currentColor"
                  d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"
                />
              </Icon>}
              colorScheme="blue"
              size="sm"
              position="absolute"
              top={2}
              left={2}
              zIndex={10}
              onClick={resetView}
              title="重置视图"
              _hover={{
                bg: "blue.500",
                color: "white"
              }}
              boxShadow="md"
            />

            <Box
              position="relative"
              role="group"
            >
              <IconButton
                aria-label={isCanvasExpanded ? "退出放大" : "放大画布"}
                icon={isCanvasExpanded ? <CloseIcon boxSize={3} /> : <SearchIcon boxSize={4} />}
                size="sm"
                position="absolute"
                top={2}
                right={2}
                zIndex={10}
                onClick={toggleCanvasExpanded}
                colorScheme="gray"
                variant="solid"
                opacity={isCanvasExpanded ? 1 : 0}
                pointerEvents={isCanvasExpanded ? "auto" : "none"}
                _groupHover={{
                  opacity: 1,
                  pointerEvents: "auto"
                }}
                boxShadow="md"
              />

              <Box 
                width="100%" 
                height={isCanvasExpanded ? "85vh" : "600px"} 
                overflow="hidden"
                cursor="grab"
                sx={{
                  '&:active': {
                    cursor: 'grabbing'
                  }
                }}
              >
                <svg ref={svgRef}></svg>
              </Box>
            </Box>
          </Box>
        </Box>
      )}
    </Box>
  );
};

export default NodeTree; 
