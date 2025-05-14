import React, { useEffect, useRef, useState } from 'react';
import { Box, Alert, AlertIcon, AlertTitle, AlertDescription } from '@chakra-ui/react';
import mermaid from 'mermaid';

const MermaidRenderer = ({ chart, id }) => {
  const elementRef = useRef(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // 初始化 Mermaid 配置
    mermaid.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      fontFamily: 'Arial, sans-serif',
      fontSize: 14,
      flowchart: {
        useMaxWidth: true,
        htmlLabels: true,
        curve: 'basis'
      },
      sequence: {
        useMaxWidth: true,
        diagramMarginX: 50,
        diagramMarginY: 10,
        actorMargin: 50,
        width: 150,
        height: 65,
        boxMargin: 10,
        boxTextMargin: 5,
        noteMargin: 10,
        messageMargin: 35
      },
      gantt: {
        useMaxWidth: true,
        leftPadding: 75,
        gridLineStartPadding: 35,
        fontSize: 11,
        sectionFontSize: 11,
        numberSectionStyles: 4
      },
      class: {
        useMaxWidth: true
      },
      git: {
        useMaxWidth: true
      },
      state: {
        useMaxWidth: true
      },
      pie: {
        useMaxWidth: true
      },
      er: {
        useMaxWidth: true
      },
      journey: {
        useMaxWidth: true
      }
    });
  }, []);

  useEffect(() => {
    const renderChart = async () => {
      if (!chart || !elementRef.current) return;

      try {
        setIsLoading(true);
        setError(null);

        // 清空容器
        elementRef.current.innerHTML = '';

        // 生成唯一 ID
        const chartId = id || `mermaid-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

        // 验证和渲染图表
        const isValid = await mermaid.parse(chart);
        if (!isValid) {
          throw new Error('Invalid Mermaid syntax');
        }

        // 渲染图表
        const { svg } = await mermaid.render(chartId, chart);
        elementRef.current.innerHTML = svg;

        // 添加样式使图表响应式
        const svgElement = elementRef.current.querySelector('svg');
        if (svgElement) {
          svgElement.style.maxWidth = '100%';
          svgElement.style.height = 'auto';
        }

        setIsLoading(false);
      } catch (err) {
        console.error('Mermaid rendering error:', err);
        setError(err.message || 'Failed to render Mermaid chart');
        setIsLoading(false);
      }
    };

    renderChart();
  }, [chart, id]);

  if (error) {
    return (
      <Alert status="error" mb={4}>
        <AlertIcon />
        <Box>
          <AlertTitle>图表渲染失败</AlertTitle>
          <AlertDescription>
            {error}
          </AlertDescription>
        </Box>
      </Alert>
    );
  }

  if (isLoading) {
    return (
      <Box
        p={4}
        border="1px solid"
        borderColor="gray.200"
        borderRadius="md"
        textAlign="center"
        color="gray.500"
        mb={4}
      >
        正在渲染图表...
      </Box>
    );
  }

  return (
    <Box
      ref={elementRef}
      mb={4}
      p={4}
      border="1px solid"
      borderColor="gray.200"
      borderRadius="md"
      bg="white"
      overflow="auto"
      textAlign="center"
    />
  );
};

export default MermaidRenderer;
