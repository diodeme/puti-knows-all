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
  Divider,
  Code,
  useColorModeValue,
} from '@chakra-ui/react';
import { RepeatIcon, CopyIcon } from '@chakra-ui/icons';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { prism, tomorrow } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { getMethodContent } from '../../api/documentationApi';

const MethodDetail = ({ methodId }) => {
  const [methodData, setMethodData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const toast = useToast();
  
  const bgColor = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.600');
  const codeStyle = useColorModeValue(prism, tomorrow);

  // 加载方法详情
  const loadMethodDetail = useCallback(async (methodId) => {
    if (!methodId) {
      setMethodData(null);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getMethodContent(methodId);
      setMethodData(data);
    } catch (err) {
      console.error('加载方法详情失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载方法详情: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // 当methodId改变时加载详情
  useEffect(() => {
    loadMethodDetail(methodId);
  }, [methodId, loadMethodDetail]);

  // 复制代码到剪贴板
  const handleCopyCode = async () => {
    if (methodData && methodData.content) {
      try {
        await navigator.clipboard.writeText(methodData.content);
        toast({
          title: '复制成功',
          description: '代码已复制到剪贴板',
          status: 'success',
          duration: 2000,
          isClosable: true,
        });
      } catch (err) {
        toast({
          title: '复制失败',
          description: '无法复制到剪贴板',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
      }
    }
  };

  // 重新加载
  const handleRefresh = () => {
    loadMethodDetail(methodId);
  };

  // 检测编程语言
  const detectLanguage = (fullName) => {
    if (!fullName) return 'text';
    
    if (fullName.includes('.java')) return 'java';
    if (fullName.includes('.js') || fullName.includes('.ts')) return 'javascript';
    if (fullName.includes('.py')) return 'python';
    if (fullName.includes('.cs')) return 'csharp';
    if (fullName.includes('.cpp') || fullName.includes('.cc')) return 'cpp';
    if (fullName.includes('.go')) return 'go';
    if (fullName.includes('.php')) return 'php';
    if (fullName.includes('.rb')) return 'ruby';
    
    // 根据包名推断
    if (fullName.startsWith('com.') || fullName.startsWith('org.')) return 'java';
    
    return 'java'; // 默认为Java
  };

  if (!methodId) {
    return (
      <Box
        height="100%"
        display="flex"
        alignItems="center"
        justifyContent="center"
        border="1px solid"
        borderColor={borderColor}
        borderRadius="md"
        bg={bgColor}
      >
        <Text color="gray.500">请选择一个方法查看详情</Text>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box
        height="100%"
        p={4}
        border="1px solid"
        borderColor={borderColor}
        borderRadius="md"
        bg={bgColor}
      >
        <VStack spacing={4} align="stretch">
          <Skeleton height="30px" />
          <Skeleton height="20px" />
          <Divider />
          <Skeleton height="200px" />
        </VStack>
      </Box>
    );
  }

  if (error) {
    return (
      <Box
        height="100%"
        p={4}
        border="1px solid"
        borderColor={borderColor}
        borderRadius="md"
        bg={bgColor}
      >
        <Alert status="error">
          <AlertIcon />
          <Box>
            <AlertTitle>加载失败</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Box>
        </Alert>
        <Button
          size="sm"
          mt={4}
          onClick={handleRefresh}
          colorScheme="blue"
          variant="outline"
        >
          重试
        </Button>
      </Box>
    );
  }

  if (!methodData) {
    return (
      <Box
        height="100%"
        display="flex"
        alignItems="center"
        justifyContent="center"
        border="1px solid"
        borderColor={borderColor}
        borderRadius="md"
        bg={bgColor}
      >
        <Text color="gray.500">方法不存在或已被删除</Text>
      </Box>
    );
  }

  const language = detectLanguage(methodData.fullName);

  return (
    <Box
      height="100%"
      display="flex"
      flexDirection="column"
      border="1px solid"
      borderColor={borderColor}
      borderRadius="md"
      bg={bgColor}
      overflow="hidden"
    >
      {/* 头部信息 */}
      <Box p={4} borderBottom="1px solid" borderColor={borderColor}>
        <HStack justify="space-between" mb={3}>
          <VStack align="start" spacing={1}>
            <Text fontSize="lg" fontWeight="bold" noOfLines={1}>
              {methodData.methodName}
            </Text>
            <Code fontSize="sm" colorScheme="gray">
              {methodData.fullName}
            </Code>
          </VStack>
          <HStack>
            {methodData.isEntryPoint && (
              <Badge colorScheme="green" variant="solid">
                入口方法
              </Badge>
            )}
            <Button
              size="sm"
              variant="outline"
              leftIcon={<CopyIcon />}
              onClick={handleCopyCode}
            >
              复制
            </Button>
            <Button
              size="sm"
              variant="outline"
              leftIcon={<RepeatIcon />}
              onClick={handleRefresh}
            >
              刷新
            </Button>
          </HStack>
        </HStack>
        
        <HStack>
          <Badge colorScheme="blue" variant="subtle">
            {language.toUpperCase()}
          </Badge>
          <Text fontSize="sm" color="gray.600">
            方法ID: {methodData.methodId}
          </Text>
        </HStack>
      </Box>

      {/* 代码内容 */}
      <Box flex={1} overflow="auto">
        {methodData.content ? (
          <SyntaxHighlighter
            language={language}
            style={codeStyle}
            customStyle={{
              margin: 0,
              padding: '16px',
              fontSize: '14px',
              lineHeight: '1.5',
              height: '100%',
              overflow: 'auto',
            }}
            showLineNumbers={true}
            wrapLines={true}
          >
            {methodData.content}
          </SyntaxHighlighter>
        ) : (
          <Box
            height="100%"
            display="flex"
            alignItems="center"
            justifyContent="center"
            color="gray.500"
          >
            该方法暂无源代码内容
          </Box>
        )}
      </Box>
    </Box>
  );
};

export default MethodDetail;
