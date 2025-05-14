import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  VStack,
  Text,
  Button,
  Badge,
  useToast,
  Skeleton,
  Alert,
  AlertIcon,
  AlertTitle,
  AlertDescription,
  Collapse,
  Icon,
} from '@chakra-ui/react';
import { ChevronDownIcon, ChevronRightIcon } from '@chakra-ui/icons';
import { getAggregatedDocumentations, getProcessDocumentations } from '../../api/documentationApi';

const AggregatedDocList = ({
  projectId,
  selectedAggregatedDocId,
  onAggregatedDocSelect,
  isExpanded = true,
  onToggleExpanded,
  onProcessDocSelect
}) => {
  const [aggregatedDocs, setAggregatedDocs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [expandedItems, setExpandedItems] = useState(new Set());
  const [processDocs, setProcessDocs] = useState({});
  const [loadingProcessDocs, setLoadingProcessDocs] = useState({});
  const toast = useToast();

  // 加载聚合说明书列表
  const loadAggregatedDocs = useCallback(async (projectId) => {
    if (!projectId) {
      setAggregatedDocs([]);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getAggregatedDocumentations(projectId);
      setAggregatedDocs(data || []);
    } catch (err) {
      console.error('加载聚合说明书列表失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载聚合说明书列表: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // 加载流程说明书列表
  const loadProcessDocs = useCallback(async (aggregatedDocId) => {
    if (!projectId || !aggregatedDocId) {
      return;
    }

    try {
      setLoadingProcessDocs(prev => ({ ...prev, [aggregatedDocId]: true }));

      const data = await getProcessDocumentations(aggregatedDocId);
      setProcessDocs(prev => ({ ...prev, [aggregatedDocId]: data || [] }));
    } catch (error) {
      console.error('加载流程说明书失败:', error);

      toast({
        title: '加载失败',
        description: error.message || '无法加载流程说明书列表',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setLoadingProcessDocs(prev => ({ ...prev, [aggregatedDocId]: false }));
    }
  }, [projectId, toast]);

  useEffect(() => {
    loadAggregatedDocs(projectId);
  }, [projectId, loadAggregatedDocs]);

  // 处理聚合说明书选择
  const handleAggregatedDocClick = (doc) => {
    if (onAggregatedDocSelect) {
      onAggregatedDocSelect(doc);
    }
  };

  // 处理聚合说明书展开/折叠
  const handleAggregatedDocToggle = (docId) => {
    const newExpandedItems = new Set(expandedItems);
    if (newExpandedItems.has(docId)) {
      newExpandedItems.delete(docId);
    } else {
      newExpandedItems.add(docId);
      // 展开时加载流程说明书
      if (!processDocs[docId]) {
        loadProcessDocs(docId);
      }
    }
    setExpandedItems(newExpandedItems);
  };

  // 处理流程说明书选择
  const handleProcessDocClick = (processDoc) => {
    if (onProcessDocSelect) {
      onProcessDocSelect(processDoc);
    }
  };

  // 重试加载
  const handleRetry = () => {
    loadAggregatedDocs(projectId);
  };

  // 切换展开状态
  const handleToggle = () => {
    if (onToggleExpanded) {
      onToggleExpanded();
    }
  };

  if (!projectId) {
    return null;
  }

  const headerContent = (
    <Button
      variant="ghost"
      size="sm"
      justifyContent="flex-start"
      leftIcon={
        <Icon as={isExpanded ? ChevronDownIcon : ChevronRightIcon} />
      }
      onClick={handleToggle}
      width="100%"
      fontWeight="bold"
      color="gray.600"
      _hover={{ bg: "gray.100" }}
    >
      聚合说明书 {aggregatedDocs.length > 0 && `(${aggregatedDocs.length})`}
    </Button>
  );

  if (loading) {
    return (
      <Box>
        {headerContent}
        <Collapse in={isExpanded}>
          <Box pl={4} mt={2}>
            <VStack spacing={2} align="stretch">
              {[1, 2].map((i) => (
                <Skeleton key={i} height="50px" borderRadius="md" />
              ))}
            </VStack>
          </Box>
        </Collapse>
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        {headerContent}
        <Collapse in={isExpanded}>
          <Box pl={4} mt={2}>
            <Alert status="error" size="sm" borderRadius="md">
              <AlertIcon />
              <Box>
                <AlertTitle fontSize="sm">加载失败</AlertTitle>
                <AlertDescription fontSize="xs">
                  {error}
                </AlertDescription>
              </Box>
            </Alert>
            <Button size="sm" mt={2} onClick={handleRetry} colorScheme="blue" variant="outline">
              重试
            </Button>
          </Box>
        </Collapse>
      </Box>
    );
  }

  if (aggregatedDocs.length === 0) {
    return (
      <Box>
        {headerContent}
        <Collapse in={isExpanded}>
          <Box pl={4} mt={2}>
            <Alert status="info" size="sm" borderRadius="md">
              <AlertIcon />
              <Box>
                <AlertTitle fontSize="sm">暂无文档</AlertTitle>
                <AlertDescription fontSize="xs">
                  该项目下还没有聚合说明书
                </AlertDescription>
              </Box>
            </Alert>
          </Box>
        </Collapse>
      </Box>
    );
  }

  return (
    <Box>
      {headerContent}
      <Collapse in={isExpanded}>
        <Box pl={4} mt={2}>
          <VStack spacing={2} align="stretch">
            {aggregatedDocs.map((doc) => (
              <Box key={doc.id}>
                {/* 聚合说明书主体 */}
                <Box display="flex" alignItems="flex-start">
                  {/* 展开/折叠按钮 */}
                  <Button
                    variant="ghost"
                    size="xs"
                    minW="auto"
                    h="auto"
                    p={1}
                    mr={2}
                    mt={1}
                    onClick={() => handleAggregatedDocToggle(doc.id)}
                    _hover={{ bg: "gray.100" }}
                  >
                    <Icon
                      as={expandedItems.has(doc.id) ? ChevronDownIcon : ChevronRightIcon}
                      boxSize={3}
                    />
                  </Button>

                  {/* 聚合说明书内容 */}
                  <Button
                    variant={selectedAggregatedDocId === doc.id ? "solid" : "ghost"}
                    colorScheme={selectedAggregatedDocId === doc.id ? "blue" : "gray"}
                    size="sm"
                    justifyContent="flex-start"
                    textAlign="left"
                    height="auto"
                    py={3}
                    px={3}
                    flex={1}
                    onClick={() => handleAggregatedDocClick(doc)}
                    _hover={{
                      bg: selectedAggregatedDocId === doc.id ? undefined : "gray.100"
                    }}
                  >
                    <Box width="100%">
                      <Text fontSize="sm" fontWeight="medium" noOfLines={2}>
                        {doc.title}
                      </Text>
                      {doc.summary && (
                        <Text fontSize="xs" color="gray.500" noOfLines={2} mt={1}>
                          {doc.summary}
                        </Text>
                      )}
                      <Box mt={1} display="flex" gap={1} flexWrap="wrap">
                        {doc.aggregationType && (
                          <Badge size="sm" colorScheme="purple" variant="subtle">
                            {doc.aggregationType}
                          </Badge>
                        )}
                        {doc.branchName && (
                          <Badge size="sm" colorScheme="gray" variant="subtle">
                            {doc.branchName}
                          </Badge>
                        )}
                      </Box>
                    </Box>
                  </Button>
                </Box>

                {/* 流程说明书列表 */}
                <Collapse in={expandedItems.has(doc.id)}>
                  <Box ml={6} mt={2} pl={4} borderLeft="2px" borderColor="gray.200">
                    {loadingProcessDocs[doc.id] ? (
                      <VStack spacing={1} align="stretch">
                        {[1, 2].map((i) => (
                          <Skeleton key={i} height="40px" borderRadius="md" />
                        ))}
                      </VStack>
                    ) : processDocs[doc.id] && processDocs[doc.id].length > 0 ? (
                      <VStack spacing={1} align="stretch">
                        {processDocs[doc.id].map((processDoc) => (
                          <Button
                            key={processDoc.id}
                            variant="ghost"
                            size="sm"
                            justifyContent="flex-start"
                            textAlign="left"
                            height="auto"
                            py={2}
                            px={3}
                            onClick={() => handleProcessDocClick(processDoc)}
                            _hover={{ bg: "gray.50" }}
                          >
                            <Box width="100%">
                              <Text fontSize="xs" fontWeight="medium" noOfLines={1}>
                                {processDoc.title}
                              </Text>
                              {processDoc.summary && (
                                <Text fontSize="xs" color="gray.500" noOfLines={1} mt={0.5}>
                                  {processDoc.summary}
                                </Text>
                              )}
                              <Box mt={1} display="flex" gap={1} flexWrap="wrap">
                                {processDoc.entryPoint && (
                                  <Badge size="xs" colorScheme="green" variant="subtle">
                                    入口: {processDoc.entryPoint}
                                  </Badge>
                                )}
                                {processDoc.branchName && (
                                  <Badge size="xs" colorScheme="gray" variant="subtle">
                                    {processDoc.branchName}
                                  </Badge>
                                )}
                              </Box>
                            </Box>
                          </Button>
                        ))}
                      </VStack>
                    ) : (
                      <Text fontSize="xs" color="gray.400" py={2}>
                        暂无流程说明书
                      </Text>
                    )}
                  </Box>
                </Collapse>
              </Box>
            ))}
          </VStack>
        </Box>
      </Collapse>
    </Box>
  );
};

export default AggregatedDocList;
