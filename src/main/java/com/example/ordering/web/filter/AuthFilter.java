package com.example.ordering.web.filter;

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
import java.util.Set;

@WebFilter(urlPatterns = {"/menu", "/cart", "/orders", "/admin/*", "/api/*"})
public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String requestUri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
        if (PUBLIC_API_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (!SessionUtil.isLoggedIn(req)) {
            if (path.startsWith("/api/")) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
