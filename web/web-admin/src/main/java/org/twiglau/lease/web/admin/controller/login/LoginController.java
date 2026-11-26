package org.twiglau.lease.web.admin.controller.login;


import org.twiglau.lease.login.LoginUser;
import org.twiglau.lease.login.LoginUserHolder;
import org.twiglau.lease.common.result.Result;
import org.twiglau.lease.web.admin.service.ILoginService;
import org.twiglau.lease.web.admin.vo.login.CaptchaVo;
import org.twiglau.lease.web.admin.vo.login.LoginVo;
import org.twiglau.lease.web.admin.vo.system.user.SystemUserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 后台管理系统登录管理
 * </p>
 *
 * @author zjw
 * @since 2024-06-05
 */
@Tag(name = "后台管理系统登录管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class LoginController {

    private final ILoginService loginService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("login/captcha")
    public Result<CaptchaVo> getCaptcha() {
        CaptchaVo captchaVo = loginService.getCaptcha();
        return Result.ok(captchaVo);
    }

    @Operation(summary = "登录")
    @PostMapping("login")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        String token = loginService.login(loginVo);
        return Result.ok(token);
    }

    @Operation(summary = "获取登陆用户个人信息")
    @GetMapping("info")
    public Result<SystemUserInfoVo> info() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        SystemUserInfoVo systemUserInfoVo = loginService.info(loginUser.getUserId());
        return Result.ok(systemUserInfoVo);
    }
}