package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteProvider implements StorageProvider {

    private HikariDataSource dataSource;

    @Override
    public void initialize(AspectSMP plugin) {
        try {
            com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db");
            config.setMaximumPoolSize(10);
            dataSource = new HikariDataSource(config);
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS hearts (
                    uuid TEXT PRIMARY KEY,
                    aspect TEXT NOT NULL,
                    tier INTEGER NOT NULL,
                    stability INTEGER NOT NULL,
                    essence INTEGER NOT NULL,
                    dormant INTEGER NOT NULL,
                    cooldowns TEXT
                )
                """);
        }
    }

    @Override
    public CompletableFuture<Void> saveHeart(Heart heart) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO hearts (uuid, aspect, tier, stability, essence, dormant, cooldowns) VALUES (?, ?, ?, ?, ?, ?, ?)");
                stmt.setString(1, heart.getOwner().toString());
                stmt.setString(2, heart.getAspect().name());
                stmt.setInt(3, heart.getTier());
                stmt.setInt(4, heart.getStability());
                stmt.setLong(5, heart.getEssence());
                stmt.setInt(6, heart.isDormant() ? 1 : 0);
                stmt.setString(7, serializeCooldowns(heart.getCooldowns()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Heart> loadHeart(UUID playerId) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM hearts WHERE uuid = ?");
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                AspectType aspect = AspectType.fromName(rs.getString("aspect"));
                int tier = rs.getInt("tier");
                int stability = rs.getInt("stability");
                long essence = rs.getLong("essence");
                boolean dormant = rs.getInt("dormant") == 1;
                
                Heart heart = new Heart(playerId, aspect, tier, stability, essence, dormant);
                heart.getCooldowns().putAll(deserializeCooldowns(rs.getString("cooldowns")));
                return CompletableFuture.completedFuture(heart);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
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