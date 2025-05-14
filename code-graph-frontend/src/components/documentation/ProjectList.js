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
} from '@chakra-ui/react';
import { getProjects } from '../../api/documentationApi';

const ProjectList = ({ selectedProjectId, onProjectSelect }) => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const toast = useToast();

  // 加载项目列表
  const loadProjects = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getProjects();
      setProjects(data || []);
    } catch (err) {
      console.error('加载项目列表失败:', err);
      setError(err.message);
      toast({
        title: '加载失败',
        description: `无法加载项目列表: ${err.message}`,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  // 处理项目选择
  const handleProjectClick = (project) => {
    if (onProjectSelect) {
      onProjectSelect(project);
    }
  };

  // 重试加载
  const handleRetry = () => {
    loadProjects();
  };

  if (loading) {
    return (
      <Box>
        <Text fontSize="sm" fontWeight="bold" mb={3} color="gray.600">
          项目列表
        </Text>
        <VStack spacing={2} align="stretch">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} height="60px" borderRadius="md" />
          ))}
        </VStack>
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        <Text fontSize="sm" fontWeight="bold" mb={3} color="gray.600">
          项目列表
        </Text>
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
    );
  }

  if (projects.length === 0) {
    return (
      <Box>
        <Text fontSize="sm" fontWeight="bold" mb={3} color="gray.600">
          项目列表
        </Text>
        <Alert status="info" size="sm" borderRadius="md">
          <AlertIcon />
          <Box>
            <AlertTitle fontSize="sm">暂无项目</AlertTitle>
            <AlertDescription fontSize="xs">
              还没有接入任何子系统
            </AlertDescription>
          </Box>
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Text fontSize="sm" fontWeight="bold" mb={3} color="gray.600">
        项目列表 ({projects.length})
      </Text>
      <VStack spacing={2} align="stretch">
        {projects.map((project) => (
          <Button
            key={project.projectId}
            variant={selectedProjectId === project.projectId ? "solid" : "ghost"}
            colorScheme={selectedProjectId === project.projectId ? "blue" : "gray"}
            size="sm"
            justifyContent="flex-start"
            textAlign="left"
            height="auto"
            py={3}
            px={3}
            onClick={() => handleProjectClick(project)}
            _hover={{
              bg: selectedProjectId === project.projectId ? undefined : "gray.100"
            }}
          >
            <Box width="100%">
              <Text fontSize="sm" fontWeight="medium" noOfLines={1}>
                {project.projectName || project.projectId}
              </Text>
              {project.projectName && (
                <Text fontSize="xs" color="gray.500" noOfLines={1}>
                  {project.projectId}
                </Text>
              )}
              <Box mt={1}>
                <Badge size="sm" colorScheme="green" variant="subtle">
                  {project.documentationCount} 个文档
                </Badge>
              </Box>
            </Box>
          </Button>
        ))}
      </VStack>
    </Box>
  );
};

export default ProjectList;
