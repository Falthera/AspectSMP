package com.aspectsmp.util;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcePackManager {

    private final AspectSMP plugin;
    private final boolean enabled;
    private final String url;
    private final String expectedSha1;
    private final boolean required;
    private final String promptMessage;
    private final boolean autoDownload;
    
    private final Set<UUID> playersWithPack = ConcurrentHashMap.newKeySet();
    private String cachedSha1 = null;
    private File packFile = null;

    public ResourcePackManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("resource-pack.enabled", true);
        this.url = plugin.getConfig().getString("resource-pack.url", "");
        this.expectedSha1 = plugin.getConfig().getString("resource-pack.sha1", "");
        this.required = plugin.getConfig().getBoolean("resource-pack.required", false);
        this.promptMessage = plugin.getConfig().getString("resource-pack.prompt-message", 
            "§b§lAspect SMP §7requires a resource pack for custom items and effects.");
        this.autoDownload = plugin.getConfig().getBoolean("resource-pack.auto-download", true);
        
if (enabled && (url.isEmpty() || autoDownload)) {
            extractEmbeddedPack();
        }
    }
    
    private void extractEmbeddedPack() {
        try {
            packFile = new File(plugin.getDataFolder(), "AspectSMP-Pack.zip");
            
            if (!packFile.exists()) {
                try (InputStream in = plugin.getResource("AspectSMP-Pack.zip")) {
                    if (in != null) {
                        Files.copy(in, packFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Extracted embedded resource pack to " + packFile.getAbsolutePath());
                    } else {
                        plugin.getLogger().warning("Embedded resource pack not found in JAR!");
                    }
                }
            }
            
            if (packFile.exists()) {
                cachedSha1 = calculateSha1(packFile);
                plugin.getLogger().info("Resource pack SHA-1: " + cachedSha1);
                
                if (!expectedSha1.isEmpty() && !expectedSha1.equalsIgnoreCase(cachedSha1)) {
                    plugin.getLogger().warning("Resource pack SHA-1 mismatch! Expected: " + expectedSha1 + ", Got: " + cachedSha1);
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            plugin.getLogger().severe("Failed to extract resource pack: " + e.getMessage());
        }
    }

    public void onPlayerJoin(Player player) {
        if (!enabled) return;
        
        if (playersWithPack.contains(player.getUniqueId())) {
            return;
        }
        
        if (url.isEmpty() && packFile == null) {
            plugin.getLogger().warning("No resource pack URL or file available!");
            return;
        }
        
        String finalUrl = url.isEmpty() ? getPackDownloadUrl() : url;
        String finalSha1 = expectedSha1.isEmpty() ? cachedSha1 : expectedSha1;
        
        if (finalUrl == null || finalUrl.isEmpty()) {
            plugin.getLogger().warning("Cannot send resource pack - no URL available");
            return;
        }
        
        try {
            player.setResourcePack(finalUrl, finalSha1, required, net.kyori.adventure.text.Component.text(promptMessage));
            playersWithPack.add(player.getUniqueId());
            plugin.getLogger().info("Sent resource pack to " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send resource pack to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private String getPackDownloadUrl() {
        String serverIp = Bukkit.getServer().getIp();
        int serverPort = Bukkit.getServer().getPort();
        
        // If server IP is empty (0.0.0.0), try to get the external IP or use localhost
        if (serverIp == null || serverIp.isEmpty() || serverIp.equals("0.0.0.0")) {
            return "http://localhost:" + serverPort + "/packs/AspectSMP-Pack.zip";
        }
        
        return "http://" + serverIp + ":" + serverPort + "/packs/AspectSMP-Pack.zip";
    }
    
    public void onResourcePackStatus(Player player, String status) {
        switch (status.toLowerCase()) {
            case "successfully_loaded" -> {
                playersWithPack.add(player.getUniqueId());
                plugin.getLogger().info(player.getName() + " loaded resource pack successfully");
            }
            case "declined" -> {
                playersWithPack.remove(player.getUniqueId());
                plugin.getLogger().info(player.getName() + " declined resource pack");
                if (required) {
                    player.kickPlayer("§cThis server requires the Aspect SMP resource pack to play.");
                }
            }
            case "failed_download" -> {
                playersWithPack.remove(player.getUniqueId());
                plugin.getLogger().warning(player.getName() + " failed to download resource pack");
            }
            case "invalid_sha1" -> {
                playersWithPack.remove(player.getUniqueId());
                plugin.getLogger().warning(player.getName() + " got invalid SHA-1 for resource pack");
            }
        }
    }
    
    public void onResourcePackStatusEvent(PlayerResourcePackStatusEvent event) {
        onResourcePackStatus(event.getPlayer(), event.getStatus().name().toLowerCase());
    }
    
    public void sendToAll() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            onPlayerJoin(player);
        }
    }
    
    public void reload() {
        playersWithPack.clear();
        cachedSha1 = null;
        packFile = null;
        
        if (enabled && (url.isEmpty() || autoDownload)) {
            extractEmbeddedPack();
        }
        
        sendToAll();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getPackUrl() {
        return url.isEmpty() ? getPackDownloadUrl() : url;
    }
    
    public String getPackSha1() {
        return expectedSha1.isEmpty() ? cachedSha1 : expectedSha1;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public Set<UUID> getPlayersWithPack() {
        return playersWithPack;
    }
    
    private String calculateSha1(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}