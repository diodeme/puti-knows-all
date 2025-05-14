import React, { useState } from 'react'
import {
  ChakraProvider,
  extendTheme,
  Box,
  Tabs,
  TabList,
  TabPanels,
  Tab,
  TabPanel
} from '@chakra-ui/react'

import Dashboard from './pages/Dashboard'
import ManualPage from './pages/ManualPage'

// 自定义主题
const theme = extendTheme({
  fonts: {
    heading: `'Roboto', sans-serif`,
    body: `'Roboto', sans-serif`
  },
  colors: {
    brand: {
      50: '#e5f6ff',
      100: '#b3e0ff',
      200: '#80ccff',
      300: '#4db8ff',
      400: '#1aa3ff',
      500: '#0096ff', // 主色调
      600: '#0077cc',
      700: '#005999',
      800: '#003a66',
      900: '#001c33'
    }
  }
})

function App () {
  // 当前选中的Tab索引
  const [tabIndex, setTabIndex] = useState(0)

  return (
    <ChakraProvider theme={theme}>
      <Box minHeight='100vh'>
        <Tabs
          index={tabIndex}
          onChange={setTabIndex}
          variant='enclosed'
          colorScheme='brand'
        >
          <TabList>
            <Tab>码灵控制台</Tab>
            <Tab>使用说明书</Tab>
          </TabList>

          <TabPanels>
            <TabPanel p={0}>
              <Dashboard />
            </TabPanel>
            <TabPanel p={0}>
              <ManualPage />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </Box>
    </ChakraProvider>
  )
}

export default App
