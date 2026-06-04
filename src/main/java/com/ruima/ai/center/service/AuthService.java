package com.ruima.ai.center.service;

import java.util.Map;

public interface AuthService {

    /**
     * 注册新用户
     */
    Map<String, Object> register(String username, String password);

    /**
     * 登录
     */
    Map<String, Object> login(String username, String password);

    /**
     * 修改密码
     */
    Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword);
}
