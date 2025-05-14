package com.example.demo.service;

import com.example.demo.dto.UserDTO;

public interface IUserService {
    UserDTO getUserInfo(Long id);
    void createUser(UserDTO userDTO);
}
