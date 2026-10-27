package com.aspectsmp.visual;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.event.EventPhase;
import com.aspectsmp.visual.AspectVisualThemes.Theme;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DynamicSoundscapes {

    private final AspectSMP plugin;
    private final Map<UUID, Map<String, BukkitTask>> activeSoundscapes = new ConcurrentHashMap<>();

    public DynamicSoundscapes(AspectSMP plugin) {
        this.plugin = plugin;
    }

    private long getSoundSeed(Player player) {
        return player.getUniqueId().getMostSignificantBits() ^ System.currentTimeMillis();
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("visual-effects.sound-seed-enabled", true)) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } else {
            long seed = getSoundSeed(player);
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch, getSoundSeed(player));
        }
    }

    private void playSound(Location location, Sound sound, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("visual-effects.sound-seed-enabled", true)) {
            location.getWorld().playSound(location, sound, volume, pitch);
        } else {
            long seed = location.getWorld().getSeed() ^ System.currentTimeMillis();
            location.getWorld().playSound(location, sound, SoundCategory.MASTER, volume, pitch, seed);
        }
    }

    private void playSound(Player player, Sound sound, SoundCategory category, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("visual-effects.sound-seed-enabled", true)) {
            player.playSound(player.getLocation(), sound, category, volume, pitch);
        } else {
            long seed = getSoundSeed(player);
            player.playSound(player.getLocation(), sound, category, volume, pitch, getSoundSeed(player));
        }
    }

    private void playSound(Player player, Sound sound, float volume, float pitch, long seed) {
        player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch, seed);
    }

    private void playSound(Player player, Location location, Sound sound, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("visual-effects.sound-seed-enabled", true)) {
            location.getWorld().playSound(location, sound, volume, pitch);
        } else {
            long seed = location.getWorld().getSeed() ^ System.currentTimeMillis();
            location.getWorld().playSound(location, sound, SoundCategory.MASTER, volume, pitch, seed);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // AMBIENT SOUNDSCAPES - Persistent per aspect
    // ════════════════════════════════════════════════════════════════════

    public void startAmbientSoundscape(Player player, Theme theme, int stability) {
        UUID id = player.getUniqueId();
        stopAmbientSoundscape(player);

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }

                // Base ambient sound (low frequency)
                if (tick % 100 == 0) { // Every 5 seconds
                    float volume = 0.15f + (100 - stability) / 1000.0f;
                    float pitch = 0.5f + stability / 200.0f;
                    playSound(player, theme.getAmbientSound(), volume, pitch);
                }

                // Stability-based layers
                if (stability < 50 && tick % 40 == 0) { // Every 2 seconds
                    // Warning hum
                    playSound(player, Sound.BLOCK_BEACON_AMBIENT, 0.1f, 0.3f + (50 - stability) / 100.0f);
                }

                if (stability < 20 && tick % 20 == 0) { // Every second
                    // Critical heartbeat
                    playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.5f + (20 - stability) / 40.0f);
                }

                if (stability >= 100 && tick % 60 == 0) { // Every 3 seconds
                    // Awakened resonance
                    playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.2f, 1.5f);
                }

                // Aspect-specific ambient layers
                playAspectAmbient(player, theme, stability, tick);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeSoundscapes.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put("ambient", task);
    }

    private void playAspectAmbient(Player player, Theme theme, int stability, int tick) {
        AspectType aspect = theme.getAspect();
        Location loc = player.getLocation();

        switch (aspect) {
            case INFERNO -> {
                if (tick % 80 == 0) playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.1f, 1.0f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 30 == 0) playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.15f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
            }
            case TIDE -> {
                if (tick % 60 == 0) playSound(loc, Sound.BLOCK_WATER_AMBIENT, 0.1f, 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f);
                if (stability < 30 && tick % 25 == 0) playSound(loc, Sound.ENTITY_GUARDIAN_AMBIENT, 0.15f, 0.7f);
            }
            case TEMPEST -> {
                if (tick % 100 == 0) playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 20 == 0) playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat());
            }
            case RIFT -> {
                if (tick % 120 == 0) playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 30 == 0) playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.15f, 0.3f + ThreadLocalRandom.current().nextFloat() * 0.5f);
            }
            case VITALITY -> {
                if (tick % 80 == 0) playSound(loc, Sound.BLOCK_DRY_GRASS_AMBIENT, 0.1f, 1.0f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability >= 100 && tick % 60 == 0) playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.15f, 1.5f);
            }
            case WAR -> {
                if (tick % 100 == 0) playSound(loc, Sound.ENTITY_IRON_GOLEM_STEP, 0.08f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 20 == 0) playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.1f, 0.3f + ThreadLocalRandom.current().nextFloat() * 0.4f);
            }
            case COSMOS -> {
                if (tick % 150 == 0) playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.1f, 0.3f + ThreadLocalRandom.current().nextFloat() * 0.4f);
                if (stability >= 100 && tick % 80 == 0) playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.15f, 0.5f);
            }
            case FORTUNE -> {
                if (tick % 80 == 0) playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.08f, 1.0f + ThreadLocalRandom.current().nextFloat() * 1.0f);
                if (stability >= 100 && tick % 60 == 0) playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.15f, 2.0f);
            }
            case CLOUD -> {
                if (tick % 80 == 0) playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 30 == 0) playSound(loc, Sound.ENTITY_BREEZE_INHALE, 0.15f, 0.5f);
            }
            case WINTER -> {
                if (tick % 100 == 0) playSound(loc, Sound.BLOCK_SNOW_STEP, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 25 == 0) playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
            }
            case SHADOW -> {
                if (tick % 120 == 0) playSound(loc, Sound.BLOCK_SCULK_BREAK, 0.08f, 0.3f + ThreadLocalRandom.current().nextFloat() * 0.4f);
                if (stability < 30 && tick % 20 == 0) playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.15f, 0.5f);
            }
            case STORM -> {
                if (tick % 80 == 0) playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, 0.1f, 0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);
                if (stability < 30 && tick % 20 == 0) playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.1f, 0.5f);
            }
        }
    }

    public void stopAmbientSoundscape(Player player) {
        Map<String, BukkitTask> sounds = activeSoundscapes.get(player.getUniqueId());
        if (sounds != null) {
            BukkitTask task = sounds.remove("ambient");
            if (task != null) task.cancel();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ABILITY SOUNDS - Layered, directional, reactive
    // ═════════════════════════════════════════════════════════════════════

    public void playCastSound(Player player, Theme theme, int tier, Location target) {
        Location loc = player.getLocation();
        
        // Layer 1: Base cast sound
        float pitch = 0.8f + tier * 0.15f + ThreadLocalRandom.current().nextFloat() * 0.2f;
        playSound(player, theme.getCastSound(), 0.8f, pitch);
        
        // Layer 2: Elemental layer
        playElementalCastLayer(player, theme, tier);
        
        // Layer 3: Power buildup (rising pitch)
        for (int i = 0; i < tier; i++) {
            final int layer = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    float p = 0.5f + layer * 0.3f + ThreadLocalRandom.current().nextFloat() * 0.2f;
                    playSound(player, theme.getCastSound(), 0.3f, p);
                }
            }.runTaskLater(plugin, layer * 3L);
        }
        
        // Layer 4: Target anticipation (if target location provided)
        if (target != null && target.distance(loc) < 30) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    playSound(target, theme.getImpactSound(), 0.3f, 0.5f);
                }
            }.runTaskLater(plugin, tier * 5L);
        }
    }

    private void playElementalCastLayer(Player player, Theme theme, int tier) {
        AspectType aspect = theme.getAspect();
        Location loc = player.getLocation();

        switch (aspect) {
            case INFERNO -> playSound(player, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f + tier * 0.1f);
            case TIDE -> playSound(player, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.5f, 1.0f + tier * 0.1f);
            case TEMPEST -> playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f + tier * 0.1f);
            case RIFT -> playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f + tier * 0.1f);
            case VITALITY -> playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f + tier * 0.1f);
            case WAR -> playSound(player, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 0.8f + tier * 0.1f);
            case COSMOS -> playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.0f + tier * 0.1f);
            case FORTUNE -> playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f + tier * 0.1f);
            case CLOUD -> playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 1.0f + tier * 0.1f);
            case WINTER -> playSound(player, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f + tier * 0.1f);
            case SHADOW -> playSound(player, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.5f + tier * 0.1f);
            case STORM -> playSound(player, Sound.ENTITY_BREEZE_INHALE, 0.5f, 1.0f + tier * 0.1f);
        }
    }

    public void playImpactSound(Theme theme, Location impactLoc, int tier, boolean isCritical) {
        // Layer 1: Base impact
        float volume = 0.8f + tier * 0.15f;
        float pitch = 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f;
        if (isCritical) { volume *= 1.5f; pitch += 0.3f; }
        playSound(impactLoc, theme.getImpactSound(), volume, pitch);

        // Layer 2: Elemental impact
        playElementalImpactLayer(impactLoc, theme, tier, isCritical);

        // Layer 3: Critical hit sound
        if (isCritical) {
            playSound(impactLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }

        // Layer 4: Directional echo for nearby players
        for (Player nearby : impactLoc.getWorld().getPlayers()) {
            double dist = nearby.getLocation().distance(impactLoc);
            if (dist < 20) {
                float vol = (float) (0.3 * (1 - dist / 20));
                playSound(nearby, impactLoc, theme.getImpactSound(), vol, pitch * 0.8f);
            }
        }
    }

    private void playElementalImpactLayer(Location loc, Theme theme, int tier, boolean critical) {
        AspectType aspect = theme.getAspect();
        float vol = 0.6f + tier * 0.1f;
        if (critical) vol *= 1.5f;

        switch (aspect) {
            case INFERNO -> playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, vol, 0.8f);
            case TIDE -> playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, vol, 1.2f);
            case TEMPEST -> playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, vol, 1.5f);
            case RIFT -> playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, vol, 0.6f);
            case VITALITY -> playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, vol, 1.5f);
            case WAR -> playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, vol, 0.6f);
            case COSMOS -> playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, vol, 1.0f);
            case FORTUNE -> playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, vol, 1.5f);
            case CLOUD -> playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, vol, 1.2f);
            case WINTER -> playSound(loc, Sound.BLOCK_GLASS_BREAK, vol, 1.5f);
            case SHADOW -> playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, vol, 0.5f);
            case STORM -> playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, vol, 1.3f);
        }
    }

    public void playImpactSound(Player player, Theme theme, Location impactLoc, int tier, boolean isCritical) {
        playImpactSound(theme, impactLoc, tier, isCritical);
        // Also play for the player
        if (player != null && player.isOnline()) {
            playSound(player, theme.getImpactSound(), 0.5f, 1.0f + ThreadLocalRandom.current().nextFloat() * 0.5f);
        }
    }

    public void playUltimateSound(Player player, Theme theme) {
        Location loc = player.getLocation();
        
        // Layer 1: Ultimate sound
        playSound(player, theme.getUltimateSound(), 2.0f, 0.8f);
        
        // Layer 2: Global resonance
        for (Player p : loc.getWorld().getPlayers()) {
            double dist = p.getLocation().distance(loc);
            if (dist < 100) {
                float vol = (float) (1.0 * (1 - dist / 100));
                playSound(p, loc, theme.getUltimateSound(), vol, 0.6f);
            }
        }
        
        // Layer 3: Elemental ultimate
        playElementalUltimate(loc, theme);
        
        // Layer 4: Reality tear sound
        new BukkitRunnable() {
            @Override
            public void run() {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.5f);
            }
        }.runTaskLater(plugin, 10L);
        
        // Layer 5: Aftershock
        new BukkitRunnable() {
            @Override
            public void run() {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
            }
        }.runTaskLater(plugin, 30L);
    }

    private void playElementalUltimate(Location loc, Theme theme) {
        AspectType aspect = theme.getAspect();
        
        switch (aspect) {
            case INFERNO -> {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5||!loc.getWorld().getPlayers().isEmpty()) {cancel();return;} playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f+c*0.1f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case TIDE -> {
                playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.6f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f+c*0.1f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case TEMPEST -> {
                playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>8) {cancel();return;} playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f); } }.runTaskTimer(plugin, 0L, 3L);
            }
            case RIFT -> {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.4f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f+c*0.1f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case VITALITY -> {
                playSound(loc, Sound.ITEM_TOTEM_USE, 2.0f, 1.0f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f+c*0.1f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case WAR -> {
                playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case COSMOS -> {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.5f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f+c*0.1f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case FORTUNE -> {
                playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 1.2f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>10) {cancel();return;} playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f); } }.runTaskTimer(plugin, 0L, 3L);
            }
            case CLOUD -> {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>8) {cancel();return;} playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case WINTER -> {
                playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.5f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case SHADOW -> {
                playSound(loc, Sound.ENTITY_WARDEN_DEATH, 2.0f, 0.4f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>5) {cancel();return;} playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f); } }.runTaskTimer(plugin, 0L, 5L);
            }
            case STORM -> {
                playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.6f);
                new BukkitRunnable() { int c=0; @Override public void run() { if(c++>10) {cancel();return;} playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.3f); } }.runTaskTimer(plugin, 0L, 3L);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HEARTBEAT & STABILITY SOUNDS
    // ════════════════════════════════════════════════════════════════════

    public void playHeartbeat(Player player, Theme theme, int stability) {
        if (!player.isOnline()) return;
        
        int bpm = Math.max(40, 120 - stability * 2);
        int interval = 1200 / bpm;
        
        String key = "heartbeat_" + System.currentTimeMillis();
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || tick > 200) { cancel(); return; }
                
                if (tick % interval == 0) {
                    float volume = 0.3f + (100 - stability) / 200.0f;
                    float pitch = 0.8f + (100 - stability) / 150.0f;
                    playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeSoundscapes.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(key, task);
    }

    public void playStabilityWarning(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        for (int i = 0; i < 5; i++) {
            final int pulse = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 0.5f + pulse * 0.1f);
                    playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.3f + pulse * 0.1f);
                }
            }.runTaskLater(plugin, pulse * 40L);
        }
    }

    public void playCriticalWarningSound(Player player, Theme theme) {
        playStabilityWarning(player, theme);
    }

    public void playEssenceGainSound(Player player, Theme theme, long amount) {
        if (!player.isOnline()) return;
        int count = (int)(amount / 10);
        for (int i = 0; i < Math.min(count, 5); i++) {
            final int c = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f + c * 0.1f);
                }
            }.runTaskLater(plugin, c * 2L);
        }
    }

    public void playAwakenedSound(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                playSound(player, theme.getUltimateSound(), 1.5f, 1.0f);
            }
        }.runTaskLater(plugin, 20L);
    }

    public void playHeartDeathSound(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        playSound(player, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 0.3f);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ════════════════════════════════════════════════════════════════════
    // TIER PROGRESSION
    // ════════════════════════════════════════════════════════════════════

    public void playTierUpSound(Player player, Theme theme, int newTier) {
        if (!player.isOnline()) return;
        
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f + newTier * 0.2f);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f + newTier * 0.2f);
        
        for (int i = 0; i < newTier; i++) {
            final int t = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    playSound(player, theme.getCastSound(), 0.5f, 1.0f + t * 0.2f);
                }
            }.runTaskLater(plugin, t * 15L);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ASPECT TRANSITION
    // ════════════════════════════════════════════════════════════════════

    public void playAspectTransitionSound(Player player, Theme oldTheme, Theme newTheme) {
        if (!player.isOnline()) return;
        
        // Old aspect fade
        playSound(player, oldTheme.getAmbientSound(), 0.5f, 0.3f);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                // Void moment
                playSound(player, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.2f);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        
                        // New aspect emerge
                        for (int i = 0; i < 3; i++) {
                            final int p = i;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!player.isOnline()) return;
                                    playSound(player, newTheme.getCastSound(), 1.0f, 1.0f + p * 0.2f);
                                }
                            }.runTaskLater(plugin, p * 10L);
                        }
                        
                        playSound(player, newTheme.getUltimateSound(), 1.5f, 0.8f);
                    }
                }.runTaskLater(plugin, 10L);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ═════════════════════════════════════════════════════════════════════
    // EVENT SOUNDS
    // ════════════════════════════════════════════════════════════════════

    public void playEventPhaseSound(Player player, Theme theme, EventPhase phase) {
        if (!player.isOnline()) return;
        
        playSound(player, theme.getUltimateSound(), 1.5f, 0.8f);
        playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                playSound(player, theme.getAmbientSound(), 1.0f, 1.0f);
            }
        }.runTaskLater(plugin, 20L);
    }

    public void playGlobalAnnouncementSound(Player player, Theme theme) {
        if (!player.isOnline()) return;
        playSound(player, theme.getUltimateSound(), 1.0f, 0.8f);
    }

    // ═════════════════════════════════════════════════════════════════════
    // COMBAT SOUNDS
    // ════════════════════════════════════════════════════════════════════

    public void playHitSound(Player player, Theme theme, boolean isCritical) {
        if (!player.isOnline()) return;
        
        if (isCritical) {
            playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        }
        playSound(player, theme.getImpactSound(), 0.5f, 1.0f + ThreadLocalRandom.current().nextFloat() * 0.5f);
    }

    public void playKillSound(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        playSound(player, theme.getAccentColor().equals(Color.fromRGB(0xFFD700)) 
            ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : theme.getImpactSound(), 0.8f, 1.5f);
    }

    public void playDeathSound(Player player, Theme theme) {
        if (!player.isOnline()) return;
        
        playSound(player, Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
        playSound(player, theme.getUltimateSound(), 0.5f, 0.5f);
    }

    // ════════════════════════════════════════════════════════════════════
    // CLEAR ALL
    // ═══════════════════════════════════════════════════════════════════

    public void clearPlayer(Player player) {
        Map<String, BukkitTask> sounds = activeSoundscapes.remove(player.getUniqueId());
        if (sounds != null) {
            sounds.values().forEach(BukkitTask::cancel);
        }
    }

    public void clearAll() {
        activeSoundscapes.values().forEach(map -> map.values().forEach(BukkitTask::cancel));
        activeSoundscapes.clear();
    }

    public void shutdown() {
        clearAll();
    }
}
