package com.example.ordering.service;

import com.example.ordering.dao.UserDao;
import com.example.ordering.domain.Role;
import com.example.ordering.domain.User;
import com.example.ordering.domain.dto.UserSessionDTO;
import com.example.ordering.exception.ApiException;
import com.example.ordering.util.PasswordUtil;
import com.example.ordering.util.ValidationUtil;

public class AuthService {
    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(final UserDao userDao) {
        this.userDao = userDao;
    }

    public UserSessionDTO register(final String username, final String password) {
        String normalizedUsername = ValidationUtil.requireLength("用户名", username, 3, 50);
        if (!normalizedUsername.matches("^[A-Za-z0-9_]+$")) {
            throw new ApiException(400, "VALIDATION_ERROR", "用户名仅支持字母、数字、下划线");
        }
        String normalizedPassword = ValidationUtil.requireLength("密码", password, 6, 64);
        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            throw new ApiException(409, "USERNAME_EXISTS", "用户名已存在");
        }
        String passwordHash = PasswordUtil.hashPassword(normalizedPassword);
        User created = userDao.createUser(normalizedUsername, passwordHash, Role.USER);
        return new UserSessionDTO(created.id(), created.username(), created.role().name());
    }

    public UserSessionDTO login(final String username, final String password) {
        String normalizedUsername = ValidationUtil.requireNonBlank("用户名", username);
        String normalizedPassword = ValidationUtil.requireNonBlank("密码", password);
        User user = userDao.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ApiException(401, "AUTH_FAILED", "用户名或密码错误"));
        if (!PasswordUtil.verifyPassword(normalizedPassword, user.passwordHash())) {
            throw new ApiException(401, "AUTH_FAILED", "用户名或密码错误");
        }
        return new UserSessionDTO(user.id(), user.username(), user.role().name());
    }
}
