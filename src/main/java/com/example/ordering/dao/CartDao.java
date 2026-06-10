package com.example.ordering.dao;

import com.example.ordering.config.DataSourceProvider;
import com.example.ordering.domain.dto.CartItemDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartDao {
    public List<CartItemDTO> listByUser(final long userId) {
        try (Connection conn = DataSourceProvider.getConnection()) {
            return listByUser(conn, userId, false);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list cart items", ex);
        }
    }

    public List<CartItemDTO> listByUserForUpdate(final Connection conn, final long userId) {
        return listByUser(conn, userId, true);
    }

    private List<CartItemDTO> listByUser(final Connection conn, final long userId, final boolean forUpdate) {
        String sql = "SELECT c.id, c.menu_item_id, m.name, m.price, c.quantity, m.image_url, m.is_available, "
                + "(m.price * c.quantity) AS line_total "
                + "FROM cart_items c "
                + "JOIN menu_items m ON m.id = c.menu_item_id "
                + "WHERE c.user_id = ? "
                + "ORDER BY c.id DESC"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<CartItemDTO> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new CartItemDTO(
                            rs.getLong("id"),
                            rs.getLong("menu_item_id"),
                            rs.getString("name"),
                            rs.getBigDecimal("price"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("line_total"),
                            rs.getString("image_url"),
                            rs.getBoolean("is_available")
                    ));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query cart", ex);
        }
    }

    public void addOrIncrease(final long userId, final long menuItemId, final int quantity) {
        String sql = "INSERT INTO cart_items (user_id, menu_item_id, quantity, created_at) VALUES (?, ?, ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, menuItemId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to add cart item", ex);
        }
    }

    public boolean updateQuantity(final long userId, final long cartItemId, final int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setLong(2, cartItemId);
            stmt.setLong(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update cart item", ex);
        }
    }

    public boolean deleteById(final long userId, final long cartItemId) {
        String sql = "DELETE FROM cart_items WHERE id = ? AND user_id = ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, cartItemId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete cart item", ex);
        }
    }

    public void clearByUser(final long userId) {
        try (Connection conn = DataSourceProvider.getConnection()) {
            clearByUser(conn, userId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to clear cart", ex);
        }
    }

    public void clearByUser(final Connection conn, final long userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to clear cart", ex);
        }
    }

    public BigDecimal sumTotal(final List<CartItemDTO> items) {
        return items.stream()
                .map(CartItemDTO::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
