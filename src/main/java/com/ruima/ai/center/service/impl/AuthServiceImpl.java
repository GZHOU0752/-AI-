package com.ruima.ai.center.service.impl;

import com.ruima.ai.center.model.entity.User;
import com.ruima.ai.center.repository.UserRepository;
import com.ruima.ai.center.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public Map<String, Object> register(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        // 校验用户名
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }
        username = username.trim();

        // 校验密码
        if (password == null || password.length() != 6) {
            result.put("success", false);
            result.put("message", "密码必须为6位");
            return result;
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        // 保存用户（明文存储，生产环境应使用 BCrypt 加密）
        User user = new User(username, password);
        userRepository.save(user);

        log.info("新用户注册: {}", username);
        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getId().toString());
        result.put("username", user.getUsername());
        return result;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        if (username == null || password == null) {
            result.put("success", false);
            result.put("message", "用户名和密码不能为空");
            return result;
        }

        Optional<User> optUser = userRepository.findByUsername(username.trim());
        if (!optUser.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = optUser.get();
        if (!user.getPassword().equals(password)) {
            result.put("success", false);
            result.put("message", "密码错误");
            return result;
        }

        log.info("用户登录成功: {}", username);
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("userId", user.getId().toString());
        result.put("username", user.getUsername());
        return result;
    }

    @Override
    public Map<String, Object> changePassword(Long userId, String oldPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        if (newPassword == null || newPassword.length() != 6) {
            result.put("success", false);
            result.put("message", "新密码必须为6位");
            return result;
        }
        Optional<User> optUser = userRepository.findById(userId);
        if (!optUser.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        User user = optUser.get();
        if (!user.getPassword().equals(oldPassword)) {
            result.put("success", false);
            result.put("message", "原密码错误");
            return result;
        }
        user.setPassword(newPassword);
        userRepository.save(user);
        log.info("用户密码已修改: {}", user.getUsername());
        result.put("success", true);
        result.put("message", "密码修改成功");
        return result;
    }
}
