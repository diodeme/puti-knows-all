import React, { useEffect, useState } from 'react'
import {
  Badge,
  Box,
  Code,
  Divider,
  Grid,
  GridItem,
  Heading,
  HStack,
  Spinner,
  Stack,
  Stat,
  StatLabel,
  StatNumber,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
  useColorModeValue
} from '@chakra-ui/react'
import { getNodeDetail } from '../api/api'
import SyntaxHighlighter from 'react-syntax-highlighter'
import { docco } from 'react-syntax-highlighter/dist/esm/styles/hljs'

const NodeDetails = ({ nodeId, selectedNode }) => {
  const [nodeDetail, setNodeDetail] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const bgColor = useColorModeValue('white', 'gray.800')
  const selectedNodeType = selectedNode?.type || selectedNode?.properties?.node_type
  const fallbackDetail = selectedNode
    ? {
        name: selectedNode.label || selectedNode.properties?.name || String(selectedNode.id),
        full_name:
          selectedNode.properties?.full_name ||
          selectedNode.properties?.qualified_name ||
          selectedNode.label ||
          String(selectedNode.id),
        content: null,
        raw_properties: selectedNode.properties || {}
      }
    : null

  useEffect(() => {
    const fetchNodeDetail = async () => {
      if (!nodeId && nodeId !== 0 && nodeId !== '0') {
        setNodeDetail(null)
        setError(null)
        return
      }

      const normalizedNodeId = String(nodeId)
      setLoading(true)
      setError(null)

      try {
        const detail = await getNodeDetail(normalizedNodeId)
        setNodeDetail(detail)
      } catch (err) {
        setError(err.message || '获取节点详情失败')
      } finally {
        setLoading(false)
      }
    }

    fetchNodeDetail()
  }, [nodeId, selectedNodeType])

  if (!nodeId && nodeId !== 0 && nodeId !== '0') {
    return (
      <Box p={5} shadow='lg' borderWidth='1px' borderRadius='24px' bg={bgColor} width='100%' minHeight='400px'>
        <Heading size='md' mb={4}>
          节点详情
        </Heading>
        <Text color='gray.600'>请从树图中选择一个节点查看详情</Text>
      </Box>
    )
  }

  if (loading) {
    return (
      <Box
        p={5}
        shadow='lg'
        borderWidth='1px'
        borderRadius='24px'
        bg={bgColor}
        width='100%'
        minHeight='400px'
        textAlign='center'
      >
        <Heading size='md' mb={4}>
          节点详情
        </Heading>
        <Spinner mt={10} size='xl' />
        <Text mt={4}>加载节点详情...</Text>
      </Box>
    )
  }

  if (error && !fallbackDetail) {
    return (
      <Box p={5} shadow='lg' borderWidth='1px' borderRadius='24px' bg={bgColor} width='100%' minHeight='400px'>
        <Heading size='md' mb={4}>
          节点详情
        </Heading>
        <Text color='red.500'>错误: {error}</Text>
      </Box>
    )
  }

  const detailToRender = nodeDetail || fallbackDetail

  if (!detailToRender) {
    return null
  }

  const { name, full_name, content, raw_properties = {} } = detailToRender

  const getLanguage = () => {
    const extension = raw_properties.file_path ? raw_properties.file_path.split('.').pop() : ''

    switch (extension) {
      case 'java':
        return 'java'
      case 'py':
        return 'python'
      case 'js':
        return 'javascript'
      case 'ts':
        return 'typescript'
      case 'php':
        return 'php'
      case 'rb':
        return 'ruby'
      case 'go':
        return 'go'
      case 'cpp':
      case 'c':
        return 'cpp'
      default:
        return 'java'
    }
  }

  return (
    <Box
      p={5}
      shadow='lg'
      borderWidth='1px'
      borderRadius='24px'
      bg={bgColor}
      width='100%'
      minHeight='600px'
      maxHeight={{ base: 'none', '2xl': 'calc(100vh - 132px)' }}
      overflowY='auto'
    >
      <Heading size='md' mb={4}>
        节点详情
      </Heading>

      <Box mb={4}>
        <HStack wrap='wrap' spacing={2} mb={3}>
          <Badge colorScheme='blue'>{raw_properties.node_type || selectedNodeType || raw_properties.type || 'function'}</Badge>
          {raw_properties.visibility && <Badge colorScheme='green'>{raw_properties.visibility}</Badge>}
          {raw_properties.branch_name && <Badge colorScheme='orange'>Branch: {raw_properties.branch_name}</Badge>}
          {raw_properties.repo_id && <Badge colorScheme='purple'>Repo: {raw_properties.repo_id}</Badge>}
          {raw_properties.is_static && <Badge colorScheme='pink'>static</Badge>}
          {raw_properties.is_constructor && <Badge colorScheme='teal'>constructor</Badge>}
        </HStack>

        <Text fontWeight='bold' fontSize='2xl' mb={2}>
          {name}
        </Text>
        <Text color='gray.600' fontSize='sm' wordBreak='break-all'>
          {full_name}
        </Text>
        <Text color='gray.400' fontSize='xs' mt={2}>
          节点ID: {nodeId}
        </Text>
      </Box>

      <Divider my={3} />

      <Grid templateColumns={{ base: '1fr', md: 'repeat(2, minmax(0, 1fr))' }} gap={4} mb={4}>
        <GridItem>
          <Stat size='sm'>
            <StatLabel>行范围</StatLabel>
            <StatNumber fontSize='md'>
              {raw_properties.line_start || '-'} - {raw_properties.line_end || '-'}
            </StatNumber>
          </Stat>
        </GridItem>

        <GridItem>
          <Stat size='sm'>
            <StatLabel>复杂度</StatLabel>
            <StatNumber fontSize='md'>{raw_properties.complexity ?? '-'}</StatNumber>
          </Stat>
        </GridItem>
      </Grid>

      <Tabs variant='enclosed' size='sm' mt={3}>
        <TabList>
          <Tab>代码内容</Tab>
          <Tab>属性信息</Tab>
        </TabList>

        <TabPanels>
          <TabPanel p={2}>
            {content ? (
              <Box borderWidth='1px' borderRadius='16px' overflow='hidden'>
                <SyntaxHighlighter
                  language={getLanguage()}
                  style={docco}
                  customStyle={{
                    margin: 0,
                    maxHeight: '380px',
                    fontSize: '12px',
                    borderRadius: '16px'
                  }}
                >
                  {content}
                </SyntaxHighlighter>
              </Box>
            ) : (
              <Text color='gray.500'>无代码内容或解压失败</Text>
            )}
          </TabPanel>

          <TabPanel p={2}>
            <Stack spacing={3}>
              <HStack spacing={2} wrap='wrap'>
                {raw_properties.branch_name && <Badge colorScheme='orange'>分支: {raw_properties.branch_name}</Badge>}
                {raw_properties.repo_id && <Badge colorScheme='purple'>仓库: {raw_properties.repo_id}</Badge>}
              </HStack>
              <Box overflowX='auto'>
                <pre>
                  <Code p={3} borderRadius='16px' fontSize='xs' width='100%' whiteSpace='pre-wrap'>
                    {JSON.stringify(raw_properties, null, 2)}
                  </Code>
                </pre>
              </Box>
            </Stack>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </Box>
  )
}

export default NodeDetails
