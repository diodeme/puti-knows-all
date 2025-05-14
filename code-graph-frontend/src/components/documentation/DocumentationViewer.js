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
  Tabs,
  TabList,
  TabPanels,
  Tab,
  TabPanel,
  Icon,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalCloseButton,
  useDisclosure,
  IconButton,
} from '@chakra-ui/react';
import { RepeatIcon, ViewIcon, SearchIcon } from '@chakra-ui/icons';
import MarkdownRenderer from './MarkdownRenderer';
import MethodList from './MethodList';
import { 
  getAggregatedDocumentationContent, 
  getProcessDocumentationContent 
} from '../../api/documentationApi';

const DocumentationViewer = ({ 
  selectedAggregatedDoc, 
  selectedProcessDoc,
  onMethodSelect 
}) => {
  const [aggregatedContent, setAggregatedContent] = useState('');
  const [processContent, setProcessContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState(0);
  const toast = useToast();
  const { isOpen, onOpen, onClose } = useDisclosure();

  // 加载聚合说明书内容
  const loadAggregatedContent = useCallback(async (docId) => {
    if (!docId) {
      setAggregatedContent('');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const content = await getAggregatedDocumentationContent(docId);
      setAggregatedContent(content || '');
    } catch (err) {
      console.error('加载聚合说明书内容失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载聚合说明书内容: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // 加载流程说明书内容
  const loadProcessContent = useCallback(async (docId) => {
    if (!docId) {
      setProcessContent('');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const content = await getProcessDocumentationContent(docId);
      setProcessContent(content || '');
    } catch (err) {
      console.error('加载流程说明书内容失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载流程说明书内容: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // 当选中的文档改变时加载内容
  useEffect(() => {
    if (selectedAggregatedDoc) {
      loadAggregatedContent(selectedAggregatedDoc.id);
      setActiveTab(0); // 切换到聚合说明书标签
    }
  }, [selectedAggregatedDoc, loadAggregatedContent]);

  useEffect(() => {
    if (selectedProcessDoc) {
      loadProcessContent(selectedProcessDoc.id);
      setActiveTab(1); // 切换到流程说明书标签
    }
  }, [selectedProcessDoc, loadProcessContent]);

  // 重新加载当前内容
  const handleRefresh = () => {
    if (activeTab === 0 && selectedAggregatedDoc) {
      loadAggregatedContent(selectedAggregatedDoc.id);
    } else if (activeTab === 1 && selectedProcessDoc) {
      loadProcessContent(selectedProcessDoc.id);
    }
  };

  // 如果没有选中任何文档
  if (!selectedAggregatedDoc && !selectedProcessDoc) {
    return (
      <Box
        height="calc(100vh - 200px)"
        minHeight="600px"
        display="flex"
        alignItems="center"
        justifyContent="center"
        border="1px solid"
        borderColor="gray.200"
        borderRadius="md"
        bg="gray.50"
      >
        <VStack spacing={4}>
          <Icon as={ViewIcon} boxSize={12} color="gray.400" />
          <Text color="gray.500" fontSize="lg">
            请从左侧选择一个说明书查看内容
          </Text>
        </VStack>
      </Box>
    );
  }

  return (
    <Box height="calc(100vh - 200px)" minHeight="600px" display="flex" flexDirection="column">
      {/* 头部信息 */}
      <HStack justify="space-between" mb={4} p={4} bg="gray.50" borderRadius="md">
        <VStack align="start" spacing={1}>
          {selectedAggregatedDoc && (
            <>
              <Text fontSize="lg" fontWeight="bold">
                {selectedAggregatedDoc.title}
              </Text>
              <HStack>
                <Badge colorScheme="purple" variant="subtle">
                  聚合说明书
                </Badge>
                {selectedAggregatedDoc.aggregationType && (
                  <Badge colorScheme="blue" variant="subtle">
                    {selectedAggregatedDoc.aggregationType}
                  </Badge>
                )}
              </HStack>
            </>
          )}
          {selectedProcessDoc && (
            <>
              <Text fontSize="lg" fontWeight="bold">
                {selectedProcessDoc.title}
              </Text>
              <HStack>
                <Badge colorScheme="green" variant="subtle">
                  流程说明书
                </Badge>
                {selectedProcessDoc.entryPointName && (
                  <Badge colorScheme="orange" variant="subtle">
                    入口: {selectedProcessDoc.entryPointName}
                  </Badge>
                )}
              </HStack>
            </>
          )}
        </VStack>
        <HStack spacing={2}>
          <IconButton
            icon={<SearchIcon />}
            size="sm"
            variant="outline"
            onClick={onOpen}
            aria-label="放大显示"
            title="放大显示"
            isDisabled={!selectedAggregatedDoc && !selectedProcessDoc}
          />
          <Button
            leftIcon={<RepeatIcon />}
            size="sm"
            variant="outline"
            onClick={handleRefresh}
            isLoading={loading}
          >
            刷新
          </Button>
        </HStack>
      </HStack>

      {/* 内容区域 */}
      <Box flex={1} overflow="hidden">
        <Tabs 
          index={activeTab} 
          onChange={setActiveTab} 
          height="100%" 
          display="flex" 
          flexDirection="column"
        >
          <TabList>
            {selectedAggregatedDoc && (
              <Tab>聚合说明书</Tab>
            )}
            {selectedProcessDoc && (
              <Tab>流程说明书</Tab>
            )}
            {selectedProcessDoc && (
              <Tab>关联方法</Tab>
            )}
          </TabList>

          <TabPanels flex={1} overflow="hidden">
            {selectedAggregatedDoc && (
              <TabPanel height="100%" p={0} pt={4}>
                {loading ? (
                  <VStack spacing={4}>
                    <Skeleton height="40px" />
                    <Skeleton height="20px" />
                    <Skeleton height="20px" />
                    <Skeleton height="60px" />
                  </VStack>
                ) : error ? (
                  <Alert status="error">
                    <AlertIcon />
                    <Box>
                      <AlertTitle>加载失败</AlertTitle>
                      <AlertDescription>{error}</AlertDescription>
                    </Box>
                  </Alert>
                ) : (
                  <MarkdownRenderer 
                    content={aggregatedContent} 
                    height="100%" 
                    overflow="auto"
                  />
                )}
              </TabPanel>
            )}

            {selectedProcessDoc && (
              <TabPanel height="100%" p={0} pt={4}>
                {loading ? (
                  <VStack spacing={4}>
                    <Skeleton height="40px" />
                    <Skeleton height="20px" />
                    <Skeleton height="20px" />
                    <Skeleton height="60px" />
                  </VStack>
                ) : error ? (
                  <Alert status="error">
                    <AlertIcon />
                    <Box>
                      <AlertTitle>加载失败</AlertTitle>
                      <AlertDescription>{error}</AlertDescription>
                    </Box>
                  </Alert>
                ) : (
                  <MarkdownRenderer 
                    content={processContent} 
                    height="100%" 
                    overflow="auto"
                  />
                )}
              </TabPanel>
            )}

            {selectedProcessDoc && (
              <TabPanel height="100%" p={0} pt={4}>
                <MethodList 
                  processDocId={selectedProcessDoc.id}
                  onMethodSelect={onMethodSelect}
                />
              </TabPanel>
            )}
          </TabPanels>
        </Tabs>
      </Box>

      {/* 全屏模态框 */}
      <Modal isOpen={isOpen} onClose={onClose} size="full">
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>
            <HStack justify="space-between" align="center">
              <VStack align="start" spacing={1}>
                {selectedAggregatedDoc && (
                  <>
                    <Text fontSize="xl" fontWeight="bold">
                      {selectedAggregatedDoc.title}
                    </Text>
                    <HStack>
                      <Badge colorScheme="purple" variant="subtle">
                        聚合说明书
                      </Badge>
                      {selectedAggregatedDoc.aggregationType && (
                        <Badge colorScheme="blue" variant="subtle">
                          {selectedAggregatedDoc.aggregationType}
                        </Badge>
                      )}
                    </HStack>
                  </>
                )}
                {selectedProcessDoc && (
                  <>
                    <Text fontSize="xl" fontWeight="bold">
                      {selectedProcessDoc.title}
                    </Text>
                    <HStack>
                      <Badge colorScheme="green" variant="subtle">
                        流程说明书
                      </Badge>
                      {selectedProcessDoc.entryPointName && (
                        <Badge colorScheme="orange" variant="subtle">
                          入口: {selectedProcessDoc.entryPointName}
                        </Badge>
                      )}
                    </HStack>
                  </>
                )}
              </VStack>
            </HStack>
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody pb={6}>
            <Tabs
              index={activeTab}
              onChange={setActiveTab}
              height="calc(100vh - 200px)"
              display="flex"
              flexDirection="column"
            >
              <TabList>
                {selectedAggregatedDoc && (
                  <Tab>聚合说明书</Tab>
                )}
                {selectedProcessDoc && (
                  <>
                    <Tab>流程说明书</Tab>
                    <Tab>关联方法</Tab>
                  </>
                )}
              </TabList>

              <TabPanels flex={1} overflow="hidden">
                {selectedAggregatedDoc && (
                  <TabPanel height="100%" p={0} pt={4}>
                    {error ? (
                      <Alert status="error">
                        <AlertIcon />
                        <AlertTitle>加载失败</AlertTitle>
                        <AlertDescription>{error}</AlertDescription>
                      </Alert>
                    ) : loading ? (
                      <VStack spacing={4} align="stretch">
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                      </VStack>
                    ) : (
                      <MarkdownRenderer
                        content={aggregatedContent}
                        height="100%"
                        overflow="auto"
                      />
                    )}
                  </TabPanel>
                )}

                {selectedProcessDoc && (
                  <TabPanel height="100%" p={0} pt={4}>
                    {error ? (
                      <Alert status="error">
                        <AlertIcon />
                        <AlertTitle>加载失败</AlertTitle>
                        <AlertDescription>{error}</AlertDescription>
                      </Alert>
                    ) : loading ? (
                      <VStack spacing={4} align="stretch">
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                        <Skeleton height="20px" />
                      </VStack>
                    ) : (
                      <MarkdownRenderer
                        content={processContent}
                        height="100%"
                        overflow="auto"
                      />
                    )}
                  </TabPanel>
                )}

                {selectedProcessDoc && (
                  <TabPanel height="100%" p={0} pt={4}>
                    <MethodList
                      processDocId={selectedProcessDoc.id}
                      onMethodSelect={onMethodSelect}
                    />
                  </TabPanel>
                )}
              </TabPanels>
            </Tabs>
          </ModalBody>
        </ModalContent>
      </Modal>
    </Box>
  );
};

export default DocumentationViewer;
