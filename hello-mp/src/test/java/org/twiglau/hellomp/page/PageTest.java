package org.twiglau.hellomp.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.twiglau.hellomp.entity.User;
import org.twiglau.hellomp.mapper.UserMapper;
import org.twiglau.hellomp.service.UserService;

@SpringBootTest
public class PageTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;

    @Test
    public void testPageService() {
        IPage<User> page = new Page<>(2, 3);
        IPage<User> result = userService.page(page);
        result.getRecords().forEach(System.out::println);
    }

    @Test
    public void testPageMapper() {
        IPage<User> page = new Page<>(2, 3);
        IPage<User> result = userMapper.selectPage(page, null);
        result.getRecords().forEach(System.out::println);
    }

    @Test
    public void testCustomMapper() {
        IPage<User> page = new Page<>(2, 3);
        IPage<User> result = userMapper.selectUserPage(page);
        result.getRecords().forEach(System.out::println);

    }
}
