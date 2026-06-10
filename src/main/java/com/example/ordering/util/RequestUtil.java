package com.example.ordering.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtil {
    private RequestUtil() {
    }

    public static String normalizePath(final HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) {
            return "/";
        }
        return pathInfo;
    }

    public static Long extractTrailingId(final String path, final String prefix) {
        if (!path.startsWith(prefix + "/")) {
            return null;
        }
        String tail = path.substring(prefix.length() + 1);
        if (tail.contains("/")) {
            return null;
        }
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Long extractIdForStatusPath(final String path, final String prefix) {
        if (!path.startsWith(prefix + "/") || !path.endsWith("/status")) {
            return null;
        }
        String mid = path.substring(prefix.length() + 1, path.length() - "/status".length());
        if (mid.contains("/")) {
            return null;
        }
        try {
            return Long.parseLong(mid);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
