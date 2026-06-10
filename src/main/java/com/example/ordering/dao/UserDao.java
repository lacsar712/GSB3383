package com.example.ordering.dao;

import com.example.ordering.config.DataSourceProvider;
import com.example.ordering.domain.Role;
import com.example.ordering.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {
    public Optional<User> findByUsername(final String username) {
        String sql = "SELECT id, username, password_hash, role, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at "
                + "FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query user by username", ex);
        }
    }

    public Optional<User> findById(final long userId) {
        String sql = "SELECT id, username, password_hash, role, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at "
                + "FROM users WHERE id = ? LIMIT 1";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query user by id", ex);
        }
    }

    public User createUser(final String username, final String passwordHash, final Role role) {
        String sql = "INSERT INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role.name());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("User creation succeeded but no generated key returned");
                }
                long id = keys.getLong(1);
                return findById(id).orElseThrow(() -> new IllegalStateException("Created user not found"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create user", ex);
        }
    }

    private User map(final ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromString(rs.getString("role")),
                rs.getString("created_at")
        );
    }
}
