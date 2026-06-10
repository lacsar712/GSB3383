package com.example.ordering.util;

import com.example.ordering.domain.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionUtil {
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_ROLE = "role";

    private SessionUtil() {
    }

    public static void setLoginSession(
            final HttpServletRequest request,
            final long userId,
            final String username,
            final Role role
    ) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(KEY_USER_ID, userId);
        session.setAttribute(KEY_USERNAME, username);
        session.setAttribute(KEY_ROLE, role.name());
    }

    public static Long currentUserId(final HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(KEY_USER_ID);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Long value) {
            return value;
        }
        if (raw instanceof Integer value) {
            return value.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String currentUsername(final HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(KEY_USERNAME);
        return raw == null ? null : String.valueOf(raw);
    }

    public static Role currentRole(final HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(KEY_ROLE);
        if (raw == null) {
            return null;
        }
        try {
            return Role.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean isLoggedIn(final HttpServletRequest request) {
        return currentUserId(request) != null;
    }
}
