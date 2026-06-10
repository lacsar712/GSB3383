package com.example.ordering.dao;

import com.example.ordering.config.DataSourceProvider;
import com.example.ordering.domain.OrderStatus;
import com.example.ordering.domain.dto.CartItemDTO;
import com.example.ordering.domain.dto.OrderDTO;
import com.example.ordering.domain.dto.OrderItemDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderDao {
    public long createOrder(
            final Connection conn,
            final long userId,
            final BigDecimal totalAmount,
            final String contactName,
            final String contactPhone,
            final String deliveryAddress
    ) throws SQLException {
        String sql = "INSERT INTO orders (user_id, status, total_amount, contact_name, contact_phone, delivery_address, created_at, updated_at) "
                + "VALUES (?, 'PLACED', ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setBigDecimal(2, totalAmount);
            stmt.setString(3, contactName);
            stmt.setString(4, contactPhone);
            stmt.setString(5, deliveryAddress);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Order created but no generated key returned");
                }
                return keys.getLong(1);
            }
        }
    }

    public void insertOrderItems(final Connection conn, final long orderId, final List<CartItemDTO> cartItems) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (CartItemDTO item : cartItems) {
                stmt.setLong(1, orderId);
                stmt.setLong(2, item.menuItemId());
                stmt.setString(3, item.name());
                stmt.setBigDecimal(4, item.unitPrice());
                stmt.setInt(5, item.quantity());
                stmt.setBigDecimal(6, item.lineTotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<OrderDTO> listByUser(final long userId, final boolean includeItems) {
        String sql = "SELECT o.id, o.user_id, u.username, o.status, o.total_amount, o.contact_name, o.contact_phone, o.delivery_address, "
                + "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') AS created_at, "
                + "DATE_FORMAT(o.updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at "
                + "FROM orders o JOIN users u ON u.id = o.user_id "
                + "WHERE o.user_id = ? ORDER BY o.created_at DESC";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapOrders(rs, includeItems, conn);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list user orders", ex);
        }
    }

    public List<OrderDTO> listForAdmin(final String status, final boolean includeItems) {
        StringBuilder sql = new StringBuilder(
                "SELECT o.id, o.user_id, u.username, o.status, o.total_amount, o.contact_name, o.contact_phone, o.delivery_address, "
                        + "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') AS created_at, "
                        + "DATE_FORMAT(o.updated_at, '%Y-%m-%d %H:%i:%s') AS updated_at "
                        + "FROM orders o JOIN users u ON u.id = o.user_id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND o.status = ?");
            params.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY o.created_at DESC");

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return mapOrders(rs, includeItems, conn);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list admin orders", ex);
        }
    }

    public Optional<OrderStatus> findStatusById(final long orderId) {
        String sql = "SELECT status FROM orders WHERE id = ? LIMIT 1";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(OrderStatus.fromString(rs.getString("status")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query order status", ex);
        }
    }

    public boolean updateStatus(final long orderId, final OrderStatus status) {
        String sql = "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update order status", ex);
        }
    }

    private List<OrderDTO> mapOrders(final ResultSet rs, final boolean includeItems, final Connection conn) throws SQLException {
        List<OrderDTO> orders = new ArrayList<>();
        List<Long> orderIds = new ArrayList<>();
        while (rs.next()) {
            long id = rs.getLong("id");
            orderIds.add(id);
            orders.add(new OrderDTO(
                    id,
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getString("status"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("contact_name"),
                    rs.getString("contact_phone"),
                    rs.getString("delivery_address"),
                    rs.getString("created_at"),
                    rs.getString("updated_at"),
                    Collections.emptyList()
            ));
        }
        if (!includeItems || orderIds.isEmpty()) {
            return orders;
        }

        Map<Long, List<OrderItemDTO>> itemsByOrder = loadItemsByOrderIds(conn, orderIds);
        List<OrderDTO> merged = new ArrayList<>();
        for (OrderDTO order : orders) {
            merged.add(new OrderDTO(
                    order.id(),
                    order.userId(),
                    order.username(),
                    order.status(),
                    order.totalAmount(),
                    order.contactName(),
                    order.contactPhone(),
                    order.deliveryAddress(),
                    order.createdAt(),
                    order.updatedAt(),
                    itemsByOrder.getOrDefault(order.id(), Collections.emptyList())
            ));
        }
        return merged;
    }

    private Map<Long, List<OrderItemDTO>> loadItemsByOrderIds(final Connection conn, final List<Long> orderIds) throws SQLException {
        String placeholders = String.join(",", orderIds.stream().map(id -> "?").toList());
        String sql = "SELECT id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, line_total "
                + "FROM order_items WHERE order_id IN (" + placeholders + ") ORDER BY id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < orderIds.size(); i++) {
                stmt.setLong(i + 1, orderIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                Map<Long, List<OrderItemDTO>> map = new HashMap<>();
                while (rs.next()) {
                    long orderId = rs.getLong("order_id");
                    map.computeIfAbsent(orderId, key -> new ArrayList<>()).add(new OrderItemDTO(
                            rs.getLong("id"),
                            rs.getLong("menu_item_id"),
                            rs.getString("item_name_snapshot"),
                            rs.getBigDecimal("unit_price_snapshot"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("line_total")
                    ));
                }
                return map;
            }
        }
    }
}
