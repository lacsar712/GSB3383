package com.example.ordering.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DataSourceProvider {
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private DataSourceProvider() {
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(AppConfig.get("DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
        config.setJdbcUrl(AppConfig.get("DB_URL",
                "jdbc:mysql://localhost:3306/online_ordering?useSSL=false&allowPublicKeyRetrieval=true"
                        + "&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8"
                        + "&connectionCollation=utf8mb4_unicode_ci"));
        config.setUsername(AppConfig.get("DB_USER", "ordering"));
        config.setPassword(AppConfig.get("DB_PASSWORD", "ordering123"));
        config.setMaximumPoolSize(AppConfig.getInt("DB_POOL_SIZE", 10));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(60_000);
        config.setInitializationFailTimeout(-1);
        config.setConnectionInitSql("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        config.setPoolName("ordering-pool");
        return new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }
}
