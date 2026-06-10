package com.example.ordering.service;

import com.example.ordering.config.DataSourceProvider;
import com.example.ordering.dao.CartDao;
import com.example.ordering.dao.OrderDao;
import com.example.ordering.domain.OrderStatus;
import com.example.ordering.domain.dto.CartItemDTO;
import com.example.ordering.domain.dto.OrderDTO;
import com.example.ordering.exception.ApiException;
import com.example.ordering.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final CartDao cartDao;
    private final OrderDao orderDao;

    public OrderService() {
        this(new CartDao(), new OrderDao());
    }

    public OrderService(final CartDao cartDao, final OrderDao orderDao) {
        this.cartDao = cartDao;
        this.orderDao = orderDao;
    }

    public OrderDTO createOrder(
            final long userId,
            final String contactName,
            final String contactPhone,
            final String deliveryAddress
    ) {
        String validName = ValidationUtil.requireLength("联系人", contactName, 1, 50);
        String validPhone = ValidationUtil.requireLength("联系电话", contactPhone, 1, 30);
        String validAddress = ValidationUtil.requireLength("配送地址", deliveryAddress, 1, 255);

        try (Connection conn = DataSourceProvider.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<CartItemDTO> cartItems = cartDao.listByUserForUpdate(conn, userId);
                if (cartItems.isEmpty()) {
                    throw new ApiException(400, "EMPTY_CART", "购物车为空，无法下单");
                }
                for (CartItemDTO item : cartItems) {
                    if (item.quantity() <= 0) {
                        throw new ApiException(400, "INVALID_CART", "购物车数量异常");
                    }
                    if (!item.isAvailable()) {
                        throw new ApiException(400, "MENU_ITEM_UNAVAILABLE", "购物车中存在已下架菜品");
                    }
                }

                BigDecimal total = cartDao.sumTotal(cartItems);
                long orderId = orderDao.createOrder(conn, userId, total, validName, validPhone, validAddress);
                orderDao.insertOrderItems(conn, orderId, cartItems);
                cartDao.clearByUser(conn, userId);
                conn.commit();
                return orderDao.listByUser(userId, true).stream()
                        .filter(order -> order.id() == orderId)
                        .findFirst()
                        .orElseThrow(() -> new ApiException(500, "INTERNAL_ERROR", "订单创建成功但查询失败"));
            } catch (ApiException ex) {
                conn.rollback();
                throw ex;
            } catch (Exception ex) {
                conn.rollback();
                throw new ApiException(500, "INTERNAL_ERROR", "创建订单失败");
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new ApiException(500, "INTERNAL_ERROR", "数据库事务异常");
        }
    }

    public List<OrderDTO> listUserOrders(final long userId, final boolean includeItems) {
        return orderDao.listByUser(userId, includeItems);
    }

    public List<OrderDTO> listAdminOrders(final String status, final boolean includeItems) {
        if (status != null && !status.isBlank()) {
            OrderStatus.fromString(status);
        }
        return orderDao.listForAdmin(status, includeItems);
    }

    public OrderDTO updateOrderStatus(final long orderId, final String nextStatusRaw) {
        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.fromString(ValidationUtil.requireNonBlank("状态", nextStatusRaw));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "INVALID_STATUS", "状态值不合法");
        }
        if (nextStatus == OrderStatus.PLACED) {
            throw new ApiException(400, "INVALID_STATUS", "管理员不能将状态设置为 PLACED");
        }

        OrderStatus current = orderDao.findStatusById(orderId)
                .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "订单不存在"));
        if (!current.canTransitionTo(nextStatus)) {
            throw new ApiException(400, "INVALID_STATUS_TRANSITION",
                    "不允许从 " + current.name() + " 变更为 " + nextStatus.name());
        }
        boolean updated = orderDao.updateStatus(orderId, nextStatus);
        if (!updated) {
            throw new ApiException(404, "NOT_FOUND", "订单不存在");
        }
        return orderDao.listForAdmin(null, true).stream()
                .filter(order -> order.id() == orderId)
                .findFirst()
                .orElseThrow(() -> new ApiException(500, "INTERNAL_ERROR", "订单更新成功但查询失败"));
    }
}
