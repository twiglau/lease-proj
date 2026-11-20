package org.twiglau.hellomp.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.twiglau.hellomp.entity.User;
import org.twiglau.hellomp.service.UserService;

@SpringBootTest
public class LambdaWrapperTest {
    @Autowired
    private UserService userService;

    @Test
    public void testQueryWrapper() throws Exception {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, 1);
        User user = userService.getOne(queryWrapper);
        System.out.println(user);
    }

    @Test
    public void testUpdateWrapper() throws Exception {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, 1);
        updateWrapper.set(User::getAge, 2);
        userService.update(updateWrapper);
    }
}
