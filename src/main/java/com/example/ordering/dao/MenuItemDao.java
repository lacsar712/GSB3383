package com.example.ordering.dao;

import com.example.ordering.config.DataSourceProvider;
import com.example.ordering.domain.dto.MenuItemDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemDao {
    public List<MenuItemDTO> list(final String query, final boolean includeUnavailable) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, name, description, price, image_url, is_available FROM menu_items WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (!includeUnavailable) {
            sql.append(" AND is_available = 1");
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + query.trim() + "%");
        }
        sql.append(" ORDER BY id DESC");

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<MenuItemDTO> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to list menu items", ex);
        }
    }

    public Optional<MenuItemDTO> findById(final long id) {
        String sql = "SELECT id, name, description, price, image_url, is_available FROM menu_items WHERE id = ? LIMIT 1";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query menu item", ex);
        }
    }

    public long create(
            final String name,
            final String description,
            final BigDecimal price,
            final String imageUrl,
            final boolean isAvailable
    ) {
        String sql = "INSERT INTO menu_items (name, description, price, image_url, is_available, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setBigDecimal(3, price);
            stmt.setString(4, imageUrl);
            stmt.setBoolean(5, isAvailable);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Menu item created but no generated key returned");
                }
                return keys.getLong(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create menu item", ex);
        }
    }

    public boolean update(
            final long id,
            final String name,
            final String description,
            final BigDecimal price,
            final String imageUrl,
            final boolean isAvailable
    ) {
        String sql = "UPDATE menu_items SET name = ?, description = ?, price = ?, image_url = ?, is_available = ? WHERE id = ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setBigDecimal(3, price);
            stmt.setString(4, imageUrl);
            stmt.setBoolean(5, isAvailable);
            stmt.setLong(6, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update menu item", ex);
        }
    }

    private MenuItemDTO map(final ResultSet rs) throws SQLException {
        return new MenuItemDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getString("image_url"),
                rs.getBoolean("is_available")
        );
    }
}
