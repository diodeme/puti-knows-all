import React, { useState } from 'react'
import {
  Badge,
  Box,
  Button,
  Collapse,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  FormControl,
  FormLabel,
  Heading,
  HStack,
  Input,
  List,
  ListItem,
  Radio,
  RadioGroup,
  Select,
  Stack,
  Text,
  VStack,
  useDisclosure,
  useToast
} from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { searchMethods } from '../api/api'

const QUERY_TYPE_LABELS = {
  self: '当前节点',
  upstream: '上游节点',
  downstream: '下游树'
}

const MethodSearch = ({
  onMethodSelect,
  queryType,
  pathDepth,
  selectedMethod,
  onQueryTypeChange,
  onPathDepthChange
}) => {
  const [methodName, setMethodName] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { isOpen, onOpen, onClose } = useDisclosure()

  const handleSearch = async () => {
    if (!methodName.trim()) {
      toast({
        title: '请输入方法名',
        status: 'warning',
        duration: 3000,
        isClosable: true
      })
      return
    }

    setLoading(true)

    try {
      const response = await searchMethods(methodName)
      const results = response.data || []
      setSearchResults(results)

      if (results.length === 0) {
        toast({
          title: '未找到结果',
          description: `未找到名称包含 "${methodName}" 的方法`,
          status: 'info',
          duration: 3000,
          isClosable: true
        })
        return
      }

      onOpen()
    } catch (error) {
      toast({
        title: '搜索失败',
        description: error.message || '无法执行搜索',
        status: 'error',
        duration: 5000,
        isClosable: true
      })
    } finally {
      setLoading(false)
    }
  }

  const handleSelectMethod = method => {
    onMethodSelect({
      ...method,
      fullName: method.full_name,
      nodeId: method.node_id,
      pathDepth: queryType === 'self' ? 1 : parseInt(pathDepth, 10)
    })
    onClose()
  }

  return (
    <>
      <Box
        p={5}
        shadow='xl'
        borderWidth='1px'
        borderRadius='24px'
        bg='white'
        width='100%'
        borderColor='gray.200'
      >
        <VStack spacing={5} align='stretch'>
          <Box>
            <Heading size='md' mb={2}>
              搜索方法
            </Heading>
          </Box>

          <FormControl>
            <Input
              placeholder='输入方法名称或完整限定名前缀'
              value={methodName}
              onChange={e => setMethodName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              size='lg'
              fontSize='md'
              py={6}
              width='100%'
              name='methodName'
              autoComplete='on'
              borderRadius='16px'
            />
          </FormControl>

          <HStack spacing={3}>
            <Button
              leftIcon={<SearchIcon />}
              colorScheme='blue'
              isLoading={loading}
              onClick={handleSearch}
              size='lg'
              flex='1'
              borderRadius='16px'
              boxShadow='0 10px 24px rgba(49, 130, 206, 0.18)'
            >
              搜索
            </Button>
            <Button
              variant='outline'
              size='lg'
              borderRadius='16px'
              onClick={onOpen}
              isDisabled={searchResults.length === 0}
            >
              结果 {searchResults.length}
            </Button>
          </HStack>

          <Box
            borderWidth='1px'
            borderRadius='20px'
            p={4}
            bg='linear-gradient(180deg, #f8fbff 0%, #ffffff 100%)'
            borderColor='blue.100'
          >
            <RadioGroup onChange={onQueryTypeChange} value={queryType}>
              <Text fontWeight='bold' mb={3}>
                查询类型
              </Text>
              <Stack direction={{ base: 'column', md: 'row', lg: 'column', xl: 'row' }} spacing={4}>
                <Radio value='self'>当前节点</Radio>
                <Radio value='upstream'>上游节点</Radio>
                <Radio value='downstream'>下游树</Radio>
              </Stack>
            </RadioGroup>

            <Collapse in={queryType === 'downstream' || queryType === 'upstream'} animateOpacity>
              <Box p={3} borderWidth='1px' borderRadius='16px' borderStyle='dashed' mt={4} bg='white'>
                <FormControl>
                  <FormLabel fontWeight='semibold'>路径深度</FormLabel>
                  <Select
                    value={pathDepth}
                    onChange={e => onPathDepthChange(e.target.value)}
                    size='md'
                    borderRadius='12px'
                  >
                    <option value='1'>1级（直接关联）</option>
                    <option value='2'>2级（包含关联节点的关联节点）</option>
                    <option value='3'>3级</option>
                    <option value='5'>5级</option>
                    <option value='10'>10级</option>
                    <option value='-1'>全部（所有可达节点）</option>
                  </Select>
                  <Text fontSize='xs' color='gray.500' mt={2}>
                    深度越大，查询耗时越长，图形也会更复杂。
                  </Text>
                </FormControl>
              </Box>
            </Collapse>
          </Box>

          <Box
            p={4}
            borderWidth='1px'
            borderRadius='20px'
            bg={selectedMethod ? 'white' : 'gray.50'}
            borderColor={selectedMethod ? 'blue.100' : 'gray.200'}
          >
            <HStack justify='space-between' align='start' mb={selectedMethod ? 3 : 0}>
              <Box>
                <Text fontWeight='bold' fontSize='lg'>
                  {selectedMethod ? '当前已选方法' : '搜索结果'}
                </Text>
                <Text fontSize='sm' color='gray.500'>
                  {searchResults.length > 0
                    ? `${searchResults.length} 条结果，可随时打开抽屉切换`
                    : '还没有搜索结果'}
                </Text>
              </Box>
              <VStack spacing={1} align='flex-end'>
                <Badge colorScheme='blue' px={3} py={1} borderRadius='full'>
                  {QUERY_TYPE_LABELS[queryType]}
                </Badge>
                {(queryType === 'upstream' || queryType === 'downstream') && (
                  <Badge colorScheme='gray' px={3} py={1} borderRadius='full'>
                    深度 {pathDepth === '-1' ? 'ALL' : pathDepth}
                  </Badge>
                )}
              </VStack>
            </HStack>

            {selectedMethod ? (
              <Box>
                <Text fontWeight='bold' fontSize='xl' mb={2}>
                  {selectedMethod.name}
                </Text>
                <Text fontSize='sm' color='gray.600' noOfLines={3} mb={3}>
                  {selectedMethod.full_name}
                </Text>
                <HStack spacing={2} wrap='wrap'>
                  <Badge colorScheme='blue'>{selectedMethod.visibility || 'public'}</Badge>
                  {selectedMethod.branch_name && (
                    <Badge colorScheme='orange'>Branch: {selectedMethod.branch_name}</Badge>
                  )}
                  {selectedMethod.repo_id && <Badge colorScheme='purple'>Repo: {selectedMethod.repo_id}</Badge>}
                </HStack>
              </Box>
            ) : (
              <Text color='gray.500'>还没有选中的方法。</Text>
            )}
          </Box>
        </VStack>
      </Box>

      <Drawer isOpen={isOpen} placement='left' onClose={onClose} size='sm'>
        <DrawerOverlay backdropFilter='blur(2px)' />
        <DrawerContent borderTopRightRadius='24px' borderBottomRightRadius='24px'>
          <DrawerCloseButton mt={2} mr={2} />
          <DrawerHeader pt={6} pb={4}>
            <VStack align='stretch' spacing={2}>
              <HStack justify='space-between' pr={8}>
                <Heading size='md'>搜索结果</Heading>
                <Badge colorScheme='blue' px={3} py={1} borderRadius='full'>
                  {searchResults.length} 条
                </Badge>
              </HStack>
              <HStack spacing={2} wrap='wrap'>
                <Badge colorScheme='blue'>{QUERY_TYPE_LABELS[queryType]}</Badge>
                {(queryType === 'upstream' || queryType === 'downstream') && (
                  <Badge colorScheme='gray'>深度 {pathDepth === '-1' ? 'ALL' : pathDepth}</Badge>
                )}
              </HStack>
            </VStack>
          </DrawerHeader>

          <DrawerBody pb={6}>
            <List spacing={3}>
              {searchResults.map((method, index) => (
                <ListItem key={`${method.node_id || method.full_name}-${index}`}>
                  <Box
                    onClick={() => handleSelectMethod(method)}
                    cursor='pointer'
                    p={4}
                    borderWidth='1px'
                    borderRadius='20px'
                    bg='white'
                    borderColor={selectedMethod?.node_id === method.node_id ? 'blue.300' : 'gray.200'}
                    boxShadow={selectedMethod?.node_id === method.node_id ? '0 12px 28px rgba(49,130,206,0.16)' : 'none'}
                    transition='all 0.18s ease'
                    _hover={{
                      borderColor: 'blue.300',
                      boxShadow: '0 16px 36px rgba(15, 23, 42, 0.08)',
                      transform: 'translateY(-1px)'
                    }}
                  >
                    <Text fontWeight='bold' fontSize='xl' lineHeight='1.1' mb={2}>
                      {method.name}
                    </Text>
                    <Text fontSize='sm' color='gray.600' noOfLines={3} mb={3}>
                      {method.full_name}
                    </Text>
                    {method.node_id && (
                      <Text fontSize='xs' color='gray.400' mb={3}>
                        ID: {method.node_id}
                      </Text>
                    )}
                    <HStack spacing={2} wrap='wrap'>
                      <Badge colorScheme='blue'>{method.visibility || 'public'}</Badge>
                      {method.branch_name && <Badge colorScheme='orange'>Branch: {method.branch_name}</Badge>}
                      {method.repo_id && <Badge colorScheme='purple'>Repo: {method.repo_id}</Badge>}
                    </HStack>
                  </Box>
                </ListItem>
              ))}
            </List>
          </DrawerBody>
        </DrawerContent>
      </Drawer>
    </>
  )
}

export default MethodSearch
