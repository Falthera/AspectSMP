package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.CloudWraith;
import com.aspectsmp.mobs.StormSentinel;
import com.aspectsmp.mobs.TempestPhantom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TempestsFuryEvent extends BaseEvent {

    private EventPhase phase = EventPhase.STORM_AWAKENS;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int fragmentsCollected = 0;
    private static final int REQUIRED_FRAGMENTS = 45;
    private static final int MAX_MOB_SPAWNS = 450;
    private int mobsSpawned = 0;
    private LivingEntity stormTitanEntity;
    private BukkitTask stormEffectsTask;
    private BukkitTask mobSpamTask;
    private BukkitTask convergenceTask;
    private final List<BukkitTask> scheduledTransitions = new ArrayList<>();
    private final List<Location> riftLocations = new ArrayList<>();
    private final List<Location> cycloneLocations = new ArrayList<>();
    private int cycloneIntensity = 0;
    private long eventStartTime = 0;
    private static final long EVENT_DURATION_MS = 7L * 24 * 60 * 60 * 1000;
    private final Map<UUID, Integer> playerWindStacks = new ConcurrentHashMap<>();

    public TempestsFuryEvent(AspectSMP plugin) {
        super(plugin);
    }

    public boolean isActive() {
        return active;
    }

    public EventPhase getPhase() {
        return phase;
    }

    public LivingEntity getStormTitanEntity() {
        return stormTitanEntity;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public void startEvent() {
        if (active) return;
        
        // Check if Storm Aspect is already claimed
        if (plugin.isAspectClaimed(AspectType.STORM)) {
            UUID holder = plugin.getAspectHolder(AspectType.STORM);
            Player holderPlayer = Bukkit.getPlayer(holder);
            String holderName = holderPlayer != null ? holderPlayer.getName() : "Unknown";
            Bukkit.broadcastMessage("\u00a7c\u00a7lThe Storm Aspect has already been claimed by \u00a7f" + holderName + "\u00a7c!");
            Bukkit.broadcastMessage("\u00a7cThe Tempest's Fury cannot begin.");
            return;
        }
        
        active = true;
        phase = EventPhase.STORM_AWAKENS;
        fragmentsCollected = 0;
        mobsSpawned = 0;
        participants.clear();
        stormTitanEntity = null;
        riftLocations.clear();
        cycloneLocations.clear();
        cycloneIntensity = 0;
        playerWindStacks.clear();
        eventStartTime = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                participants.add(player.getUniqueId());
            }
        }

        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a7b\u00a7l      THE TEMPEST'S FURY HAS BEGUN");
        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a77The sky darkens... winds howl across the world.");
        Bukkit.broadcastMessage("\u00a77An ancient storm awakens, tearing the heavens asunder.");
        Bukkit.broadcastMessage("\u00a7e\u00a7lCollect Storm Fragments \u00a77scattered across the overworld.");
        Bukkit.broadcastMessage("\u00a77Defeat \u00a7bWind Walkers\u00a77, survive the cyclones, and claim the \u00a7bStorm Aspect\u00a77!");
        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
                plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
                    "THE TEMPEST'S FURY",
                    "The sky darkens... winds howl across the world.",
                    AspectType.STORM
                );
            }
        }

        schedulePhaseTransition(200L, EventPhase.STORM_FRAGMENTS);
        startGlobalEffects();
    }

    public void stopEvent() {
        active = false;
        cancelAllTasks();
        participants.clear();
        fragmentsCollected = 0;
        mobsSpawned = 0;
        stormTitanEntity = null;
        riftLocations.clear();
        cycloneLocations.clear();
        playerWindStacks.clear();
        Bukkit.broadcastMessage("\u00a7cThe Tempest's Fury has been stopped by an admin.");
    }

    private void cancelAllTasks() {
        if (stormEffectsTask != null) stormEffectsTask.cancel();
        if (mobSpamTask != null) mobSpamTask.cancel();
        if (convergenceTask != null) convergenceTask.cancel();
        for (BukkitTask task : scheduledTransitions) {
            task.cancel();
        }
        scheduledTransitions.clear();
    }

    private void schedulePhaseTransition(long delay, EventPhase nextPhase) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                scheduledTransitions.remove(this);
                if (!active) { cancel(); return; }
                advancePhase(nextPhase);
            }
        }.runTaskLater(plugin, delay);
        scheduledTransitions.add(task);
    }

    private void startGlobalEffects() {
        stormEffectsTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                tick++;

                if (System.currentTimeMillis() - eventStartTime > EVENT_DURATION_MS) {
                    if (phase != EventPhase.STORM_COMPLETED) {
                        advancePhase(EventPhase.STORM_COMPLETED);
                    }
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

                    if (phase == EventPhase.STORM_FRAGMENTS || phase == EventPhase.STORM_GUSTS) {
                        if (tick % 40 == 0) {
                            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 2, 0), 2, 1.5, 1, 1.5, 0.01);
                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 1, 1, 0.5, 1, 0.005);
                        }
                    }

                    if (phase == EventPhase.STORM_CYCLONES || phase == EventPhase.STORM_TITAN || phase == EventPhase.STORM_CONVERGENCE) {
                        if (tick % 20 == 0) {
                            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 2, 0), 4, 3, 2, 3, 0.03);
                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 2, 2, 1, 2, 0.01);
                        }
                        if (tick % 60 == 0) {
                            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.LEVITATION, 80, 0, true, false), true);
                            int stacks = playerWindStacks.getOrDefault(player.getUniqueId(), 0);
                            if (stacks < 3) {
                                playerWindStacks.put(player.getUniqueId(), stacks + 1);
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.SPEED, 100, stacks, true, false), true);
                            }
                        }
                    }

                    if (phase == EventPhase.STORM_CONVERGENCE) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.3f, 0.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void advancePhase(EventPhase next) {
        for (BukkitTask task : scheduledTransitions) { task.cancel(); }
        scheduledTransitions.clear();

        this.phase = next;
        
        // Play visual effects for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getVisualEffectsManager().playEventPhaseTransition(player, next, AspectType.STORM);
        }
        
        switch (next) {
            case STORM_FRAGMENTS -> {
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7b\u00a7l   STORM FRAGMENTS HAVE SCATTERED ACROSS THE WORLD");
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77\u00a7l" + REQUIRED_FRAGMENTS + " Storm Fragments \u00a77are scattered across the overworld.");
                Bukkit.broadcastMessage("\u00a77Defeat \u00a7bWind Walkers \u00a77guarding them to collect fragments.");
                Bukkit.broadcastMessage("\u00a77Gust rifts will open periodically, spawning more Walkers.");
                startFragmentPhase();
                schedulePhaseTransition(144000L, EventPhase.STORM_GUSTS); // 2 hours
            }
            case STORM_GUSTS -> {
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7b\u00a7l   GUST RIFTS TEAR OPEN - WINDS HOWL");
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Rifts open across the world! \u00a7bWind Walkers \u00a77pour through.");
                Bukkit.broadcastMessage("\u00a77Collect remaining fragments before the cyclones form!");
                openGustRifts();
                schedulePhaseTransition(144000L, EventPhase.STORM_CYCLONES); // 2 hours
            }
            case STORM_CYCLONES -> {
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7b\u00a7l   MASSIVE CYCLONES FORM ACROSS THE LAND");
                Bukkit.broadcastMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Colossal cyclones ravage the overworld! \u00a7bLevitation\u00a77 affects all!");
                Bukkit.broadcastMessage("\u00a77Speed stacks accumulate. Survive the winds and prepare for the Titan!");
                startCyclones();
                schedulePhaseTransition(144000L, EventPhase.STORM_TITAN); // 2 hours
            }
            case STORM_TITAN -> {
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7c\u00a7l   THE STORM TITAN DESCENDS FROM THE HEAVENS");
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77A colossal entity of pure wind and lightning manifests!");
                Bukkit.broadcastMessage("\u00a77Rally together and defeat it to proceed!");
                spawnStormTitan();
            }
            case STORM_CONVERGENCE -> {
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7a\u00a7l   THE TEMPEST CONVERGES - FINAL TRIAL");
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77The Storm Titan falls... but the true trial begins.");
                Bukkit.broadcastMessage("\u00a77Stand at the convergence points and endure the final gale!");
                startConvergence();
            }
            case STORM_COMPLETED -> {
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7a\u00a7l   THE TEMPEST'S FURY HAS BEEN CONQUERED!");
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                active = false;
            }
            default -> {}
        }
    }

    private void startFragmentPhase() {
        mobSpamTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_FRAGMENTS && phase != EventPhase.STORM_GUSTS) {
                    cancel();
                    mobSpamTask = null;
                    return;
                }
                if (mobsSpawned >= MAX_MOB_SPAWNS) return;
                spawnFragmentGuards();
            }
        }.runTaskTimer(plugin, 0L, 200L);
        schedulePhaseTransition(144000L, EventPhase.STORM_GUSTS);
    }

    private void spawnFragmentGuards() {
        @SuppressWarnings("unchecked")
        List<Player> overworldPlayers = (List<Player>) Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().getEnvironment() == World.Environment.NORMAL)
            .filter(p -> participants.contains(p.getUniqueId()))
            .toList();
        if (overworldPlayers.isEmpty()) return;

        int spawnsThisTick = Math.min(2, MAX_MOB_SPAWNS - mobsSpawned);
        for (int i = 0; i < spawnsThisTick; i++) {
            Player randomPlayer = overworldPlayers.get(ThreadLocalRandom.current().nextInt(overworldPlayers.size()));
            Location base = randomPlayer.getLocation();
            Location spawn = new Location(
                base.getWorld(),
                base.getX() + 15 + ThreadLocalRandom.current().nextInt(30) - 15,
                0,
                base.getZ() + 15 + ThreadLocalRandom.current().nextInt(30) - 15
            );
            int safeY = spawn.getWorld().getHighestBlockYAt(spawn) + 1;
            spawn.setY(safeY);
            if (spawn.getBlock().getType().isSolid()) spawn.setY(safeY + 1);
            if (mobsSpawned >= MAX_MOB_SPAWNS) break;
            mobsSpawned++;
            switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0 -> new CloudWraith(plugin, spawn);
                case 1 -> new StormSentinel(plugin, spawn);
                case 2 -> new TempestPhantom(plugin, spawn);
            }
        }
    }

    private void openGustRifts() {
        World overworld = Bukkit.getWorld("world");
        riftLocations.clear();
        for (int i = 0; i < 12; i++) {
            Location rift = new Location(overworld,
                ThreadLocalRandom.current().nextInt(2000) - 1000,
                0,
                ThreadLocalRandom.current().nextInt(2000) - 1000
            );
            rift.setY(overworld.getHighestBlockYAt(rift) + 2);
            riftLocations.add(rift);
            overworld.spawnParticle(Particle.CLOUD, rift, 100, 5, 10, 5, 0.15);
            overworld.spawnParticle(Particle.SWEEP_ATTACK, rift, 50, 4, 6, 4, 0.05);
            overworld.playSound(rift, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.8f);
        }
        Bukkit.broadcastMessage("\u00a7b\u00a7l" + riftLocations.size() + " GUST RIFTS HAVE OPENED! \u00a77Find them and defeat the Walkers within!");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_GUSTS) { cancel(); return; }
                ticks++;
                for (Location rift : riftLocations) {
                    if (Math.random() < 0.3 && mobsSpawned < MAX_MOB_SPAWNS) {
                        mobsSpawned++;
                        new StormSentinel(plugin, rift.clone().add(ThreadLocalRandom.current().nextInt(10) - 5, 0, ThreadLocalRandom.current().nextInt(10) - 5));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void startCyclones() {
        World overworld = Bukkit.getWorld("world");
        cycloneLocations.clear();
        for (int i = 0; i < 8; i++) {
            Location cyclone = new Location(overworld,
                ThreadLocalRandom.current().nextInt(2000) - 1000,
                255,
                ThreadLocalRandom.current().nextInt(2000) - 1000
            );
            cycloneLocations.add(cyclone);
        }

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_CYCLONES && phase != EventPhase.STORM_TITAN) { cancel(); return; }
                tick++;
                cycloneIntensity = Math.min(3, tick / 2000);

                for (Location cyclone : cycloneLocations) {
                    cyclone.getWorld().spawnParticle(Particle.CLOUD, cyclone, 60 + cycloneIntensity * 25, 20, 10, 20, 0.06);
                    cyclone.getWorld().spawnParticle(Particle.SWEEP_ATTACK, cyclone, 40 + cycloneIntensity * 15, 15, 5, 15, 0.04);
                    cyclone.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, cyclone, 15 + cycloneIntensity * 8, 10, 5, 10, 0.03);

                    if (tick % 40 == 0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld() == cyclone.getWorld() && player.getLocation().distanceSquared(cyclone) < 2500) {
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.LEVITATION, 100, cycloneIntensity, true, false), true);
                                if (cycloneIntensity >= 2 && tick % 80 == 0) {
                                    player.damage(1.0);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_CYCLONES) { cancel(); return; }
                ticks++;
                if (ticks % 300 == 0 && mobsSpawned < MAX_MOB_SPAWNS) {
                    Location cyclone = cycloneLocations.get(ThreadLocalRandom.current().nextInt(cycloneLocations.size()));
                    mobsSpawned += 2;
                    new StormSentinel(plugin, cyclone.clone().add(ThreadLocalRandom.current().nextInt(20) - 10, 0, ThreadLocalRandom.current().nextInt(20) - 10));
                    new TempestPhantom(plugin, cyclone.clone().add(ThreadLocalRandom.current().nextInt(20) - 10, 0, ThreadLocalRandom.current().nextInt(20) - 10));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnStormTitan() {
        World overworld = Bukkit.getWorld("world");
        Location spawn = new Location(overworld,
            ThreadLocalRandom.current().nextInt(1000) - 500,
            255,
            ThreadLocalRandom.current().nextInt(1000) - 500
        );
        spawn.setY(overworld.getHighestBlockYAt(spawn) + 50);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.sendTitle("\u00a7b\u00a7lSTORM TITAN", "\u00a77Descends from the tempest...", 20, 100, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            }
        }

        com.aspectsmp.mobs.SkyGuardian guardian = new com.aspectsmp.mobs.SkyGuardian(plugin, spawn);
        stormTitanEntity = guardian.getEntity();
        
        stormTitanEntity.setCustomName("\u00a7b\u00a7lThe Storm Titan");
        stormTitanEntity.setCustomNameVisible(true);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!active || stormTitanEntity == null || stormTitanEntity.isDead()) { cancel(); return; }
                ticks++;
                if (ticks % 2400 == 0) {
                    double hpPercent = stormTitanEntity.getHealth() / stormTitanEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                    if (hpPercent < 0.5) {
                        stormTitanEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.STRENGTH, 2400, 1, true, false), true);
                        stormTitanEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SPEED, 2400, 1, true, false), true);
                        stormTitanEntity.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, stormTitanEntity.getLocation(), 10, 2, 2, 2, 0.2);
                        stormTitanEntity.getWorld().strikeLightningEffect(stormTitanEntity.getLocation());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startConvergence() {
        World overworld = Bukkit.getWorld("world");
        convergenceTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_CONVERGENCE) { cancel(); return; }
                tick++;

                if (tick % 600 == 0) {
                    int livingParticipants = (int) participants.stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .filter(p -> p.isOnline() && p.getWorld().getEnvironment() == World.Environment.NORMAL)
                        .count();

                    if (livingParticipants == 0) {
                        Bukkit.broadcastMessage("\u00a7cAll participants have fallen... The Tempest retreats.");
                        stopEvent();
                        cancel();
                        return;
                    }

                    Bukkit.broadcastMessage("\u00a7b\u00a7lConvergence active: \u00a7a" + livingParticipants + " \u00a7bsurvivors weather the storm.");
                }

                for (Location rift : riftLocations) {
                    rift.getWorld().spawnParticle(Particle.CLOUD, rift, 100, 10, 20, 10, 0.2);
                    rift.getWorld().spawnParticle(Particle.SWEEP_ATTACK, rift, 50, 8, 15, 8, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.STORM_CONVERGENCE) { cancel(); return; }
                int atConvergence = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!participants.contains(player.getUniqueId())) continue;
                    for (Location rift : riftLocations) {
                        if (player.getLocation().distanceSquared(rift) <= 100) {
                            atConvergence++;
                            break;
                        }
                    }
                }
                if (atConvergence >= Math.max(1, participants.size() / 2)) {
                    Bukkit.broadcastMessage("\u00a7a\u00a7lTHE TEMPEST CONVERGENCE IS COMPLETE!");
                    Bukkit.broadcastMessage("\u00a77The storm calms... The worthy shall claim their reward.");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        schedulePhaseTransition(36000L, EventPhase.STORM_COMPLETED); // 30 minutes
    }

    public void collectFragment(Player player) {
        if (!active || (phase != EventPhase.STORM_FRAGMENTS && phase != EventPhase.STORM_GUSTS)) return;
        if (!participants.contains(player.getUniqueId())) return;
        fragmentsCollected++;
        player.sendMessage("\u00a7aStorm Fragment collected! \u00a77(" + fragmentsCollected + "/" + REQUIRED_FRAGMENTS + ")");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        if (fragmentsCollected >= REQUIRED_FRAGMENTS) {
            Bukkit.broadcastMessage("\u00a7e\u00a7lALL STORM FRAGMENTS RECOVERED!");
            Bukkit.broadcastMessage("\u00a77The Gust Rifts begin to tear open...");
            if (mobSpamTask != null) { mobSpamTask.cancel(); mobSpamTask = null; }
            if (phase == EventPhase.STORM_FRAGMENTS) advancePhase(EventPhase.STORM_GUSTS);
        }
    }

    public void addParticipant(Player player) {
        if (active && (phase == EventPhase.STORM_AWAKENS || phase == EventPhase.STORM_FRAGMENTS || phase == EventPhase.STORM_GUSTS)) {
            participants.add(player.getUniqueId());
            player.sendMessage("\u00a7eYou have joined the Tempest's Fury!");
        }
    }

    public void awardWinner(Player winner) {
        if (winner == null || !winner.isOnline()) return;
        Heart heart = plugin.getHeartManager().getHeart(winner.getUniqueId()).orElse(null);
        if (heart == null) return;

        int previousTier = heart.getTier();
        heart.setAspect(AspectType.STORM);
        heart.setTier(previousTier >= 2 ? previousTier : 1);
        heart.setWinterUnlocked(true); // Reuse field for Storm
        heart.setStability(100);
        heart.setDormant(false);
        heart.getCooldowns().clear();
        plugin.getHeartManager().saveHeart(heart);

        // Claim the aspect for this player
        plugin.claimAspect(AspectType.STORM, winner.getUniqueId());

        ItemStack stormheart = createStormheart();
        Map<Integer, ItemStack> leftover = winner.getInventory().addItem(stormheart);
        if (!leftover.isEmpty()) {
            winner.getWorld().dropItemNaturally(winner.getLocation(), leftover.values().iterator().next());
        }

        winner.sendMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("\u00a7b\u00a7l   YOU HAVE WEATHERED THE TEMPEST'S FURY!");
        winner.sendMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("\u00a7a\u00a7lYou are the Master of the Storm!");
        winner.sendMessage("\u00a77You have unlocked the \u00a7bStorm Aspect \u00a77and received a \u00a7bStormheart\u00a77!");

        Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("\u00a7a\u00a7l" + winner.getName() + " \u00a7ahas conquered the Tempest's Fury!");
        Bukkit.broadcastMessage("\u00a77They are now the \u00a7bSOLE HOLDER \u00a77of the Storm Aspect and carry the \u00a7bStormheart\u00a77.");
        Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        winner.getWorld().strikeLightningEffect(winner.getLocation());
        for (int i = 0; i < 5; i++) {
            winner.getWorld().spawnParticle(Particle.CLOUD, winner.getLocation().add(0, 2, 0), 50, 2, 2, 2, 0.1);
            winner.getWorld().spawnParticle(Particle.SWEEP_ATTACK, winner.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);
            winner.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, winner.getLocation().add(0, 1, 0), 20, 1, 1, 1, 0.03);
        }
        
        // Play visual effects for winner
        int tier = heart != null ? heart.getTier() : 2;
        plugin.getVisualEffectsManager().playHeartUpgradeVisuals(winner, tier);
        plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
            "MASTER OF THE STORM",
            winner.getName() + " has conquered the Tempest's Fury!",
            AspectType.STORM
        );
    }

    private ItemStack createStormheart() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lStormheart");
        meta.setLore(Arrays.asList(
            "\u00a77A crystallized fragment of pure tempest.",
            "\u00a77Grants the \u00a7bStorm Aspect \u00a77permanently.",
            "\u00a7b\u00a7lRight-click to activate Tempest Avatar."
        ));
        setCustomModelDataString(item, "stormheart");
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCompleted() {
        return !active && phase == EventPhase.STORM_COMPLETED;
    }
    private void setCustomModelDataString(ItemStack item, String modelDataString) {
        CustomModelData cmd = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        CustomModelData.Builder builder = CustomModelData.customModelData();
        if (cmd != null) {
            for (String s : cmd.strings()) builder.addString(s);
            for (Float f : cmd.floats()) builder.addFloat(f);
            for (Boolean b : cmd.flags()) builder.addFlag(b);
            for (org.bukkit.Color c : cmd.colors()) builder.addColor(c);
        }
        builder.addString(modelDataString);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
    }

    @Override
    public void saveState() {
        // Save event state to storage
    }

    @Override
    public void loadState() {
        // Load event state from storage
    }
}


