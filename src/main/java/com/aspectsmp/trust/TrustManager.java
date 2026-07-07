package com.aspectsmp.trust;

import com.aspectsmp.AspectSMP;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrustManager {

    private final AspectSMP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, Set<UUID>> trusts;

    public TrustManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trust.yml");
        this.trusts = new HashMap<>();
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create trust.yml: " + e.getMessage());
            }
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID truster = UUID.fromString(key);
                Set<UUID> trusted = new HashSet<>();
                for (String uuidStr : config.getStringList(key)) {
                    try {
                        trusted.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                trusts.put(truster, trusted);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        config.set("", null);
        for (Map.Entry<UUID, Set<UUID>> entry : trusts.entrySet()) {
            List<String> uuidStrings = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                uuidStrings.add(uuid.toString());
            }
            config.set(entry.getKey().toString(), uuidStrings);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save trust.yml: " + e.getMessage());
        }
    }

    public boolean isTrusted(UUID truster, UUID trusted) {
        if (truster.equals(trusted)) return true;
        Set<UUID> trustedSet = trusts.get(truster);
        return trustedSet != null && trustedSet.contains(trusted);
    }

    public void trust(UUID truster, UUID trusted) {
        trusts.computeIfAbsent(truster, k -> new HashSet<>()).add(trusted);
        save();
    }

    public void untrust(UUID truster, UUID trusted) {
        Set<UUID> trustedSet = trusts.get(truster);
        if (trustedSet != null) {
            trustedSet.remove(trusted);
            if (trustedSet.isEmpty()) {
                trusts.remove(truster);
            }
        }
        save();
    }

    public void clear(UUID truster) {
        trusts.remove(truster);
        save();
    }

    public Set<UUID> getTrusted(UUID truster) {
        Set<UUID> trusted = trusts.get(truster);
        return trusted != null ? new HashSet<>(trusted) : new HashSet<>();
    }

    public Collection<UUID> getOnlineTrusted(UUID truster) {
        Set<UUID> trusted = getTrusted(truster);
        Collection<UUID> online = new ArrayList<>();
        for (UUID uuid : trusted) {
            if (plugin.getServer().getPlayer(uuid) != null) {
                online.add(uuid);
            }
        }
        return online;
    }
}
