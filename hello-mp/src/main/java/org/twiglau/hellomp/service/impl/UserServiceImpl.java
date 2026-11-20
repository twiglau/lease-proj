package org.twiglau.hellomp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.twiglau.hellomp.entity.User;
import org.twiglau.hellomp.service.UserService;
import org.twiglau.hellomp.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author feiya
* @description 针对表【user】的数据库操作Service实现
* @createDate 2026-11-20 15:24:49
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




