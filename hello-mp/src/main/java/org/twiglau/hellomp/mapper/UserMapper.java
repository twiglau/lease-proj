package org.twiglau.hellomp.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.twiglau.hellomp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author feiya
* @description 针对表【user】的数据库操作Mapper
* @createDate 2026-11-20 15:24:49
* @Entity org.twiglau.hellomp.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    IPage<User> selectUserPage(IPage<User> page);
}




