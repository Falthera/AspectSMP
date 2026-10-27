package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.CloudWraith;
import com.aspectsmp.mobs.SkyGuardian;
import com.aspectsmp.mobs.StormSentinel;
import com.aspectsmp.mobs.TempestPhantom;
import com.aspectsmp.items.Cloudheart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class NinthSkyEvent extends BaseEvent {

    private final AtomicReference<EventPhase> phase = new AtomicReference<>(EventPhase.SKY_AWAKENS);
    private final AtomicInteger fragmentsCollected = new AtomicInteger(0);
    private static final int REQUIRED_FRAGMENTS = 50;
    private static final int MAX_MOB_SPAWNS = 500;
    private final AtomicInteger mobsSpawned = new AtomicInteger(0);
    private volatile LivingEntity skyGuardianEntity;
    private final AtomicLong eventStartTime = new AtomicLong(0);
    private static final long EVENT_DURATION_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long PHASE_DELAY_HOURS = 2 * 60 * 60 * 1000; // 2 hours in ms
    
    // Missing fields
    private final List<Location> riftLocations = new CopyOnWriteArrayList<>();
    private final List<Location> stormLocations = new CopyOnWriteArrayList<>();
    private final AtomicInteger stormIntensity = new AtomicInteger(0);

    public NinthSkyEvent(AspectSMP plugin) {
        super(plugin);
    }

    @Override
    public EventPhase getPhase() {
        return phase.get();
    }

    @Override
    public void setPhase(EventPhase phase) {
        this.phase.set(phase);
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void setActive(boolean active) {
        this.active.set(active);
    }

    public LivingEntity getSkyGuardianEntity() {
        return skyGuardianEntity;
    }

    @Override
    public Set<UUID> getParticipants() {
        return super.getParticipants();
    }

    @Override
    public void startEvent() {
        if (isActive()) return;

        if (plugin.isAspectClaimed(AspectType.CLOUD)) {
            UUID holder = plugin.getAspectHolder(AspectType.CLOUD);
            Player holderPlayer = Bukkit.getPlayer(holder);
            String holderName = holderPlayer != null ? holderPlayer.getName() : "Unknown";
            Bukkit.broadcastMessage("§c§lThe Cloud Aspect has already been claimed by §f" + holderName + "§c!");
            Bukkit.broadcastMessage("§cThe Awakening of the Ninth Sky cannot begin.");
            return;
        }

        setActive(true);
        phase.set(EventPhase.SKY_AWAKENS);
        fragmentsCollected.set(0);
        mobsSpawned.set(0);
        getParticipants().clear();
        skyGuardianEntity = null;
        riftLocations.clear();
        stormLocations.clear();
        stormIntensity.set(0);
        eventStartTime.set(System.currentTimeMillis());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                addParticipant(player.getUniqueId());
            }
        }

        Bukkit.broadcastMessage("§b§l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("§b§l   THE AWAKENING OF THE NINTH SKY HAS BEGUN");
        Bukkit.broadcastMessage("§b§l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("§7The winds howl across the world... ancient clouds gather.");
        Bukkit.broadcastMessage("§7The boundary between earth and sky grows thin.");
        Bukkit.broadcastMessage("§e§lSeek the Sky Fragments §7scattered across the overworld.");
        Bukkit.broadcastMessage("§7Defeat the Sky Wardens, survive the storms, and claim the §bCloud Aspect§7!");
        Bukkit.broadcastMessage("§b§l═══════════════════════════════════════════════");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
                plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
                    "THE AWAKENING OF THE NINTH SKY",
                    "Ancient clouds gather... The boundary between earth and sky grows thin.",
                    AspectType.CLOUD
                );
            }
        }

        schedulePhaseTransition(PHASE_DELAY_HOURS / 360, EventPhase.SKY_FRAGMENTS); // 20 seconds
        startGlobalEffects();
    }

    @Override
    public void stopEvent() {
        setActive(false);
        stopAllTasks();
        getParticipants().clear();
        fragmentsCollected.set(0);
        mobsSpawned.set(0);
        skyGuardianEntity = null;
        riftLocations.clear();
        stormLocations.clear();
        Bukkit.broadcastMessage("§cThe Awakening of the Ninth Sky has been stopped by an admin.");
    }

    private void startGlobalEffects() {
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                tick++;

                if (System.currentTimeMillis() - eventStartTime.get() > EVENT_DURATION_MS) {
                    if (phase.get() != EventPhase.SKY_COMPLETED) {
                        advancePhase(EventPhase.SKY_COMPLETED);
                    }
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

                    if (phase.get() == EventPhase.SKY_FRAGMENTS || phase.get() == EventPhase.SKY_RIFTS_OPEN) {
                        if (tick % 40 == 0) {
                            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                                player.getLocation().add(0, 2, 0), 3, 2, 1, 2, 0.01);
                            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,
                                player.getLocation().add(0, 1, 0), 1, 1, 0.5, 1, 0.005);
                        }
                    }

                    if (phase.get() == EventPhase.SKY_STORMS || phase.get() == EventPhase.SKY_GUARDIAN || phase.get() == EventPhase.SKY_CONVERGENCE) {
                        if (tick % 20 == 0) {
                            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                                player.getLocation().add(0, 3, 0), 5, 3, 2, 3, 0.02);
                            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD,
                                player.getLocation().add(0, 2, 0), 3, 2, 1, 2, 0.01);
                        }
                        if (tick % 60 == 0) {
                            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.LEVITATION, 40, 0, true, false), true);
                        }
                    }

                    if (phase.get() == EventPhase.SKY_CONVERGENCE) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.3f, 0.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        trackTask(task);
    }

    public void advancePhase(EventPhase next) {
        for (BukkitTask task : getActiveTasks()) { task.cancel(); }
        stopAllTasks();

        phase.set(next);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getVisualEffectsManager().playEventPhaseTransition(player, next, AspectType.CLOUD);
        }
        
        switch (next) {
            case SKY_FRAGMENTS -> {
                Bukkit.broadcastMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§e§l   SKY FRAGMENTS HAVE FALLEN ACROSS THE WORLD");
                Bukkit.broadcastMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§7§l" + REQUIRED_FRAGMENTS + " Sky Fragments §7are scattered across the overworld.");
                Bukkit.broadcastMessage("§7Defeat §bSky Wardens §7guarding them to collect fragments.");
                Bukkit.broadcastMessage("§7Rifts will open periodically, spawning more Wardens.");
                startFragmentPhase();
                schedulePhaseTransition(PHASE_DELAY_HOURS, EventPhase.SKY_RIFTS_OPEN); // 2 hours
            }
            case SKY_RIFTS_OPEN -> {
                Bukkit.broadcastMessage("§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§d§l   THE SKY RIFTS TEAR OPEN - STORMS APPROACH");
                Bukkit.broadcastMessage("§d§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§7Massive rifts open across the world! §bSky Wardens §7pour through.");
                Bukkit.broadcastMessage("§7Collect remaining fragments before the storms consume all!");
                openSkyRifts();
                schedulePhaseTransition(PHASE_DELAY_HOURS + 7200000, EventPhase.SKY_STORMS); // 4 hours
            }
            case SKY_STORMS -> {
                Bukkit.broadcastMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§b§l   THE TEMPEST STORMS ENGULF THE WORLD");
                Bukkit.broadcastMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§7Violent storms ravage the overworld. §bLevitation §7affects all!");
                Bukkit.broadcastMessage("§7Survive the storms and prepare for the Guardian's arrival.");
                startStorms();
                schedulePhaseTransition(PHASE_DELAY_HOURS * 2 + 7200000, EventPhase.SKY_GUARDIAN); // 5.5 hours
            }
            case SKY_GUARDIAN -> {
                Bukkit.broadcastMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§c§l   THE SKY GUARDIAN DESCENDS UPON THE WORLD");
                Bukkit.broadcastMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§7A colossal entity tears through the sky!");
                Bukkit.broadcastMessage("§7Rally together and defeat it to proceed!");
                spawnSkyGuardian();
            }
            case SKY_CONVERGENCE -> {
                Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§a§l   THE NINTH SKY CONVERGES - FINAL TRIAL");
                Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§7The Sky Guardian falls... but the true trial begins.");
                Bukkit.broadcastMessage("§7Stand at the convergence points and endure the final ascent!");
                startConvergence();
            }
            case SKY_COMPLETED -> {
                Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("§a§l   THE NINTH SKY HAS BEEN CONQUERED!");
                Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                setActive(false);
            }
            default -> {}
        }
    }

    private void schedulePhaseTransition(long delayMs, EventPhase nextPhase) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                advancePhase(nextPhase);
            }
        }.runTaskLater(plugin, delayMs / 50); // Convert ms to ticks (1 tick = 50ms)
        trackTask(task);
    }

    private void startFragmentPhase() {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive() || (phase.get() != EventPhase.SKY_FRAGMENTS && phase.get() != EventPhase.SKY_RIFTS_OPEN)) {
                    cancel();
                    return;
                }
                if (mobsSpawned.get() >= MAX_MOB_SPAWNS) return;
                spawnFragmentGuards();
            }
        }.runTaskTimer(plugin, 0L, 200L); // Every 10 seconds
        trackTask(task);
        schedulePhaseTransition(PHASE_DELAY_HOURS, EventPhase.SKY_RIFTS_OPEN);
    }

    private void spawnFragmentGuards() {
        List<Player> overworldPlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().getEnvironment() == World.Environment.NORMAL)
            .filter(p -> getParticipants().contains(p.getUniqueId()))
            .collect(java.util.stream.Collectors.toList());
        if (overworldPlayers.isEmpty()) return;

        int spawnsThisTick = Math.min(2, MAX_MOB_SPAWNS - mobsSpawned.get());
        for (int i = 0; i < spawnsThisTick; i++) {
            Player randomPlayer = overworldPlayers.get(new Random().nextInt(overworldPlayers.size()));
            Location base = randomPlayer.getLocation();
            Location spawn = new Location(
                base.getWorld(),
                base.getX() + 15 + new Random().nextInt(30) - 15,
                0,
                base.getZ() + 15 + new Random().nextInt(30) - 15
            );
            int safeY = spawn.getWorld().getHighestBlockYAt(spawn) + 1;
            spawn.setY(safeY);
            if (spawn.getBlock().getType().isSolid()) spawn.setY(safeY + 1);
            if (mobsSpawned.get() >= MAX_MOB_SPAWNS) break;
            mobsSpawned.incrementAndGet();
            switch (new Random().nextInt(3)) {
                case 0 -> new CloudWraith(plugin, spawn);
                case 1 -> new StormSentinel(plugin, spawn);
                case 2 -> new TempestPhantom(plugin, spawn);
            }
        }
    }

    private void openSkyRifts() {
        World overworld = Bukkit.getWorlds().get(0);
        riftLocations.clear();
        for (int i = 0; i < 12; i++) {
            Location rift = new Location(overworld,
                new Random().nextInt(2000) - 1000,
                0,
                new Random().nextInt(2000) - 1000
            );
            rift.setY(overworld.getHighestBlockYAt(rift) + 2);
            riftLocations.add(rift);
            overworld.spawnParticle(org.bukkit.Particle.PORTAL, rift, 100, 5, 10, 5, 0.1);
            overworld.spawnParticle(org.bukkit.Particle.END_ROD, rift, 50, 3, 5, 3, 0.05);
            overworld.playSound(rift, Sound.BLOCK_PORTAL_TRIGGER, 2.0f, 0.5f);
        }
        Bukkit.broadcastMessage("§d§l" + riftLocations.size() + " SKY RIFTS HAVE OPENED! §7Find them and defeat the Wardens within!");

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isActive() || phase.get() != EventPhase.SKY_RIFTS_OPEN) { cancel(); return; }
                ticks++;
                for (Location rift : riftLocations) {
                    if (Math.random() < 0.3 && mobsSpawned.get() < MAX_MOB_SPAWNS) {
                        mobsSpawned.incrementAndGet();
                        new CloudWraith(plugin, rift.clone().add(new Random().nextInt(10) - 5, 0, new Random().nextInt(10) - 5));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
        trackTask(task);
    }

    private void startStorms() {
        World overworld = Bukkit.getWorlds().get(0);
        stormLocations.clear();
        for (int i = 0; i < 8; i++) {
            Location storm = new Location(overworld,
                new Random().nextInt(2000) - 1000,
                255,
                new Random().nextInt(2000) - 1000
            );
            stormLocations.add(storm);
        }

        BukkitTask stormTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!isActive() || (phase.get() != EventPhase.SKY_STORMS && phase.get() != EventPhase.SKY_GUARDIAN)) { cancel(); return; }
                tick++;
                stormIntensity.set(Math.min(3, tick / 2000));

                for (Location storm : stormLocations) {
                    int intensity = stormIntensity.get();
                    storm.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, storm, 50 + intensity * 20, 20, 10, 20, 0.05);
                    storm.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, storm, 30 + intensity * 10, 15, 5, 15, 0.03);
                    storm.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, storm, 10 + intensity * 5, 10, 5, 10, 0.02);

                    if (tick % 40 == 0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld() == storm.getWorld() && player.getLocation().distanceSquared(storm) < 2500) {
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.LEVITATION, 60, intensity, true, false), true);
                                if (intensity >= 2 && tick % 80 == 0) {
                                    player.damage(1.0);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        trackTask(stormTask);

        BukkitTask spawnTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isActive() || phase.get() != EventPhase.SKY_STORMS) { cancel(); return; }
                ticks++;
                if (ticks % 300 == 0 && mobsSpawned.get() < MAX_MOB_SPAWNS) {
                    Location storm = stormLocations.get(new Random().nextInt(stormLocations.size()));
                    mobsSpawned.addAndGet(2);
                    new StormSentinel(plugin, storm.clone().add(new Random().nextInt(20) - 10, 0, new Random().nextInt(20) - 10));
                    new TempestPhantom(plugin, storm.clone().add(new Random().nextInt(20) - 10, 0, new Random().nextInt(20) - 10));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        trackTask(spawnTask);
    }

    private void spawnSkyGuardian() {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = new Location(overworld,
            new Random().nextInt(1000) - 500,
            255,
            new Random().nextInt(1000) - 500
        );
        spawn.setY(overworld.getHighestBlockYAt(spawn) + 50);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.sendTitle("§c§lSKY GUARDIAN", "§7Descending from the heavens...", 20, 100, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
            }
        }

        SkyGuardian guardian = new SkyGuardian(plugin, spawn);
        skyGuardianEntity = guardian.getEntity();

        BukkitTask enrageTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isActive() || skyGuardianEntity == null || skyGuardianEntity.isDead()) { cancel(); return; }
                ticks++;
                if (ticks % 2400 == 0) { // Every 2 minutes
                    double hpPercent = skyGuardianEntity.getHealth() / skyGuardianEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                    if (hpPercent < 0.5) {
                        skyGuardianEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.STRENGTH, 2400, 1, true, false), true);
                        skyGuardianEntity.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, skyGuardianEntity.getLocation(), 10, 2, 2, 2, 0.2);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        trackTask(enrageTask);
    }

    private void startConvergence() {
        World overworld = Bukkit.getWorlds().get(0);
        BukkitTask convergenceTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!isActive() || phase.get() != EventPhase.SKY_CONVERGENCE) { cancel(); return; }
                tick++;

                if (tick % 600 == 0) { // Every 30 seconds
                    int livingParticipants = (int) getParticipants().stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .filter(p -> p.isOnline() && p.getWorld().getEnvironment() == World.Environment.NORMAL)
                        .count();

                    if (livingParticipants == 0) {
                        Bukkit.broadcastMessage("§cAll participants have fallen... The Ninth Sky retreats.");
                        stopEvent();
                        cancel();
                        return;
                    }

                    Bukkit.broadcastMessage("§e§lConvergence active: §a" + livingParticipants + " §esurvivors remain.");
                }

                for (Location rift : riftLocations) {
                    rift.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, rift, 100, 10, 20, 10, 0.2);
                    rift.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, rift, 50, 10, 10, 10, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        trackTask(convergenceTask);

        BukkitTask checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive() || phase.get() != EventPhase.SKY_CONVERGENCE) { cancel(); return; }
                int atConvergence = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!getParticipants().contains(player.getUniqueId())) continue;
                    for (Location rift : riftLocations) {
                        if (player.getLocation().distanceSquared(rift) <= 100) {
                            atConvergence++;
                            break;
                        }
                    }
                }
                if (atConvergence >= Math.max(1, getParticipants().size() / 2)) {
                    Bukkit.broadcastMessage("§a§lTHE CONVERGENCE IS COMPLETE!");
                    Bukkit.broadcastMessage("§7The Ninth Sky stabilizes... The worthy shall claim their reward.");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        trackTask(checkTask);

        schedulePhaseTransition(1800000L, EventPhase.SKY_COMPLETED); // 30 minutes
    }

    public void collectFragment(Player player) {
        if (!isActive() || (phase.get() != EventPhase.SKY_FRAGMENTS && phase.get() != EventPhase.SKY_RIFTS_OPEN)) return;
        if (!getParticipants().contains(player.getUniqueId())) return;
        int collected = fragmentsCollected.incrementAndGet();
        player.sendMessage("§aSky Fragment collected! §7(" + collected + "/" + REQUIRED_FRAGMENTS + ")");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        if (collected >= REQUIRED_FRAGMENTS) {
            Bukkit.broadcastMessage("§e§lALL SKY FRAGMENTS RECOVERED!");
            Bukkit.broadcastMessage("§7The Sky Rifts begin to open...");
            if (phase.get() == EventPhase.SKY_FRAGMENTS) advancePhase(EventPhase.SKY_RIFTS_OPEN);
        }
    }

    @Override
    public void addParticipant(UUID uuid) {
        if (isActive() && (phase.get() == EventPhase.SKY_AWAKENS || phase.get() == EventPhase.SKY_FRAGMENTS || phase.get() == EventPhase.SKY_RIFTS_OPEN)) {
            getParticipants().add(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage("§eYou have joined the Awakening of the Ninth Sky!");
            }
        }
    }

    public void awardWinner(Player winner) {
        if (winner == null || !winner.isOnline()) return;
        Heart heart = plugin.getHeartManager().getHeart(winner.getUniqueId()).orElse(null);
        if (heart == null) return;

        int previousTier = heart.getTier();
        heart.setAspect(AspectType.CLOUD);
        heart.setTier(previousTier >= 2 ? previousTier : 1);
        heart.setCloudUnlocked(true);
        heart.setStability(100);
        heart.setDormant(false);
        heart.getCooldowns().clear();
        plugin.getHeartManager().saveHeart(heart);

        plugin.claimAspect(AspectType.CLOUD, winner.getUniqueId());

        ItemStack cloudheart = new Cloudheart().createItemStack();
        Map<Integer, ItemStack> leftover = winner.getInventory().addItem(cloudheart);
        if (!leftover.isEmpty()) {
            winner.getWorld().dropItemNaturally(winner.getLocation(), leftover.values().iterator().next());
        }

        winner.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("§b§l   YOU HAVE CONQUERED THE NINTH SKY!");
        winner.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("§a§lYou are the Champion of the Ninth Sky!");
        winner.sendMessage("§7You have unlocked the §bCloud Aspect §7and received a §bCloudheart§7!");

        Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("§a§l" + winner.getName() + " §ahas conquered the Ninth Sky!");
        Bukkit.broadcastMessage("§7They are now the §bSOLE HOLDER §7of the Cloud Aspect and carry the §bCloudheart§7.");
        Bukkit.broadcastMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        winner.getWorld().strikeLightningEffect(winner.getLocation());
        for (int i = 0; i < 5; i++) {
            winner.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, winner.getLocation().add(0, 2, 0), 50, 2, 2, 2, 0.1);
            winner.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, winner.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);
        }
        
        int tier = heart != null ? heart.getTier() : 2;
        plugin.getVisualEffectsManager().playHeartUpgradeVisuals(winner, tier);
        plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
            "CHAMPION OF THE NINTH SKY",
            winner.getName() + " has claimed the Cloud Aspect!",
            AspectType.CLOUD
        );
    }

    public boolean isCompleted() {
        return !isActive() && phase.get() == EventPhase.SKY_COMPLETED;
    }

    public void enforceCloudheartRules() {
        // Enforce Cloudheart rules - only the holder can use it during the event
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