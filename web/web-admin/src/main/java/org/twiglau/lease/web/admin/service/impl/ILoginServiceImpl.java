package org.twiglau.lease.web.admin.service.impl;

import org.twiglau.lease.model.entity.SystemUser;
import org.twiglau.lease.web.admin.mapper.SystemUserMapper;
import org.twiglau.lease.web.admin.service.ILoginService;
import org.twiglau.lease.web.admin.vo.login.CaptchaVo;
import org.twiglau.lease.web.admin.vo.login.LoginVo;
import org.twiglau.lease.web.admin.vo.system.user.SystemUserInfoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 登录服务
 *
 * @author 朱俊伟
 * @since 2024/06/15 08:54
 */
@Service
@RequiredArgsConstructor
public class ILoginServiceImpl implements ILoginService {

    private final SystemUserMapper systemUserMapper;

    /**
     * 获取图形验证码
     * @return 图形验证码base64
     */
    @Override
    public CaptchaVo getCaptcha() {
        return null;
    }

    /**
     * 登录
     * @param loginVo 登录信息
     * @return token
     */
    @Override
    public String login(LoginVo loginVo) {
        return null;
    }

    /**
     * 获取登录用户个人信息
     * @param userId 用户id
     * @return 用户信息
     */
    @Override
    public SystemUserInfoVo info(Long userId) {
        SystemUser systemUser = systemUserMapper.selectById(userId);
        SystemUserInfoVo systemUserInfoVo = new SystemUserInfoVo();
        if (systemUser != null) {
            systemUserInfoVo.setName(systemUser.getName());
            systemUserInfoVo.setAvatarUrl(systemUser.getAvatarUrl());
        }
        return systemUserInfoVo;
    }
}
