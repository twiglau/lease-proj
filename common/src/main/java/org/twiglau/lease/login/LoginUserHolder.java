package org.twiglau.lease.login;

/**
 * 基于 ThreadLocal 保存当前请求的登录用户
 */
public class LoginUserHolder {

    private static final ThreadLocal<LoginUser> LOGIN_USER_HOLDER = new ThreadLocal<>();

    public static void setLoginUser(LoginUser loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    public static LoginUser getLoginUser() {
        return LOGIN_USER_HOLDER.get();
    }

    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }
}
