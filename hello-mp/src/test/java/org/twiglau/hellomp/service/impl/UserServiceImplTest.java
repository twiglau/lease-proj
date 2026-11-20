package org.twiglau.hellomp.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.twiglau.hellomp.entity.User;
import org.twiglau.hellomp.service.UserService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceImplTest {

    @Autowired
    private UserService userService;


    @Test
    public void testGetById() {
        User user = userService.getById(1);
        assertNotNull(user);
    }


    @Test
    public void testSaveOrUpdate() {
        User user = userService.getById(2);
        user.setName("Lucy");

        User user2 = new User();
        user2.setName("Jimi");
        user2.setAge(12);
        user2.setEmail("jimi@email.com");

        userService.saveOrUpdate(user);
        userService.saveOrUpdate(user2);
    }

    @Test
    public void testSaveBatch() {
        User user1 = new User();
        user1.setName("Loes");
        user1.setAge(35);
        user1.setEmail("loes@gmail.com");

        User user2 = new User();
        user2.setName("Africa");
        user2.setAge(33);
        user2.setEmail("africa@gmail.com");
        List<User> users = List.of(user1, user2); // Arrays.asList(user1, user2);
        userService.saveBatch(users);
    }
}