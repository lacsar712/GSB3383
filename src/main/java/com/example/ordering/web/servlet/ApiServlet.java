package com.example.ordering.web.servlet;

import com.example.ordering.domain.Role;
import com.example.ordering.domain.dto.CartDTO;
import com.example.ordering.domain.dto.MenuItemDTO;
import com.example.ordering.domain.dto.OrderDTO;
import com.example.ordering.domain.dto.UserSessionDTO;
import com.example.ordering.exception.ApiException;
import com.example.ordering.service.AuthService;
import com.example.ordering.service.CartService;
import com.example.ordering.service.MenuService;
import com.example.ordering.service.OrderService;
import com.example.ordering.util.JsonUtil;
import com.example.ordering.util.RequestUtil;
import com.example.ordering.util.SessionUtil;
import com.example.ordering.util.ValidationUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/api/*")
public class ApiServlet extends HttpServlet {
    private final AuthService authService = new AuthService();
    private final MenuService menuService = new MenuService();
    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        handle(req, resp);
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        super.service(req, resp);
    }

    private void handle(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String method = req.getMethod();
        String path = RequestUtil.normalizePath(req);
        try {
            if ("POST".equals(method) && "/auth/register".equals(path)) {
                handleRegister(req, resp);
                return;
            }
            if ("POST".equals(method) && "/auth/login".equals(path)) {
                handleLogin(req, resp);
                return;
            }
            if ("POST".equals(method) && "/auth/logout".equals(path)) {
                handleLogout(req, resp);
                return;
            }
            if ("GET".equals(method) && "/menu-items".equals(path)) {
                handleMenuList(req, resp);
                return;
            }
            if ("GET".equals(method) && "/cart".equals(path)) {
                handleGetCart(req, resp);
                return;
            }
            if ("POST".equals(method) && "/cart/items".equals(path)) {
                handleAddCartItem(req, resp);
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/cart/items/")) {
                handleUpdateCartItem(req, resp, path);
                return;
            }
            if ("DELETE".equals(method) && path.startsWith("/cart/items/")) {
                handleDeleteCartItem(req, resp, path);
                return;
            }
            if ("POST".equals(method) && "/orders".equals(path)) {
                handleCreateOrder(req, resp);
                return;
            }
            if ("GET".equals(method) && "/orders".equals(path)) {
                handleListOrders(req, resp);
                return;
            }
            if ("POST".equals(method) && "/admin/menu-items".equals(path)) {
                handleCreateMenuItem(req, resp);
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/admin/menu-items/")) {
                handleUpdateMenuItem(req, resp, path);
                return;
            }
            if ("GET".equals(method) && "/admin/orders".equals(path)) {
                handleAdminListOrders(req, resp);
                return;
            }
            if ("PUT".equals(method) && path.matches("^/admin/orders/\\d+/status$")) {
                handleAdminUpdateOrderStatus(req, resp, path);
                return;
            }

            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "接口不存在");
        } catch (ApiException ex) {
            JsonUtil.writeError(resp, ex.getHttpStatus(), ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_ERROR", "服务器内部错误");
        }
    }

    private void handleRegister(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.parseBody(req);
        String username = getRequiredString(body, "username");
        String password = getRequiredString(body, "password");
        UserSessionDTO user = authService.register(username, password);
        JsonUtil.writeSuccess(resp, user);
    }

    private void handleLogin(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.parseBody(req);
        String username = getRequiredString(body, "username");
        String password = getRequiredString(body, "password");
        UserSessionDTO user = authService.login(username, password);
        SessionUtil.setLoginSession(req, user.id(), user.username(), Role.fromString(user.role()));
        JsonUtil.writeSuccess(resp, user);
    }

    private void handleLogout(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, String> payload = new HashMap<>();
        payload.put("message", "已退出登录");
        JsonUtil.writeSuccess(resp, payload);
    }

    private void handleMenuList(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        Role role = SessionUtil.currentRole(req);
        List<MenuItemDTO> items = menuService.list(req.getParameter("q"), role);
        JsonUtil.writeSuccess(resp, items);
    }

    private void handleGetCart(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        long userId = requireCurrentUserId(req);
        CartDTO cart = cartService.getCart(userId);
        JsonUtil.writeSuccess(resp, cart);
    }

    private void handleAddCartItem(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        long userId = requireCurrentUserId(req);
        JsonObject body = JsonUtil.parseBody(req);
        long menuItemId = getRequiredLong(body, "menuItemId");
        int quantity = body.has("quantity") ? getRequiredInt(body, "quantity") : 1;
        CartDTO cart = cartService.addItem(userId, menuItemId, quantity);
        JsonUtil.writeSuccess(resp, cart);
    }

    private void handleUpdateCartItem(final HttpServletRequest req, final HttpServletResponse resp, final String path)
            throws IOException {
        Long cartItemId = RequestUtil.extractTrailingId(path, "/cart/items");
        if (cartItemId == null) {
            throw new ApiException(400, "VALIDATION_ERROR", "购物车项 ID 不合法");
        }
        JsonObject body = JsonUtil.parseBody(req);
        if (!body.has("quantity")) {
            throw new ApiException(400, "VALIDATION_ERROR", "quantity 不能为空");
        }
        int quantity = getRequiredInt(body, "quantity");
        CartDTO cart = cartService.updateQuantity(requireCurrentUserId(req), cartItemId, quantity);
        JsonUtil.writeSuccess(resp, cart);
    }

    private void handleDeleteCartItem(final HttpServletRequest req, final HttpServletResponse resp, final String path)
            throws IOException {
        Long cartItemId = RequestUtil.extractTrailingId(path, "/cart/items");
        if (cartItemId == null) {
            throw new ApiException(400, "VALIDATION_ERROR", "购物车项 ID 不合法");
        }
        CartDTO cart = cartService.deleteItem(requireCurrentUserId(req), cartItemId);
        JsonUtil.writeSuccess(resp, cart);
    }

    private void handleCreateOrder(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        long userId = requireCurrentUserId(req);
        JsonObject body = JsonUtil.parseBody(req);
        OrderDTO order = orderService.createOrder(
                userId,
                getRequiredString(body, "contactName"),
                getRequiredString(body, "contactPhone"),
                getRequiredString(body, "deliveryAddress")
        );
        JsonUtil.writeSuccess(resp, order);
    }

    private void handleListOrders(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        long userId = requireCurrentUserId(req);
        boolean includeItems = parseBoolean(req.getParameter("includeItems"), false);
        JsonUtil.writeSuccess(resp, orderService.listUserOrders(userId, includeItems));
    }

    private void handleCreateMenuItem(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        JsonObject body = JsonUtil.parseBody(req);
        MenuItemDTO created = menuService.create(
                getRequiredString(body, "name"),
                getRequiredString(body, "description"),
                getRequiredString(body, "price"),
                getRequiredString(body, "imageUrl"),
                body.has("isAvailable") && body.get("isAvailable").getAsBoolean()
        );
        JsonUtil.writeSuccess(resp, created);
    }

    private void handleUpdateMenuItem(final HttpServletRequest req, final HttpServletResponse resp, final String path)
            throws IOException {
        Long menuItemId = RequestUtil.extractTrailingId(path, "/admin/menu-items");
        if (menuItemId == null) {
            throw new ApiException(400, "VALIDATION_ERROR", "菜品 ID 不合法");
        }
        JsonObject body = JsonUtil.parseBody(req);
        MenuItemDTO updated = menuService.update(
                menuItemId,
                getRequiredString(body, "name"),
                getRequiredString(body, "description"),
                getRequiredString(body, "price"),
                getRequiredString(body, "imageUrl"),
                body.has("isAvailable") && body.get("isAvailable").getAsBoolean()
        );
        JsonUtil.writeSuccess(resp, updated);
    }

    private void handleAdminListOrders(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String status = req.getParameter("status");
        boolean includeItems = parseBoolean(req.getParameter("includeItems"), true);
        JsonUtil.writeSuccess(resp, orderService.listAdminOrders(status, includeItems));
    }

    private void handleAdminUpdateOrderStatus(
            final HttpServletRequest req,
            final HttpServletResponse resp,
            final String path
    ) throws IOException {
        Long orderId = RequestUtil.extractIdForStatusPath(path, "/admin/orders");
        if (orderId == null) {
            throw new ApiException(400, "VALIDATION_ERROR", "订单 ID 不合法");
        }
        JsonObject body = JsonUtil.parseBody(req);
        OrderDTO updated = orderService.updateOrderStatus(orderId, getRequiredString(body, "status"));
        JsonUtil.writeSuccess(resp, updated);
    }

    private long requireCurrentUserId(final HttpServletRequest req) {
        Long userId = SessionUtil.currentUserId(req);
        if (userId == null) {
            throw new ApiException(401, "UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    private String getRequiredString(final JsonObject body, final String key) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) {
            throw new ApiException(400, "VALIDATION_ERROR", key + " 不能为空");
        }
        return ValidationUtil.requireNonBlank(key, element.getAsString());
    }

    private int getRequiredInt(final JsonObject body, final String key) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) {
            throw new ApiException(400, "VALIDATION_ERROR", key + " 不能为空");
        }
        try {
            return Integer.parseInt(element.getAsString());
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "VALIDATION_ERROR", key + " 必须是整数");
        }
    }

    private long getRequiredLong(final JsonObject body, final String key) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) {
            throw new ApiException(400, "VALIDATION_ERROR", key + " 不能为空");
        }
        try {
            return Long.parseLong(element.getAsString());
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "VALIDATION_ERROR", key + " 必须是整数");
        }
    }

    private boolean parseBoolean(final String raw, final boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }
}
