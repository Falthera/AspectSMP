package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.CloudWraith;
import com.aspectsmp.mobs.SkyGuardian;
import com.aspectsmp.mobs.StormSentinel;
import com.aspectsmp.mobs.TempestPhantom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NinthSkyEvent {

    private final AspectSMP plugin;
    private EventPhase phase = EventPhase.SKY_AWAKENS;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int fragmentsCollected = 0;
    private static final int REQUIRED_FRAGMENTS = 5;

    public NinthSkyEvent(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return active;
    }

    public EventPhase getPhase() {
        return phase;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public void startEvent() {
        if (active) return;
        active = true;
        phase = EventPhase.SKY_AWAKENS;
        fragmentsCollected = 0;
        participants.clear();

        Bukkit.broadcastMessage("§b§lThe Ninth Sky is awakening...");
        Bukkit.broadcastMessage("§7The clouds have begun to move. Something ancient stirs above.");

        startSkyEffects();
        schedulePhaseTransition(200L, EventPhase.SKY_FRAGMENTS);
    }

    private void startSkyEffects() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void schedulePhaseTransition(long delay, EventPhase nextPhase) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                advancePhase(nextPhase);
            }
        }.runTaskLater(plugin, delay);
    }

    public void advancePhase(EventPhase next) {
        this.phase = next;
        switch (next) {
            case SKY_FRAGMENTS -> {
                Bukkit.broadcastMessage("§e§lSky Fragments have fallen across the world!");
                Bukkit.broadcastMessage("§7Find and recover them before they are claimed.");
                spawnFragmentGuards();
            }
            case SKY_GATE -> {
                Bukkit.broadcastMessage("§d§lThe Sky Gate opens!");
                Bukkit.broadcastMessage("§7A portal to the Ninth Sky has formed.");
                openSkyGate();
            }
            case NINTH_SKY_REALM -> {
                Bukkit.broadcastMessage("§b§lEntering the Ninth Sky Realm...");
                Bukkit.broadcastMessage("§7Prove yourselves worthy.");
                teleportParticipantsToSkyRealm();
            }
            case TRIALS -> {
                Bukkit.broadcastMessage("§e§lThe Trials of the Ninth Sky begin!");
                Bukkit.broadcastMessage("§7Master movement, control, and ascension.");
                startTrials();
            }
            case SKY_GUARDIAN -> {
                Bukkit.broadcastMessage("§c§lThe Sky Guardian awakens!");
                Bukkit.broadcastMessage("§7Defeat it to claim the Aspect of the Cloud.");
                spawnSkyGuardian();
            }
            case COMPLETED -> {
                Bukkit.broadcastMessage("§a§lThe Ninth Sky has been conquered!");
                Bukkit.broadcastMessage("§7Aspect of the Cloud has been unlocked for all participants.");
                grantCloudAspect();
            }
            default -> {}
        }
    }

    private void spawnFragmentGuards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL && Math.random() < 0.15) {
                Location spawn = player.getLocation().add(5 + new Random().nextInt(10), 2, 5 + new Random().nextInt(10));
                spawn.getChunk().load();
                switch (new Random().nextInt(3)) {
                    case 0 -> new CloudWraith(plugin, spawn);
                    case 1 -> new StormSentinel(plugin, spawn);
                    case 2 -> new TempestPhantom(plugin, spawn);
                }
            }
        }
    }

    private void openSkyGate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                Location gateLoc = player.getWorld().getHighestBlockAt(player.getLocation()).getLocation().add(0, 30, 0);
                player.sendTitle("§b§lSky Gate Opened", "§7Look to the heavens", 20, 100, 20);
            }
        }
    }

    private void teleportParticipantsToSkyRealm() {
        World skyWorld = Bukkit.getWorld("ninth_sky");
        if (skyWorld == null) {
            plugin.getLogger().warning("Ninth Sky world not found! Create a world named 'ninth_sky' first.");
            return;
        }
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Location spawn = skyWorld.getSpawnLocation().add(0, 20, 0);
                player.teleport(spawn);
            }
        }
    }

    private void startTrials() {
        schedulePhaseTransition(400L, EventPhase.SKY_GUARDIAN);
    }

    private void spawnSkyGuardian() {
        World skyWorld = Bukkit.getWorld("ninth_sky");
        if (skyWorld == null) return;
        Location spawn = skyWorld.getSpawnLocation().add(0, 15, 0);
        new SkyGuardian(plugin, spawn);
        Bukkit.broadcastMessage("§c§lSKY GUARDIAN SPAWNED!");
    }

    private void grantCloudAspect() {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
                if (heart != null) {
                    heart.setAspect(AspectType.CLOUD);
                    heart.setTier(1);
                    heart.getCooldowns().clear();
                    player.sendMessage("§b§lYou have unlocked the Aspect of the Cloud!");
                }
            }
        }
    }

    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    public void collectFragment(Player player) {
        if (!active || phase != EventPhase.SKY_FRAGMENTS) return;
        if (!participants.contains(player.getUniqueId())) return;
        fragmentsCollected++;
        player.sendMessage("§aSky Fragment collected! (" + fragmentsCollected + "/" + REQUIRED_FRAGMENTS + ")");
        if (fragmentsCollected >= REQUIRED_FRAGMENTS) {
            Bukkit.broadcastMessage("§eAll Sky Fragments have been recovered!");
            advancePhase(EventPhase.SKY_GATE);
            schedulePhaseTransition(300L, EventPhase.NINTH_SKY_REALM);
            schedulePhaseTransition(600L, EventPhase.TRIALS);
            schedulePhaseTransition(1000L, EventPhase.SKY_GUARDIAN);
        }
    }
}
