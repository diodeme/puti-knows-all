import React, { useState, useEffect, useCallback } from 'react';
import {
  Select,
  HStack,
  Text,
  useToast,
  Spinner,
} from '@chakra-ui/react';
import { getProjects } from '../../api/documentationApi';

const ProjectSelector = ({ selectedProjectId, onProjectSelect }) => {
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
  const handleProjectChange = (event) => {
    const projectId = event.target.value;
    if (projectId) {
      const selectedProject = projects.find(p => p.projectId === projectId);
      if (selectedProject && onProjectSelect) {
        onProjectSelect(selectedProject);
      }
    } else {
      // 选择了空选项，清除选择
      if (onProjectSelect) {
        onProjectSelect(null);
      }
    }
  };

  if (loading) {
    return (
      <HStack spacing={2}>
        <Text fontSize="sm" color="gray.600">项目:</Text>
        <Spinner size="sm" />
      </HStack>
    );
  }

  if (error) {
    return (
      <HStack spacing={2}>
        <Text fontSize="sm" color="red.500">项目加载失败</Text>
      </HStack>
    );
  }

  return (
    <HStack spacing={2}>
      <Text fontSize="sm" color="gray.600" whiteSpace="nowrap">项目:</Text>
      <Select
        placeholder="请选择项目"
        value={selectedProjectId || ''}
        onChange={handleProjectChange}
        size="sm"
        width="200px"
        bg="white"
      >
        {projects.map((project) => (
          <option key={project.projectId} value={project.projectId}>
            {project.projectName || project.projectId}
          </option>
        ))}
      </Select>
    </HStack>
  );
};

export default ProjectSelector;
