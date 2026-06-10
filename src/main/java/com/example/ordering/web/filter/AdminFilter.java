package com.example.ordering.web.filter;

import com.example.ordering.domain.Role;
import com.example.ordering.util.JsonUtil;
import com.example.ordering.util.SessionUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = {"/admin/*", "/api/admin/*"})
public class AdminFilter implements Filter {
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (!SessionUtil.isLoggedIn(req)) {
            if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }

        Role role = SessionUtil.currentRole(req);
        if (role != Role.ADMIN) {
            if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "仅管理员可访问");
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "仅管理员可访问");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
