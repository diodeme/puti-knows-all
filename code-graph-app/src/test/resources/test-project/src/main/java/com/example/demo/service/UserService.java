package com.example.demo.service;

import com.example.demo.dao.UserDAO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.UserDO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserService extends BaseUserService implements IUserService {
    
    @Autowired
    private UserDAO userDAO;

    // Mapping tool simulation to trigger maps_to
    public static class BeanUtil {
        public static void copyProperties(Object source, Object target) {
            // fake
        }
    }

    public UserDTO getUserInfo(Long id) {
        UserDO userDO = userDAO.findUserById(id);
        UserDTO userDTO = new UserDTO();
        
        // This triggers MAPS_TO in lineage
        BeanUtil.copyProperties(userDO, userDTO);
        return userDTO;
    }

    public void createUser(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtil.copyProperties(userDTO, userDO);
        // This triggers PASSES_TO in lineage
        userDAO.saveUser(userDO);
    }
}
