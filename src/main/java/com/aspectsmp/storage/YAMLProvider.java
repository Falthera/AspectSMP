package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class YAMLProvider implements StorageProvider {

    private YamlConfiguration config;
    private File file;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<UUID, Heart> cache = new ConcurrentHashMap<>();

    @Override
    public void initialize(AspectSMP plugin) {
        file = new File(plugin.getDataFolder(), "hearts.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create hearts.yml: " + e.getMessage());
            }
        }
        lock.writeLock().lock();
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public CompletableFuture<Void> saveHeart(Heart heart) {
        if (heart == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            String path = heart.getOwner().toString();
            lock.writeLock().lock();
            try {
                config.set(path + ".aspect", heart.getAspect().name());
                config.set(path + ".tier", heart.getTier());
                config.set(path + ".stability", heart.getStability());
                config.set(path + ".essence", heart.getEssence());
                config.set(path + ".dormant", heart.isDormant());
                config.set(path + ".cloud_unlocked", heart.isCloudUnlocked());
                config.set(path + ".winter_unlocked", heart.isWinterUnlocked());

                Map<String, Long> cooldowns = heart.getCooldowns();
                if (cooldowns != null && !cooldowns.isEmpty()) {
                    config.set(path + ".cooldowns", cooldowns);
                } else {
                    config.set(path + ".cooldowns", null);
                }
            } finally {
                lock.writeLock().unlock();
            }

            lock.readLock().lock();
            try {
                config.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save hearts.yml", e);
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public Heart loadHeart(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        lock.readLock().lock();
        try {
            String path = playerId.toString();
            if (!config.isConfigurationSection(path)) {
                return null;
            }

            String aspectName = config.getString(path + ".aspect", "INFERNO");
            AspectType aspect;
            try {
                aspect = AspectType.valueOf(aspectName.toUpperCase());
            } catch (IllegalArgumentException e) {
                aspect = AspectType.INFERNO;
            }

            int tier = config.getInt(path + ".tier", 1);
            int stability = config.getInt(path + ".stability", 100);
            long essence = config.getLong(path + ".essence", 0);
            boolean dormant = config.getBoolean(path + ".dormant", false);
            boolean cloudUnlocked = config.getBoolean(path + ".cloud_unlocked", false);
            boolean winterUnlocked = config.getBoolean(path + ".winter_unlocked", false);

            Heart heart = new Heart(playerId, aspect, tier, stability, essence, dormant);
            heart.setCloudUnlocked(cloudUnlocked);
            heart.setWinterUnlocked(winterUnlocked);

            var cooldownSection = config.getConfigurationSection(path + ".cooldowns");
            if (cooldownSection != null) {
                Map<String, Long> cooldowns = new ConcurrentHashMap<>();
                for (String key : cooldownSection.getKeys(false)) {
                    Object value = cooldownSection.get(key);
                    if (value instanceof Number num) {
                        cooldowns.put(key, num.longValue());
                    }
                }
                heart.getCooldowns().putAll(cooldowns);
            }

            return heart;
        } catch (Exception e) {
            return handleCorruptedData(playerId, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Heart handleCorruptedData(UUID playerId, Exception e) {
        AspectSMP.getInstance().getLogger().log(Level.WARNING, "Corrupted heart data for " + playerId + ", creating new heart", e);
        return new Heart(playerId, AspectType.INFERNO);
    }

    @Override
    public void shutdown() {
        lock.writeLock().lock();
        try {
            if (config != null && file != null) {
                config.save(file);
            }
        } catch (IOException ignored) {
        } finally {
            lock.writeLock().unlock();
        }
    }
}