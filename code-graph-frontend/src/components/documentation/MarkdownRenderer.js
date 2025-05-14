import React from 'react';
import { Box, useColorModeValue } from '@chakra-ui/react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/github.css'; // 代码高亮样式
import MermaidRenderer from './MermaidRenderer';

const MarkdownRenderer = ({ content, ...props }) => {
  const bgColor = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.600');
  const codeBlockBg = useColorModeValue('gray.50', 'gray.900');
  const inlineCodeBg = useColorModeValue('gray.100', 'gray.700');
  const inlineCodeColor = useColorModeValue('red.600', 'red.300');
  const blockquoteBg = useColorModeValue('blue.50', 'blue.900');
  const theadBg = useColorModeValue('gray.50', 'gray.700');

  // 自定义组件映射
  const components = {
    // 标题样式
    h1: ({ children }) => (
      <Box
        as="h1"
        fontSize="2xl"
        fontWeight="bold"
        mb={4}
        mt={6}
        pb={2}
        borderBottom="2px solid"
        borderColor={borderColor}
      >
        {children}
      </Box>
    ),
    h2: ({ children }) => (
      <Box
        as="h2"
        fontSize="xl"
        fontWeight="bold"
        mb={3}
        mt={5}
        pb={1}
        borderBottom="1px solid"
        borderColor={borderColor}
      >
        {children}
      </Box>
    ),
    h3: ({ children }) => (
      <Box as="h3" fontSize="lg" fontWeight="bold" mb={2} mt={4}>
        {children}
      </Box>
    ),
    h4: ({ children }) => (
      <Box as="h4" fontSize="md" fontWeight="bold" mb={2} mt={3}>
        {children}
      </Box>
    ),
    h5: ({ children }) => (
      <Box as="h5" fontSize="sm" fontWeight="bold" mb={2} mt={3}>
        {children}
      </Box>
    ),
    h6: ({ children }) => (
      <Box as="h6" fontSize="xs" fontWeight="bold" mb={2} mt={3}>
        {children}
      </Box>
    ),

    // 段落样式
    p: ({ children }) => (
      <Box as="p" mb={4} lineHeight="1.6">
        {children}
      </Box>
    ),

    // 列表样式
    ul: ({ children }) => (
      <Box as="ul" pl={6} mb={4} listStyleType="disc">
        {children}
      </Box>
    ),
    ol: ({ children }) => (
      <Box as="ol" pl={6} mb={4} listStyleType="decimal">
        {children}
      </Box>
    ),
    li: ({ children }) => (
      <Box as="li" mb={1}>
        {children}
      </Box>
    ),

    // 代码块样式
    pre: ({ children }) => {
      // 检查是否是 Mermaid 代码块
      const codeElement = children?.props?.children;
      const className = children?.props?.className || '';
      const language = className.replace('language-', '');

      if (language === 'mermaid' && typeof codeElement === 'string') {
        return <MermaidRenderer chart={codeElement.trim()} />;
      }

      return (
        <Box
          as="pre"
          bg={codeBlockBg}
          p={4}
          borderRadius="md"
          border="1px solid"
          borderColor={borderColor}
          overflow="auto"
          mb={4}
          fontSize="sm"
          fontFamily="'Fira Code', 'Monaco', 'Consolas', monospace"
        >
          {children}
        </Box>
      );
    },

    // 行内代码样式
    code: ({ inline, children, className, ...props }) => {
      // 检查是否是 Mermaid 代码块（非行内）
      const language = className?.replace('language-', '') || '';

      if (!inline && language === 'mermaid' && typeof children === 'string') {
        return <MermaidRenderer chart={children.trim()} />;
      }

      if (inline) {
        return (
          <Box
            as="code"
            bg={inlineCodeBg}
            color={inlineCodeColor}
            px={1}
            py={0.5}
            borderRadius="sm"
            fontSize="sm"
            fontFamily="'Fira Code', 'Monaco', 'Consolas', monospace"
            {...props}
          >
            {children}
          </Box>
        );
      }

      return (
        <Box
          as="code"
          fontFamily="'Fira Code', 'Monaco', 'Consolas', monospace"
          {...props}
        >
          {children}
        </Box>
      );
    },

    // 引用块样式
    blockquote: ({ children }) => (
      <Box
        as="blockquote"
        borderLeft="4px solid"
        borderColor="blue.400"
        pl={4}
        py={2}
        bg={blockquoteBg}
        mb={4}
        fontStyle="italic"
      >
        {children}
      </Box>
    ),

    // 表格样式
    table: ({ children }) => (
      <Box
        as="table"
        width="100%"
        mb={4}
        border="1px solid"
        borderColor={borderColor}
        borderRadius="md"
        overflow="hidden"
      >
        {children}
      </Box>
    ),
    thead: ({ children }) => (
      <Box as="thead" bg={theadBg}>
        {children}
      </Box>
    ),
    tbody: ({ children }) => (
      <Box as="tbody">
        {children}
      </Box>
    ),
    tr: ({ children }) => (
      <Box as="tr" borderBottom="1px solid" borderColor={borderColor}>
        {children}
      </Box>
    ),
    th: ({ children }) => (
      <Box as="th" p={3} textAlign="left" fontWeight="bold">
        {children}
      </Box>
    ),
    td: ({ children }) => (
      <Box as="td" p={3}>
        {children}
      </Box>
    ),

    // 分割线样式
    hr: () => (
      <Box
        as="hr"
        border="none"
        borderTop="1px solid"
        borderColor={borderColor}
        my={6}
      />
    ),

    // 链接样式
    a: ({ children, href }) => (
      <Box
        as="a"
        href={href}
        color="blue.500"
        textDecoration="underline"
        _hover={{ color: 'blue.600' }}
        target="_blank"
        rel="noopener noreferrer"
      >
        {children}
      </Box>
    ),

    // 图片样式
    img: ({ src, alt }) => (
      <Box
        as="img"
        src={src}
        alt={alt}
        maxWidth="100%"
        height="auto"
        borderRadius="md"
        mb={4}
      />
    ),
  };

  if (!content) {
    return (
      <Box
        bg={bgColor}
        p={6}
        borderRadius="md"
        border="1px solid"
        borderColor={borderColor}
        textAlign="center"
        color="gray.500"
        {...props}
      >
        暂无内容
      </Box>
    );
  }

  return (
    <Box
      bg={bgColor}
      p={6}
      borderRadius="md"
      border="1px solid"
      borderColor={borderColor}
      overflow="auto"
      {...props}
    >
      <ReactMarkdown
        components={components}
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
      >
        {content}
      </ReactMarkdown>
    </Box>
  );
};

export default MarkdownRenderer;
