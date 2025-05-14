import React, { useMemo, useState } from 'react'
import { Box, Grid, GridItem, Heading, Text } from '@chakra-ui/react'
import MethodSearch from '../components/MethodSearch'
import NodeTree from '../components/NodeTree'
import NodeDetails from '../components/NodeDetails'

const Dashboard = () => {
  const [selectedMethod, setSelectedMethod] = useState(null)
  const [selectedNodeId, setSelectedNodeId] = useState(null)
  const [selectedNode, setSelectedNode] = useState(null)
  const [queryType, setQueryType] = useState('self')
  const [pathDepth, setPathDepth] = useState('1')

  const activeMethodData = useMemo(() => {
    if (!selectedMethod) {
      return null
    }

    return {
      ...selectedMethod,
      queryType,
      pathDepth: queryType === 'self' ? 1 : parseInt(pathDepth, 10)
    }
  }, [pathDepth, queryType, selectedMethod])

  const handleMethodSelect = method => {
    setSelectedMethod(method)
    setSelectedNodeId(method.nodeId || null)
    setSelectedNode(null)
  }

  const handleNodeClick = node => {
    setSelectedNodeId(node?.id || null)
    setSelectedNode(node || null)
  }

  return (
    <Box
      p={{ base: 4, lg: 6 }}
      maxWidth='100%'
      minHeight='100vh'
      bg='linear-gradient(180deg, #f6f8fb 0%, #eef4fb 100%)'
    >
      <Grid
        templateColumns={{
          base: '1fr',
          xl: '340px minmax(0, 1.2fr) minmax(360px, 0.85fr)'
        }}
        gap={6}
        alignItems='start'
      >
        <GridItem position={{ base: 'static', xl: 'sticky' }} top={{ xl: 6 }} alignSelf='start'>
          <MethodSearch
            onMethodSelect={handleMethodSelect}
            queryType={queryType}
            pathDepth={pathDepth}
            selectedMethod={selectedMethod}
            onQueryTypeChange={setQueryType}
            onPathDepthChange={setPathDepth}
          />
        </GridItem>

        <GridItem>
          {activeMethodData ? (
            <NodeTree methodData={activeMethodData} onNodeClick={handleNodeClick} />
          ) : (
            <Box
              p={6}
              shadow='lg'
              borderWidth='1px'
              borderRadius='24px'
              bg='white'
              minHeight='420px'
            >
              <Heading size='md' mb={4}>
                方法关系树
              </Heading>
              <Text color='gray.600'>请先从左侧搜索并选择一个方法节点。</Text>
            </Box>
          )}
        </GridItem>

        <GridItem position={{ base: 'static', xl: 'sticky' }} top={{ xl: 6 }} alignSelf='start'>
          <NodeDetails nodeId={selectedNodeId} selectedNode={selectedNode} />
        </GridItem>
      </Grid>
    </Box>
  )
}

export default Dashboard
