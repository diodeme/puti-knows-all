import React, { useCallback, useEffect, useState } from 'react';
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
import { getProcessDocumentations } from '../../api/documentationApi';

const ProcessDocList = ({ 
  aggregatedDocId, 
  selectedProcessDocId, 
  onProcessDocSelect,
  isExpanded = true,
  onToggleExpanded 
}) => {
  const [processDocs, setProcessDocs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const toast = useToast();

  // 加载流程说明书列表
  const loadProcessDocs = useCallback(async (currentAggregatedDocId) => {
    if (!currentAggregatedDocId) {
      setProcessDocs([]);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getProcessDocumentations(currentAggregatedDocId);
      setProcessDocs(data || []);
    } catch (err) {
      console.error('加载流程说明书列表失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载流程说明书列表: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadProcessDocs(aggregatedDocId);
  }, [aggregatedDocId, loadProcessDocs]);

  // 处理流程说明书选择
  const handleProcessDocClick = (doc) => {
    if (onProcessDocSelect) {
      onProcessDocSelect(doc);
    }
  };

  // 重试加载
  const handleRetry = () => {
    loadProcessDocs(aggregatedDocId);
  };

  // 切换展开状态
  const handleToggle = () => {
    if (onToggleExpanded) {
      onToggleExpanded();
    }
  };

  if (!aggregatedDocId) {
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
      流程说明书 {processDocs.length > 0 && `(${processDocs.length})`}
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

  if (processDocs.length === 0) {
    return (
      <Box>
        {headerContent}
        <Collapse in={isExpanded}>
          <Box pl={4} mt={2}>
            <Alert status="info" size="sm" borderRadius="md">
              <AlertIcon />
              <Box>
                <AlertTitle fontSize="sm">暂无流程</AlertTitle>
                <AlertDescription fontSize="xs">
                  该聚合说明书下还没有流程说明书
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
            {processDocs.map((doc) => (
              <Button
                key={doc.id}
                variant={selectedProcessDocId === doc.id ? "solid" : "ghost"}
                colorScheme={selectedProcessDocId === doc.id ? "blue" : "gray"}
                size="sm"
                justifyContent="flex-start"
                textAlign="left"
                height="auto"
                py={3}
                px={3}
                onClick={() => handleProcessDocClick(doc)}
                _hover={{
                  bg: selectedProcessDocId === doc.id ? undefined : "gray.100"
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
                    {doc.entryPointName && (
                      <Badge size="sm" colorScheme="green" variant="subtle">
                        入口: {doc.entryPointName}
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
            ))}
          </VStack>
        </Box>
      </Collapse>
    </Box>
  );
};

export default ProcessDocList;
