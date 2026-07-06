package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariConfig;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLProvider implements StorageProvider {

    private HikariDataSource dataSource;

    @Override
    public void initialize(AspectSMP plugin) {
        try {
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "aspectsmp");
            String username = plugin.getConfig().getString("storage.mysql.username", "root");
            String password = plugin.getConfig().getString("storage.mysql.password", "");
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(20);
            dataSource = new HikariDataSource(config);
            
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS hearts (uuid VARCHAR(36) PRIMARY KEY, aspect VARCHAR(32), tier INT, stability INT, essence BIGINT, dormant BOOLEAN, cooldowns TEXT)")) {
            stmt.executeUpdate();
        }
    }

    @Override
    public CompletableFuture<Void> saveHeart(Heart heart) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO hearts (uuid, aspect, tier, stability, essence, dormant, cooldowns) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE aspect=?, tier=?, stability=?, essence=?, dormant=?, cooldowns=?");
                stmt.setString(1, heart.getOwner().toString());
                stmt.setString(2, heart.getAspect().name());
                stmt.setInt(3, heart.getTier());
                stmt.setInt(4, heart.getStability());
                stmt.setLong(5, heart.getEssence());
                stmt.setBoolean(6, heart.isDormant());
                stmt.setString(7, serializeCooldowns(heart.getCooldowns()));
                stmt.setString(8, heart.getAspect().name());
                stmt.setInt(9, heart.getTier());
                stmt.setInt(10, heart.getStability());
                stmt.setLong(11, heart.getEssence());
                stmt.setBoolean(12, heart.isDormant());
                stmt.setString(13, serializeCooldowns(heart.getCooldowns()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Heart> loadHeart(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM hearts WHERE uuid = ?");
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    AspectType aspect = AspectType.fromName(rs.getString("aspect"));
                    int tier = rs.getInt("tier");
                    int stability = rs.getInt("stability");
                    long essence = rs.getLong("essence");
                    boolean dormant = rs.getBoolean("dormant");
                    
                    Heart heart = new Heart(playerId, aspect, tier, stability, essence, dormant);
                    heart.getCooldowns().putAll(deserializeCooldowns(rs.getString("cooldowns")));
                    return heart;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private String serializeCooldowns(java.util.Map<String, Long> cooldowns) {
        StringBuilder sb = new StringBuilder();
        cooldowns.forEach((k, v) -> sb.append(k).append(":").append(v).append(";"));
        return sb.toString();
    }

    private java.util.Map<String, Long> deserializeCooldowns(String data) {
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        if (data == null || data.isEmpty()) return result;
        for (String entry : data.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                result.put(parts[0], Long.parseLong(parts[1]));
            }
        }
        return result;
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}