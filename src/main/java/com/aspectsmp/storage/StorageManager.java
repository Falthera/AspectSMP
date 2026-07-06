package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageManager {

    private final AspectSMP plugin;
    private StorageProvider provider;
    private ExecutorService asyncExecutor;

    public StorageManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        asyncExecutor = Executors.newFixedThreadPool(4);
        
        String type = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        switch (type) {
            case "mysql" -> provider = new MySQLProvider();
            case "yaml" -> provider = new YAMLProvider();
            default -> provider = new SQLiteProvider();
        }
        
        provider.initialize(plugin);
    }

    public void shutdown() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
        if (provider != null) {
            provider.shutdown();
        }
    }

    public CompletableFuture<Void> saveHeart(Heart heart) {
        return CompletableFuture.runAsync(() -> provider.saveHeart(heart), asyncExecutor);
    }

    public Heart loadHeart(UUID playerId) {
        try {
            return provider.loadHeart(playerId).join();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load heart for " + playerId + ": " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<Heart> loadHeartAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> provider.loadHeart(playerId).join(), asyncExecutor);
    }
}