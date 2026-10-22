package com.aspectsmp.util;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicManager {

    private final AspectSMP plugin;
    private final boolean enabled;
    private final float defaultVolume;
    private final int fadeDuration;
    private final boolean crossfadeEnabled;
    private final String defaultPlaylist;
    
    private final Map<String, List<String>> playlists = new ConcurrentHashMap<>();
    private final Map<UUID, MusicSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<String, File> musicFiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private String currentGlobalPlaylist = null;
    private float globalVolume = 1.0f;

    public MusicManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("music.enabled", true);
        this.defaultVolume = (float) plugin.getConfig().getDouble("music.volume", 0.5);
        this.fadeDuration = plugin.getConfig().getInt("music.fade-duration", 3);
        this.crossfadeEnabled = plugin.getConfig().getBoolean("music.crossfade-enabled", true);
        this.defaultPlaylist = plugin.getConfig().getString("music.default-playlist", "ambient");
        
        if (enabled) {
            loadPlaylists();
            extractMusicFiles();
        }
    }
    
    private void loadPlaylists() {
        var playlistsConfig = plugin.getConfig().getConfigurationSection("music.playlists");
        if (playlistsConfig != null) {
            for (String key : playlistsConfig.getKeys(false)) {
                List<String> tracks = playlistsConfig.getStringList(key);
                if (!tracks.isEmpty()) {
                    playlists.put(key, new ArrayList<>(tracks));
                }
            }
        }
        
        if (!playlists.containsKey("ambient")) {
            playlists.put("ambient", List.of("aspect_smp_theme"));
        }
        if (!playlists.containsKey("combat")) {
            playlists.put("combat", List.of("aspect_smp_theme"));
        }
        if (!playlists.containsKey("event")) {
            playlists.put("event", List.of("aspect_smp_theme"));
        }
    }
    
    private void extractMusicFiles() {
        File musicDir = new File(plugin.getDataFolder(), "music");
        if (!musicDir.exists()) musicDir.mkdirs();
        
        String[] musicFiles = {
            "aspect smp theme song.ogg",
            "aspect_smp_part1a_core_themes.zip",
            "aspect_smp_part4_state_tracks_manifest_source.zip",
            "aspect_smp_part3_events_shadows_tempest.zip",
            "aspect_smp_part2_events_ninth_sky_frozen_eclipse.zip",
            "aspect_smp_part1c_aspects_fortune_to_base.zip",
            "aspect_smp_part1b_aspects_inferno_to_vitality.zip"
        };
        
        for (String fileName : musicFiles) {
            File dest = new File(musicDir, fileName);
            if (!dest.exists()) {
                try (InputStream in = plugin.getResource("music/" + fileName)) {
                    if (in != null) {
                        Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        this.musicFiles.put(fileName.replace(".ogg", "").replace(".zip", ""), dest);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to extract music file " + fileName + ": " + e.getMessage());
                }
            } else {
                this.musicFiles.put(fileName.replace(".ogg", "").replace(".zip", ""), dest);
            }
        }
        
        plugin.getLogger().info("Extracted " + this.musicFiles.size() + " music files");
    }
    
    public void playForPlayer(Player player, String trackName) {
        if (!enabled) return;
        
        File file = musicFiles.get(trackName);
        if (file == null || !file.exists()) {
            plugin.getLogger().warning("Music track not found: " + trackName);
            return;
        }
        
        MusicSession session = playerSessions.computeIfAbsent(player.getUniqueId(), k -> new MusicSession(player));
        session.playTrack(file, defaultVolume * globalVolume);
    }
    
    public void playPlaylistForPlayer(Player player, String playlistName) {
        if (!enabled) return;
        
        List<String> tracks = playlists.get(playlistName);
        if (tracks == null || tracks.isEmpty()) {
            tracks = playlists.get("ambient");
        }
        
        MusicSession session = playerSessions.computeIfAbsent(player.getUniqueId(), k -> new MusicSession(player));
        session.playPlaylist(tracks, defaultVolume * globalVolume);
    }
    
    public void stopForPlayer(Player player) {
        MusicSession session = playerSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }
    }
    
    public void pauseForPlayer(Player player) {
        MusicSession session = playerSessions.get(player.getUniqueId());
        if (session != null) {
            session.pause();
        }
    }
    
    public void resumeForPlayer(Player player) {
        MusicSession session = playerSessions.get(player.getUniqueId());
        if (session != null) {
            session.resume();
        }
    }
    
    public void setVolumeForPlayer(Player player, float volume) {
        MusicSession session = playerSessions.get(player.getUniqueId());
        if (session != null) {
            session.setVolume(Math.max(0, Math.min(1, volume)) * globalVolume);
        }
    }
    
    public void playGlobalPlaylist(String playlistName) {
        if (!enabled) return;
        currentGlobalPlaylist = playlistName;
        
        List<String> tracks = playlists.get(playlistName);
        if (tracks == null || tracks.isEmpty()) {
            tracks = playlists.get("ambient");
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            playPlaylistForPlayer(player, playlistName);
        }
    }
    
    public void stopGlobalPlaylist() {
        currentGlobalPlaylist = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopForPlayer(player);
        }
    }
    
    public void setGlobalVolume(float volume) {
        globalVolume = Math.max(0, Math.min(1, volume));
        for (MusicSession session : playerSessions.values()) {
            session.setVolume(session.getBaseVolume() * globalVolume);
        }
    }
    
    public void onPlayerJoin(Player player) {
        if (!enabled) return;
        
        if (currentGlobalPlaylist != null) {
            playPlaylistForPlayer(player, currentGlobalPlaylist);
        } else {
            playPlaylistForPlayer(player, defaultPlaylist);
        }
    }
    
    public void onPlayerQuit(Player player) {
        stopForPlayer(player);
    }
    
    public void reload() {
        stopGlobalPlaylist();
        playerSessions.values().forEach(MusicSession::stop);
        playerSessions.clear();
        musicFiles.clear();
        playlists.clear();
        
        loadPlaylists();
        extractMusicFiles();
        
        if (currentGlobalPlaylist != null) {
            playGlobalPlaylist(currentGlobalPlaylist);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                onPlayerJoin(player);
            }
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        playerSessions.values().forEach(MusicSession::stop);
        playerSessions.clear();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Map<String, List<String>> getPlaylists() {
        return Collections.unmodifiableMap(playlists);
    }
    
    public String getCurrentGlobalPlaylist() {
        return currentGlobalPlaylist;
    }
    
    public float getGlobalVolume() {
        return globalVolume;
    }
    
    private static class MusicSession {
        private final Player player;
        private final Queue<String> playlist = new LinkedList<>();
        private File currentTrack = null;
        private float baseVolume = 0.5f;
        private float currentVolume = 0.5f;
        private boolean paused = false;
        private boolean playing = false;
        private int fadeTaskId = -1;
        
        public MusicSession(Player player) {
            this.player = player;
        }
        
        public void playTrack(File file, float volume) {
            stopCurrentFade();
            currentTrack = file;
            baseVolume = volume;
            currentVolume = volume;
            playing = true;
            paused = false;
            sendPlayPacket(file, volume);
        }
        
        public void playPlaylist(List<String> tracks, float volume) {
            stopCurrentFade();
            playlist.clear();
            playlist.addAll(tracks);
            baseVolume = volume;
            currentVolume = volume;
            playing = true;
            paused = false;
            playNextTrack();
        }
        
        private void playNextTrack() {
            if (playlist.isEmpty()) {
                playlist.addAll(playlists.getOrDefault("ambient", List.of("aspect_smp_theme")));
            }
            String trackName = playlist.poll();
            File file = musicFiles.get(trackName);
            if (file != null && file.exists()) {
                currentTrack = file;
                sendPlayPacket(file, currentVolume);
            } else {
                playNextTrack();
            }
        }
        
        private void sendPlayPacket(File file, float volume) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || !playing) return;
                
                try {
                    String url = "http://" + Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort() 
                        + "/music/" + file.getName();
                    
                    player.stopSound(org.bukkit.Sound.MUSIC_AMBIENT, org.bukkit.SoundCategory.RECORDS);
                    
                    org.bukkit.sound.Sound sound = org.bukkit.sound.Sound.valueOf("ITEM_MUSIC_DISC_13");
                    player.playSound(player.getLocation(), sound, volume, 1.0f);
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to play music for " + player.getName() + ": " + e.getMessage());
                }
            });
        }
        
        public void stop() {
            stopCurrentFade();
            playing = false;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.stopSound(org.bukkit.Sound.MUSIC_AMBIENT, org.bukkit.SoundCategory.RECORDS);
                }
            });
        }
        
        public void pause() {
            paused = true;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.stopSound(org.bukkit.Sound.MUSIC_AMBIENT, org.bukkit.SoundCategory.RECORDS);
                }
            });
        }
        
        public void resume() {
            if (paused && currentTrack != null) {
                paused = false;
                sendPlayPacket(currentTrack, currentVolume);
            }
        }
        
        public void setVolume(float volume) {
            currentVolume = volume;
            if (playing && !paused && currentTrack != null) {
                sendPlayPacket(currentTrack, volume);
            }
        }
        
        public float getBaseVolume() {
            return baseVolume;
        }
        
        private void stopCurrentFade() {
            if (fadeTaskId != -1) {
                plugin.getServer().getScheduler().cancelTask(fadeTaskId);
                fadeTaskId = -1;
            }
        }
    }
}