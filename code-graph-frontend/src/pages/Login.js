import React from 'react';
import { Box, Flex, Heading, Text, useColorModeValue } from '@chakra-ui/react';
import ConnectionForm from '../components/ConnectionForm';

const Login = ({ onLogin }) => {
  const bgColor = useColorModeValue('gray.50', 'gray.900');
  
  return (
    <Flex 
      minHeight="100vh" 
      width="full" 
      align="center" 
      justifyContent="center"
      bg={bgColor}
    >
      <Box textAlign="center" mx={2}>
        <Box mb={8}>
          <Heading as="h1" size="xl">码灵控制台</Heading>
          <Text mt={4} color="gray.600">
            当前前端直接调用 Spring Boot 接口，可视化查看方法依赖关系
          </Text>
        </Box>
        
        <Flex 
          direction={{ base: "column", md: "row" }} 
          justify="center" 
          align="center"
          gap={8}
        >
          <ConnectionForm onConnect={onLogin} />
          
          {/* <Image
            src="/logo.png"
            alt="码灵 Logo"
            fallbackSrc="https://via.placeholder.com/400x300?text=码灵"
            maxWidth="250px"
            mx="auto"
            my={4}
          /> */}
        </Flex>
      </Box>
    </Flex>
  );
};

export default Login; 
