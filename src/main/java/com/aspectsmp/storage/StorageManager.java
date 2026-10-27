package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StorageManager {

    private final AspectSMP plugin;
    private StorageProvider provider;
    private ExecutorService asyncExecutor;
    private String currentStorageType;

    public StorageManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        initializeExecutor();
        currentStorageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        createProvider(currentStorageType);
        provider.initialize(plugin);
    }

    private void initializeExecutor() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            shutdownExecutor();
        }
        asyncExecutor = new ThreadPoolExecutor(
            4,
            8,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void createProvider(String type) {
        switch (type) {
            case "mysql" -> provider = new MySQLProvider();
            case "yaml" -> provider = new YAMLProvider();
            default -> provider = new SQLiteProvider();
        }
    }

    public void reinitialize() {
        String newStorageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        boolean typeChanged = !newStorageType.equals(currentStorageType);

        if (provider != null) {
            provider.shutdown();
        }

        if (typeChanged) {
            plugin.getLogger().info("Storage type changed from " + currentStorageType + " to " + newStorageType + ", reconnecting...");
        }

        initializeExecutor();
        currentStorageType = newStorageType;
        createProvider(currentStorageType);
        provider.initialize(plugin);
    }

    public void shutdown() {
        shutdownExecutor();
        if (provider != null) {
            provider.shutdown();
        }
    }

    private void shutdownExecutor() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                    if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        plugin.getLogger().warning("Async executor did not terminate properly");
                    }
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                plugin.getLogger().log(Level.WARNING, "Interrupted while waiting for executor termination", e);
            }
        }
    }

    public CompletableFuture<Void> saveHeart(Heart heart) {
        if (asyncExecutor == null || asyncExecutor.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage executor is shutdown"));
        }
        return CompletableFuture.runAsync(() -> {
            try {
                provider.saveHeart(heart);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save heart for " + heart.getOwner(), e);
                throw new RuntimeException(e);
            }
        }, asyncExecutor).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Save heart future failed for " + heart.getOwner(), throwable);
            return null;
        });
    }

    public Heart loadHeart(UUID playerId) {
        return provider.loadHeart(playerId);
    }

    public CompletableFuture<Heart> loadHeartAsync(UUID playerId) {
        if (asyncExecutor == null || asyncExecutor.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage executor is shutdown"));
        }
        return CompletableFuture.supplyAsync(() -> provider.loadHeart(playerId), asyncExecutor)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Load heart async failed for " + playerId, throwable);
                return null;
            });
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }
}