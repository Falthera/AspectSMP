package com.aspectsmp.core;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HeartManager {

    private static final AspectType[] ASPECTS = Arrays.stream(AspectType.values())
            .filter(type -> type != AspectType.NONE)
            .toArray(AspectType[]::new);

    private final AspectSMP plugin;
    private final ConcurrentHashMap<UUID, Heart> heartCache;
    private final Random random = new Random();

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
                AspectType randomAspect = ASPECTS[random.nextInt(ASPECTS.length)];
                heart = new Heart(id, randomAspect);
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
            plugin.getStorageManager().saveHeart(heart);
        });
    }

    public void removeHeart(UUID playerId) {
        heartCache.remove(playerId);
    }

    public boolean hasHeart(Player player) {
        return heartCache.containsKey(player.getUniqueId());
    }
}