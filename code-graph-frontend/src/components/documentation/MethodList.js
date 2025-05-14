import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  VStack,
  HStack,
  Text,
  Button,
  Badge,
  useToast,
  Skeleton,
  Alert,
  AlertIcon,
  AlertTitle,
  AlertDescription,
  Input,
  InputGroup,
  InputLeftElement,
  Icon,
  Flex,
  Spacer,
} from '@chakra-ui/react';
import { SearchIcon, ChevronLeftIcon, ChevronRightIcon } from '@chakra-ui/icons';
import { getProcessDocumentationMethods } from '../../api/documentationApi';

const MethodList = ({ processDocId, onMethodSelect }) => {
  const [methods, setMethods] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [pagination, setPagination] = useState({
    page: 1,
    size: 20,
    total: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const toast = useToast();

  // 加载方法列表
  const loadMethods = useCallback(async (processDocId, page = 1, size = 20) => {
    if (!processDocId) {
      setMethods([]);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getProcessDocumentationMethods(processDocId, page, size);
      
      if (data && data.data) {
        setMethods(data.data);
        setPagination({
          page: data.page || 1,
          size: data.size || 20,
          total: data.total || 0,
          totalPages: data.totalPages || 0,
          hasNext: data.hasNext || false,
          hasPrevious: data.hasPrevious || false,
        });
      } else {
        setMethods([]);
        setPagination({
          page: 1,
          size: 20,
          total: 0,
          totalPages: 0,
          hasNext: false,
          hasPrevious: false,
        });
      }
    } catch (err) {
      console.error('加载方法列表失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载方法列表: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // 当processDocId改变时重新加载
  useEffect(() => {
    loadMethods(processDocId, 1, 20);
    setSearchTerm('');
  }, [processDocId, loadMethods]);

  // 处理方法选择
  const handleMethodClick = (methodId) => {
    if (onMethodSelect) {
      onMethodSelect(methodId);
    }
  };

  // 处理分页
  const handlePageChange = (newPage) => {
    loadMethods(processDocId, newPage, pagination.size);
  };

  // 重试加载
  const handleRetry = () => {
    loadMethods(processDocId, pagination.page, pagination.size);
  };

  // 过滤方法列表
  const filteredMethods = methods.filter(methodId =>
    methodId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!processDocId) {
    return (
      <Box
        height="100%"
        display="flex"
        alignItems="center"
        justifyContent="center"
        color="gray.500"
      >
        请先选择一个流程说明书
      </Box>
    );
  }

  return (
    <Box height="100%" display="flex" flexDirection="column">
      {/* 搜索框 */}
      <Box mb={4}>
        <InputGroup>
          <InputLeftElement pointerEvents="none">
            <Icon as={SearchIcon} color="gray.400" />
          </InputLeftElement>
          <Input
            placeholder="搜索方法..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            size="sm"
          />
        </InputGroup>
      </Box>

      {/* 统计信息 */}
      <HStack mb={4} justify="space-between">
        <Text fontSize="sm" color="gray.600">
          共 {pagination.total} 个方法
          {searchTerm && ` (筛选出 ${filteredMethods.length} 个)`}
        </Text>
        <Badge colorScheme="blue" variant="subtle">
          第 {pagination.page} / {pagination.totalPages} 页
        </Badge>
      </HStack>

      {/* 方法列表 */}
      <Box flex={1} overflow="auto">
        {loading ? (
          <VStack spacing={2} align="stretch">
            {[1, 2, 3, 4, 5].map((i) => (
              <Skeleton key={i} height="40px" borderRadius="md" />
            ))}
          </VStack>
        ) : error ? (
          <Alert status="error">
            <AlertIcon />
            <Box>
              <AlertTitle fontSize="sm">加载失败</AlertTitle>
              <AlertDescription fontSize="xs">
                {error}
              </AlertDescription>
            </Box>
          </Alert>
        ) : filteredMethods.length === 0 ? (
          <Box
            height="100%"
            display="flex"
            alignItems="center"
            justifyContent="center"
            color="gray.500"
          >
            {searchTerm ? '没有找到匹配的方法' : '该流程说明书下暂无方法'}
          </Box>
        ) : (
          <VStack spacing={2} align="stretch">
            {filteredMethods.map((methodId, index) => (
              <Button
                key={`${methodId}-${index}`}
                variant="ghost"
                size="sm"
                justifyContent="flex-start"
                textAlign="left"
                height="auto"
                py={3}
                px={3}
                onClick={() => handleMethodClick(methodId)}
                _hover={{ bg: "gray.100" }}
              >
                <Box width="100%">
                  <Text fontSize="sm" fontFamily="mono" noOfLines={2}>
                    {methodId}
                  </Text>
                </Box>
              </Button>
            ))}
          </VStack>
        )}
      </Box>

      {/* 分页控件 */}
      {!loading && !error && pagination.totalPages > 1 && (
        <Flex mt={4} align="center">
          <Button
            size="sm"
            variant="outline"
            leftIcon={<ChevronLeftIcon />}
            onClick={() => handlePageChange(pagination.page - 1)}
            isDisabled={!pagination.hasPrevious}
          >
            上一页
          </Button>
          <Spacer />
          <Text fontSize="sm" color="gray.600">
            {pagination.page} / {pagination.totalPages}
          </Text>
          <Spacer />
          <Button
            size="sm"
            variant="outline"
            rightIcon={<ChevronRightIcon />}
            onClick={() => handlePageChange(pagination.page + 1)}
            isDisabled={!pagination.hasNext}
          >
            下一页
          </Button>
        </Flex>
      )}

      {/* 重试按钮 */}
      {error && (
        <Button
          size="sm"
          mt={4}
          onClick={handleRetry}
          colorScheme="blue"
          variant="outline"
        >
          重试
        </Button>
      )}
    </Box>
  );
};

export default MethodList;
