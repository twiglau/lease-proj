package org.twiglau.hellomp.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.twiglau.hellomp.entity.User;
import org.twiglau.hellomp.service.UserService;

import java.util.List;
import java.util.function.Consumer;

@SpringBootTest
public class WrapperTest {
    @Autowired
    private UserService userService;

    @Test
    public void testQueryWrapper() throws Exception {
        // 查询name=Jimi的所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", "Jimi");
        List<User> users = userService.list(queryWrapper);
        for (User user : users) {
            System.out.println(user);
        }
        System.out.println("==========================");
        //查询邮箱域名为gmail.com的所有用户
        QueryWrapper<User> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.like("email", "gmail.com");
        List<User> users2 = userService.list(queryWrapper2);
        for (User user : users2) {
            System.out.println(user);
        }
        System.out.println("==========================");
        // 查询所有用户信息，并按照age字段降序排序
        QueryWrapper<User> queryWrapper3 = new QueryWrapper<>();
        queryWrapper3.orderByDesc("age");
        List<User> users3 = userService.list(queryWrapper3);
        for (User user : users3) {
            System.out.println(user);
        }
        System.out.println("==========================");
        // 查询age介于[20,30]的所有用户
        QueryWrapper<User> queryWrapper4 = new QueryWrapper<>();
        queryWrapper4.between("age", 20, 30);
        List<User> users4 = userService.list(queryWrapper4);
        for (User user : users4) {
            System.out.println(user);
        }
        System.out.println("==========================");
        // 查询age小于20或大于30的用户
        QueryWrapper<User> queryWrapper5 = new QueryWrapper<>();
        queryWrapper5.lt("age", 20).or().gt("age", 30);
        List<User> users5 = userService.list(queryWrapper5);
        for (User user : users5) {
            System.out.println(user);
        }
        System.out.println("==========================");
        // 查询邮箱域名为gmail.com 且 年龄 小于30 或 大于40的用户
        QueryWrapper<User> queryWrapper6 = new QueryWrapper<>();
        queryWrapper6
                .like("email", "gmail.com")
                .and(wrapper -> wrapper.lt("age", 30).or().gt("age", 40));

        /**
        queryWrapper6.like("email", "gmail.com").and(new Consumer<QueryWrapper<User>>() {
            @Override
            public void accept(QueryWrapper<User> userQueryWrapper) {
                userQueryWrapper.lt("age", 30).or().gt("age", 40);
            }
        });
         **/




    }

    @Test
    public void testUpdateWrapper() throws Exception {

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("name", "Jimi");
        updateWrapper.set("age", 20);
        userService.update(updateWrapper);
    }
}
