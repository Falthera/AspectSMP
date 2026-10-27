package com.aspectsmp.core;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HeartManager {

    private static final AspectType[] ASPECTS = AspectType.values();

    private final AspectSMP plugin;
    private final ConcurrentHashMap<UUID, CompletableFuture<Heart>> heartCache;
    private final Random random = new Random();

    public HeartManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.heartCache = new ConcurrentHashMap<>();
    }

    public Optional<Heart> getHeart(UUID playerId) {
        CompletableFuture<Heart> future = heartCache.get(playerId);
        if (future != null && future.isDone()) {
            try {
                return Optional.ofNullable(future.getNow(null));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public CompletableFuture<Heart> getOrCreateHeart(UUID playerId) {
        return heartCache.computeIfAbsent(playerId, id -> {
            CompletableFuture<Heart> loadFuture = plugin.getStorageManager().loadHeartAsync(id);
            return loadFuture.thenApplyAsync(heart -> {
                if (heart == null) {
                    AspectType randomAspect = ASPECTS[random.nextInt(ASPECTS.length)];
                    heart = new Heart(id, randomAspect);
                }
                return heart;
            }, plugin.getStorageManager().getAsyncExecutor());
        });
    }

    public CompletableFuture<Heart> getHeart(Player player) {
        return getOrCreateHeart(player.getUniqueId());
    }

    public CompletableFuture<Heart> loadHeartAsync(UUID playerId) {
        return heartCache.computeIfAbsent(playerId, id -> {
            CompletableFuture<Heart> loadFuture = plugin.getStorageManager().loadHeartAsync(id);
            return loadFuture.thenApplyAsync(heart -> {
                if (heart == null) {
                    AspectType randomAspect = ASPECTS[random.nextInt(ASPECTS.length)];
                    heart = new Heart(id, randomAspect);
                }
                return heart;
            }, plugin.getStorageManager().getAsyncExecutor());
        });
    }

    public CompletableFuture<Void> saveHeart(Heart heart) {
        return plugin.getStorageManager().saveHeart(heart);
    }

    public void saveAll() {
        heartCache.values().forEach(future -> {
            future.thenAccept(heart -> {
                if (heart != null) {
                    plugin.getStorageManager().saveHeart(heart);
                }
            });
        });
    }

    public void removeHeart(UUID playerId) {
        heartCache.remove(playerId);
    }

    public boolean hasHeart(Player player) {
        CompletableFuture<Heart> future = heartCache.get(player.getUniqueId());
        return future != null && future.isDone();
    }
}