import React, { useState } from 'react';
import {
  Box,
  Grid,
  GridItem,
  Heading,
  Text,
  VStack,
  HStack,
  Button,
  useColorModeValue,
  Divider,
  Icon,
  useDisclosure,
} from '@chakra-ui/react';
import { HamburgerIcon, CloseIcon } from '@chakra-ui/icons';
import ProjectSelector from '../components/documentation/ProjectSelector';
import AggregatedDocList from '../components/documentation/AggregatedDocList';
import DocumentationViewer from '../components/documentation/DocumentationViewer';
import MethodDetail from '../components/documentation/MethodDetail';

const ManualPage = () => {
  // 状态管理
  const [selectedProject, setSelectedProject] = useState(null);
  const [selectedAggregatedDoc, setSelectedAggregatedDoc] = useState(null);
  const [selectedProcessDoc, setSelectedProcessDoc] = useState(null);
  const [selectedMethodId, setSelectedMethodId] = useState(null);

  // 导航展开状态
  const [aggregatedExpanded, setAggregatedExpanded] = useState(true);

  // 侧边栏控制
  const { isOpen: sidebarOpen, onToggle: toggleSidebar } = useDisclosure({ defaultIsOpen: true });

  const bgColor = useColorModeValue('gray.50', 'gray.900');
  const sidebarBg = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.600');

  // 处理项目选择
  const handleProjectSelect = (project) => {
    setSelectedProject(project);
    setSelectedAggregatedDoc(null);
    setSelectedProcessDoc(null);
    setSelectedMethodId(null);
  };

  // 处理聚合说明书选择
  const handleAggregatedDocSelect = (doc) => {
    setSelectedAggregatedDoc(doc);
    setSelectedProcessDoc(null);
    setSelectedMethodId(null);
  };

  // 处理流程说明书选择
  const handleProcessDocSelect = (doc) => {
    setSelectedProcessDoc(doc);
    setSelectedMethodId(null);
  };

  // 处理方法选择
  const handleMethodSelect = (methodId) => {
    setSelectedMethodId(methodId);
  };

  return (
    <Box minHeight="100vh" bg={bgColor}>
      {/* 顶部导航栏 */}
      <Box
        bg={sidebarBg}
        borderBottom="1px solid"
        borderColor={borderColor}
        px={4}
        py={3}
      >
        <HStack justify="space-between">
          <HStack spacing={4}>
            <Button
              size="sm"
              variant="ghost"
              onClick={toggleSidebar}
              leftIcon={<Icon as={sidebarOpen ? CloseIcon : HamburgerIcon} />}
            >
              {sidebarOpen ? '收起' : '展开'}
            </Button>
            <Divider orientation="vertical" height="20px" />
            <Heading as="h1" size="md">
              说明书查看器
            </Heading>
            <Divider orientation="vertical" height="20px" />
            {/* 项目选择器 */}
            <ProjectSelector
              selectedProjectId={selectedProject?.projectId}
              onProjectSelect={handleProjectSelect}
            />
          </HStack>

          {/* 面包屑导航 */}
          <HStack spacing={2} fontSize="sm" color="gray.600">
            {selectedProject && (
              <>
                <Text>{selectedProject.projectName || selectedProject.projectId}</Text>
                {selectedAggregatedDoc && (
                  <>
                    <Text>/</Text>
                    <Text>{selectedAggregatedDoc.title}</Text>
                  </>
                )}
                {selectedProcessDoc && (
                  <>
                    <Text>/</Text>
                    <Text>{selectedProcessDoc.title}</Text>
                  </>
                )}
              </>
            )}
          </HStack>
        </HStack>
      </Box>

      {/* 主要内容区域 */}
      <Grid
        templateColumns={sidebarOpen ? "300px 1fr" : "0 1fr"}
        height="calc(100vh - 60px)"
        transition="all 0.3s"
      >
        {/* 左侧导航面板 */}
        <GridItem
          bg={sidebarBg}
          borderRight="1px solid"
          borderColor={borderColor}
          overflow="hidden"
          transition="all 0.3s"
        >
          {sidebarOpen && (
            <Box p={4} height="100%" overflow="auto">
              <VStack spacing={4} align="stretch">
                {/* 聚合说明书列表（包含流程说明书） */}
                {selectedProject && (
                  <AggregatedDocList
                    projectId={selectedProject.projectId}
                    selectedAggregatedDocId={selectedAggregatedDoc?.id}
                    onAggregatedDocSelect={handleAggregatedDocSelect}
                    onProcessDocSelect={handleProcessDocSelect}
                    isExpanded={aggregatedExpanded}
                    onToggleExpanded={() => setAggregatedExpanded(!aggregatedExpanded)}
                  />
                )}
              </VStack>
            </Box>
          )}
        </GridItem>

        {/* 右侧内容区域 */}
        <GridItem overflow="hidden">
          <Grid
            templateRows="1fr"
            templateColumns={selectedMethodId ? "1fr 400px" : "1fr"}
            height="100%"
            gap={0}
          >
            {/* 文档查看器 */}
            <GridItem p={4} overflow="hidden">
              <DocumentationViewer
                selectedAggregatedDoc={selectedAggregatedDoc}
                selectedProcessDoc={selectedProcessDoc}
                onMethodSelect={handleMethodSelect}
              />
            </GridItem>

            {/* 方法详情面板 */}
            {selectedMethodId && (
              <GridItem
                borderLeft="1px solid"
                borderColor={borderColor}
                p={4}
                overflow="hidden"
              >
                <MethodDetail methodId={selectedMethodId} />
              </GridItem>
            )}
          </Grid>
        </GridItem>
      </Grid>
    </Box>
  );
};

export default ManualPage;
