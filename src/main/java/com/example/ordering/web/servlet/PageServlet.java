package com.example.ordering.web.servlet;

import com.example.ordering.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = {
        "",
        "/login",
        "/register",
        "/menu",
        "/cart",
        "/orders",
        "/admin/menu",
        "/admin/orders"
})
public class PageServlet extends HttpServlet {
    private static final Map<String, String> VIEW_MAP = new HashMap<>();

    static {
        VIEW_MAP.put("/login", "/WEB-INF/views/login.jsp");
        VIEW_MAP.put("/register", "/WEB-INF/views/register.jsp");
        VIEW_MAP.put("/menu", "/WEB-INF/views/menu.jsp");
        VIEW_MAP.put("/cart", "/WEB-INF/views/cart.jsp");
        VIEW_MAP.put("/orders", "/WEB-INF/views/orders.jsp");
        VIEW_MAP.put("/admin/menu", "/WEB-INF/views/admin-menu.jsp");
        VIEW_MAP.put("/admin/orders", "/WEB-INF/views/admin-orders.jsp");
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            if (SessionUtil.isLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/menu");
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }

        String view = VIEW_MAP.get(path);
        if (view == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.setAttribute("currentUsername", SessionUtil.currentUsername(req));
        req.setAttribute("currentRole", SessionUtil.currentRole(req) == null ? null : SessionUtil.currentRole(req).name());
        req.setAttribute("currentUserId", SessionUtil.currentUserId(req));
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
