import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Button, 
  FormControl, 
  FormLabel, 
  Input, 
  VStack, 
  Heading, 
  useToast,
  InputGroup,
  InputRightElement,
  IconButton,
  Checkbox,
  Flex,
  Spacer
} from '@chakra-ui/react';
import { ViewIcon, ViewOffIcon } from '@chakra-ui/icons';

// 存储到localStorage的key
const STORAGE_KEY = 'nebula_connection_info';

const ConnectionForm = ({ onConnect }) => {
  // 默认连接设置
  const defaultFormData = {
    ip: 'localhost',
    port: 9669,
    username: 'root',
    password: 'nebula',
    space_name: 'code_space',
  };
  
  const [formData, setFormData] = useState(defaultFormData);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [rememberSettings, setRememberSettings] = useState(true);
  const toast = useToast();

  // 当组件加载时，从localStorage中获取保存的连接设置
  useEffect(() => {
    try {
      const savedSettings = localStorage.getItem(STORAGE_KEY);
      if (savedSettings) {
        const parsedSettings = JSON.parse(savedSettings);
        setFormData(prevData => ({
          ...prevData,
          ...parsedSettings
        }));
        toast({
          title: '已加载上次的连接设置',
          status: 'info',
          duration: 2000,
          isClosable: true,
        });
      }
    } catch (error) {
      console.error('加载保存的连接设置失败:', error);
    }
  }, [toast]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (rememberSettings) {
        const settingsToSave = {
          ip: formData.ip,
          port: formData.port,
          username: formData.username,
          space_name: formData.space_name,
          // 根据用户需求，可以选择是否保存密码
          password: formData.password
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settingsToSave));
      }
      
      toast({
        title: '配置已保存',
        description: '当前前端直接请求 Spring Boot 后端，不再单独执行 Nebula 连接',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });
      onConnect?.();
    } catch (error) {
      toast({
        title: '保存失败',
        description: error.message || '无法保存当前配置',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  };

  // 清除保存的设置
  const clearSavedSettings = () => {
    localStorage.removeItem(STORAGE_KEY);
    setFormData(defaultFormData);
    toast({
      title: '已清除保存的连接设置',
      status: 'info',
      duration: 2000,
      isClosable: true,
    });
  };

  return (
    <Box p={5} shadow="md" borderWidth="1px" borderRadius="md" bg="white" width="100%" maxWidth="500px">
      <Heading size="md" mb={4}>后端访问信息</Heading>
      
      <form onSubmit={handleSubmit}>
        <VStack spacing={4}>
          <Box width="100%" fontSize="sm" color="gray.600">
            当前版本由 Spring Boot 后端统一管理图数据库连接。
            这里仅保留历史配置录入能力，不会再调用旧的 `/connect` 接口。
          </Box>

          <FormControl isRequired>
            <FormLabel>IP地址</FormLabel>
            <Input
              name="ip"
              value={formData.ip}
              onChange={handleChange}
              placeholder="数据库IP地址"
            />
          </FormControl>
          
          <FormControl isRequired>
            <FormLabel>端口</FormLabel>
            <Input
              name="port"
              type="number"
              value={formData.port}
              onChange={handleChange}
              placeholder="数据库端口"
            />
          </FormControl>
          
          <FormControl isRequired>
            <FormLabel>用户名</FormLabel>
            <Input
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="用户名"
            />
          </FormControl>
          
          <FormControl isRequired>
            <FormLabel>密码</FormLabel>
            <InputGroup>
              <Input
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={handleChange}
                placeholder="密码"
              />
              <InputRightElement>
                <IconButton
                  icon={showPassword ? <ViewOffIcon /> : <ViewIcon />}
                  onClick={() => setShowPassword(!showPassword)}
                  variant="ghost"
                  aria-label={showPassword ? '隐藏密码' : '显示密码'}
                />
              </InputRightElement>
            </InputGroup>
          </FormControl>
          
          <FormControl isRequired>
            <FormLabel>图空间名称</FormLabel>
            <Input
              name="space_name"
              value={formData.space_name}
              onChange={handleChange}
              placeholder="图空间名称"
            />
          </FormControl>
          
          <FormControl>
            <Checkbox 
              isChecked={rememberSettings}
              onChange={(e) => setRememberSettings(e.target.checked)}
            >
              记住连接设置
            </Checkbox>
          </FormControl>
          
          <Flex width="100%">
            <Button
              mt={2}
              colorScheme="blue"
              isLoading={loading}
              type="submit"
              flex="1"
            >
              保存并继续
            </Button>
            <Spacer mx={2} />
            <Button
              mt={2}
              colorScheme="gray"
              onClick={clearSavedSettings}
              size="md"
            >
              清除保存
            </Button>
          </Flex>
        </VStack>
      </form>
    </Box>
  );
};

export default ConnectionForm; 
