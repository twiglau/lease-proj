package org.twiglau.hellomp.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.twiglau.hellomp.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserMapperTest {
    @Autowired
   private UserMapper userMapper;

    @Test
    public void testList() {
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
    }

    @Test
    public void testFindById() {
        User user = userMapper.selectById(1);
        assertNotNull(user);
        System.out.println(user);
    }

    @Test
    public void testInsert() {
        User user = new User();
        user.setId(6L);
        user.setName("Lau");
        user.setAge(24);
        user.setEmail("lau@gmail.com");
        userMapper.insert(user);

    }
}