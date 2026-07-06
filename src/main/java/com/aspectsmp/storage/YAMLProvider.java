package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YAMLProvider implements StorageProvider {

    private YamlConfiguration config;
    private File file;

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
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public CompletableFuture<Void> saveHeart(Heart heart) {
        return CompletableFuture.runAsync(() -> {
            String path = heart.getOwner().toString();
            config.set(path + ".aspect", heart.getAspect().name());
            config.set(path + ".tier", heart.getTier());
            config.set(path + ".stability", heart.getStability());
            config.set(path + ".essence", heart.getEssence());
            config.set(path + ".dormant", heart.isDormant());
            config.set(path + ".cooldowns", heart.getCooldowns());
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Heart loadHeart(UUID playerId) {
        String path = playerId.toString();
        if (!config.isConfigurationSection(path)) {
            return null;
        }
        AspectType aspect = AspectType.fromName(config.getString(path + ".aspect", "INFERNO"));
        int tier = config.getInt(path + ".tier", 1);
        int stability = config.getInt(path + ".stability", 100);
        long essence = config.getLong(path + ".essence", 0);
        boolean dormant = config.getBoolean(path + ".dormant", false);
        
        Heart heart = new Heart(playerId, aspect, tier, stability, essence, dormant);
        heart.getCooldowns().putAll(config.getConfigurationSection(path + ".cooldowns") != null ? 
            config.getConfigurationSection(path + ".cooldowns").getValues(false).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    e -> e.getKey(),
                    e -> ((Number) e.getValue()).longValue())) : 
                new java.util.HashMap<>());
        return heart;
    }

    @Override
    public void shutdown() {
        try {
            if (config != null) {
                config.save(file);
            }
        } catch (IOException ignored) {}
    }
}