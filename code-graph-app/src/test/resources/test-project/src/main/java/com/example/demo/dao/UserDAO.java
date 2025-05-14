package com.example.demo.dao;

import com.example.demo.entity.UserDO;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAO {
    
    public UserDO findUserById(Long id) {
        // dummy reads
        UserDO user = new UserDO();
        user.setId(id);
        user.setUsername("test_user");
        user.setEmail("test@example.com");
        return user;
    }

    public void saveUser(UserDO userDO) {
        // dummy writes
        Long id = userDO.getId();
        String username = userDO.getUsername();
        System.out.println("Saving: " + id + ", " + username);
    }
}
