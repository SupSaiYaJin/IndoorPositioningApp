package com.saiya.indoorposapp.tools;

/**
 * 表示服务器鉴权响应码的Enum类
 */
public enum AuthResponse {

    /** 未知错误 */
    UNEXPECTED_ERROR,

    /** 登录成功 */
    LOGIN_SUCCEED,

    /** 用户名不存在 */
    USERNAME_NOT_EXIST,

    /** 密码错误 */
    PASSWORD_ERROR,

    /** 注册成功 */
    REGISTER_SUCCEED,

    /** 用户名重复 */
    DUPLICATE_USERNAME;

    public static AuthResponse getInstance(int responseCode) {
        switch (responseCode) {
            case 0:
                return  LOGIN_SUCCEED;
            case 1:
                return  USERNAME_NOT_EXIST;
            case 2:
                return PASSWORD_ERROR;
            case 3:
                return REGISTER_SUCCEED;
            case 4:
                return DUPLICATE_USERNAME;
            default:
                return UNEXPECTED_ERROR;
        }
    }
}
