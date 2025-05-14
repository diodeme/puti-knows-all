package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for User operations
 */
@RestController
public class UserController {
    
    @Autowired
    private IUserService userService;

    public UserDTO getUser(Long id) {
        return userService.getUserInfo(id);
    }

    public void registerUser(UserDTO userDTO) {
        // passes_to
        userService.createUser(userDTO);
    }
}
