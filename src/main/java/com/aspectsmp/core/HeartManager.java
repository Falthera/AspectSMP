package com.aspectsmp.core;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeartManager {

    private final AspectSMP plugin;
    private final ConcurrentHashMap<UUID, Heart> heartCache;

    public HeartManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.heartCache = new ConcurrentHashMap<>();
    }

    public Optional<Heart> getHeart(UUID playerId) {
        return Optional.ofNullable(heartCache.get(playerId));
    }

    public Heart getOrCreateHeart(UUID playerId) {
        return heartCache.computeIfAbsent(playerId, id -> {
            Heart heart = plugin.getStorageManager().loadHeart(id);
            if (heart == null) {
                heart = new Heart(id, AspectType.INFERNO);
            }
            return heart;
        });
    }

    public Heart getHeart(Player player) {
        return getOrCreateHeart(player.getUniqueId());
    }

    public void saveHeart(Heart heart) {
        plugin.getStorageManager().saveHeart(heart);
    }

    public void saveAll() {
        heartCache.values().forEach(heart -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> 
                plugin.getStorageManager().saveHeart(heart));
        });
    }

    public void removeHeart(UUID playerId) {
        heartCache.remove(playerId);
    }

    public boolean hasHeart(Player player) {
        return heartCache.containsKey(player.getUniqueId());
    }
}