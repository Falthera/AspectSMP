package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FrozenEclipseEvent {

    private final AspectSMP plugin;
    private EventPhase phase = EventPhase.FIRST_FROST;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int guardiansDefeated = 0;
    private static final int REQUIRED_GUARDIANS = 5;

    private final Map<UUID, Long> lastFrostDamage = new ConcurrentHashMap<>();
    private static final long FROST_DAMAGE_INTERVAL_MS = 1000;
    private UUID winner;
    private org.bukkit.entity.LivingEntity frostMonarchEntity;

    public FrozenEclipseEvent(AspectSMP plugin) {
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

    public UUID getWinner() {
        return winner;
    }

    public org.bukkit.entity.LivingEntity getFrostMonarchEntity() {
        return frostMonarchEntity;
    }

    public void startEvent() {
        if (active) return;
        active = true;
        phase = EventPhase.FIRST_FROST;
        guardiansDefeated = 0;
        participants.clear();

        Bukkit.broadcastMessage("§b§lThe Frozen Eclipse has begun...");
        Bukkit.broadcastMessage("§7An unnatural winter descends upon the world.");
        Bukkit.broadcastMessage("§7Find the path to the Frozen Kingdom.");

        startFrostEffects();
        schedulePhaseTransition(200L, EventPhase.FROZEN_KINGDOM);
    }

    private void startFrostEffects() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, false));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 0.8f);
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
            case FROZEN_KINGDOM -> {
                Bukkit.broadcastMessage("§e§lThe Frozen Kingdom has appeared!");
                Bukkit.broadcastMessage("§7Enter the ancient frozen civilization.");
                spawnFrozenGuardians();
            }
            case TRIALS -> {
                Bukkit.broadcastMessage("§d§lThe Trials of Winter begin!");
                Bukkit.broadcastMessage("§7Survive the absolute cold.");
                startTrials();
            }
            case FROST_MONARCH -> {
                Bukkit.broadcastMessage("§c§lThe Frost Monarch awakens!");
                Bukkit.broadcastMessage("§7Defeat it to claim the Aspect of Winter.");
                spawnFrostMonarch();
            }
            case COMPLETED -> {
                Bukkit.broadcastMessage("§a§lThe Frozen Eclipse has been conquered!");
                Bukkit.broadcastMessage("§7Aspect of Winter has been unlocked for all participants.");
                grantWinterAspect();
            }
            default -> {}
        }
    }

    private void spawnFrozenGuardians() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL && Math.random() < 0.12) {
                Location spawn = player.getLocation().add(5 + new Random().nextInt(10), 2, 5 + new Random().nextInt(10));
                spawn.getChunk().load();
                switch (new Random().nextInt(3)) {
                    case 0 -> new Frostborn(plugin, spawn);
                    case 1 -> new IceWraith(plugin, spawn);
                    case 2 -> new GlacierTitan(plugin, spawn);
                }
            }
        }
    }

    private void startTrials() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (participants.contains(player.getUniqueId())) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (participants.contains(player.getUniqueId())) {
                        player.damage(1.0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);

        schedulePhaseTransition(400L, EventPhase.FROST_MONARCH);
    }

    private void spawnFrostMonarch() {
        World winterWorld = Bukkit.getWorld("winter_kingdom");
        if (winterWorld == null) {
            plugin.getLogger().warning("Winter Kingdom world not found! Create a world named 'winter_kingdom' first.");
            return;
        }
        Location spawn = winterWorld.getSpawnLocation().add(0, 15, 0);
        FrostMonarch frostMonarch = new FrostMonarch(plugin, spawn);
        frostMonarchEntity = frostMonarch.getEntity();
        Bukkit.broadcastMessage("§c§lFROST MONARCH SPAWNED!");
    }

    private void grantWinterAspect() {
        if (winner == null) return;
        Player player = Bukkit.getPlayer(winner);
        if (player != null && player.isOnline()) {
            Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
            if (heart != null) {
                heart.setAspect(AspectType.WINTER);
                heart.setWinterUnlocked(true);
                heart.setTier(1);
                heart.getCooldowns().clear();
                player.sendMessage("§b§lYou have unlocked the Aspect of Winter!");
            }
            Bukkit.broadcastMessage("§e§l" + player.getName() + " has conquered the Frozen Eclipse!");
        }
    }

    public void awardWinner(Player player) {
        this.winner = player.getUniqueId();
        this.active = false;
    }

    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    public void stopEvent() {
        active = false;
        Bukkit.broadcastMessage("§cThe Frozen Eclipse event has been stopped.");
    }

    public void onGuardianDefeated() {
        guardiansDefeated++;
        Bukkit.broadcastMessage("§aGuardian defeated! (" + guardiansDefeated + "/" + REQUIRED_GUARDIANS + ")");
        if (guardiansDefeated >= REQUIRED_GUARDIANS) {
            Bukkit.broadcastMessage("§eAll guardians have been defeated!");
            advancePhase(EventPhase.TRIALS);
            schedulePhaseTransition(300L, EventPhase.FROST_MONARCH);
        }
    }
}
