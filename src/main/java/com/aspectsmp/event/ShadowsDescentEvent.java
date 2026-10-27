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

public class ShadowsDescentEvent extends BaseEvent {

    private EventPhase phase = EventPhase.SHADOW_AWAKENS;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int fragmentsCollected = 0;
    private static final int REQUIRED_FRAGMENTS = 40;
    private static final int MAX_MOB_SPAWNS = 400;
    private int mobsSpawned = 0;
    private LivingEntity shadowLordEntity;
    private BukkitTask shadowEffectsTask;
    private BukkitTask mobSpamTask;
    private BukkitTask convergenceTask;
    private final List<BukkitTask> scheduledTransitions = new ArrayList<>();
    private final List<Location> riftLocations = new ArrayList<>();
    private final List<Location> stalkerLocations = new ArrayList<>();
    private int stalkerIntensity = 0;
    private long eventStartTime = 0;
    private static final long EVENT_DURATION_MS = 7L * 24 * 60 * 60 * 1000;
    private final Map<UUID, Integer> playerShadowStacks = new ConcurrentHashMap<>();

    public ShadowsDescentEvent(AspectSMP plugin) {
        super(plugin);
    }

    public boolean isActive() {
        return active;
    }

    public EventPhase getPhase() {
        return phase;
    }

    public LivingEntity getShadowLordEntity() {
        return shadowLordEntity;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public void startEvent() {
        if (active) return;
        
        // Check if Shadow Aspect is already claimed
        if (plugin.isAspectClaimed(AspectType.SHADOW)) {
            UUID holder = plugin.getAspectHolder(AspectType.SHADOW);
            Player holderPlayer = Bukkit.getPlayer(holder);
            String holderName = holderPlayer != null ? holderPlayer.getName() : "Unknown";
            Bukkit.broadcastMessage("\u00a7c\u00a7lThe Shadow Aspect has already been claimed by \u00a7f" + holderName + "\u00a7c!");
            Bukkit.broadcastMessage("\u00a7cThe Shadow's Descent cannot begin.");
            return;
        }
        
        active = true;
        phase = EventPhase.SHADOW_AWAKENS;
        fragmentsCollected = 0;
        mobsSpawned = 0;
        participants.clear();
        shadowLordEntity = null;
        riftLocations.clear();
        stalkerLocations.clear();
        stalkerIntensity = 0;
        playerShadowStacks.clear();
        eventStartTime = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                participants.add(player.getUniqueId());
            }
        }

        Bukkit.broadcastMessage("\u00a78\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a78\u00a7l    THE SHADOW'S DESCENT HAS BEGUN");
        Bukkit.broadcastMessage("\u00a78\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a77The sun dims unnaturally... shadows lengthen across the land.");
        Bukkit.broadcastMessage("\u00a77Ancient darkness stirs beneath the world.");
        Bukkit.broadcastMessage("\u00a7e\u00a7lCollect Shadow Fragments \u00a77scattered across the overworld.");
        Bukkit.broadcastMessage("\u00a77Defeat \u00a78Shadow Stalkers\u00a77, survive the darkness, and claim the \u00a78Shadow Aspect\u00a77!");
        Bukkit.broadcastMessage("\u00a78\u00a7l═══════════════════════════════════════════════");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
                plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
                    "THE SHADOW'S DESCENT",
                    "The sun dims unnaturally... shadows lengthen across the land.",
                    AspectType.SHADOW
                );
            }
        }

        schedulePhaseTransition(200L, EventPhase.SHADOW_FRAGMENTS);
        startGlobalEffects();
    }

    public void stopEvent() {
        active = false;
        cancelAllTasks();
        participants.clear();
        fragmentsCollected = 0;
        mobsSpawned = 0;
        shadowLordEntity = null;
        riftLocations.clear();
        stalkerLocations.clear();
        playerShadowStacks.clear();
        Bukkit.broadcastMessage("\u00a7cThe Shadow's Descent has been stopped by an admin.");
    }

    private void cancelAllTasks() {
        if (shadowEffectsTask != null) shadowEffectsTask.cancel();
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
        shadowEffectsTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                tick++;

                if (System.currentTimeMillis() - eventStartTime > EVENT_DURATION_MS) {
                    if (phase != EventPhase.SHADOW_COMPLETED) {
                        advancePhase(EventPhase.SHADOW_COMPLETED);
                    }
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

                    if (phase == EventPhase.SHADOW_FRAGMENTS || phase == EventPhase.SHADOW_RIFTS) {
                        if (tick % 40 == 0) {
                            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 2, 1, 0.5, 1, 0.01);
                            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.005);
                        }
                    }

                    if (phase == EventPhase.SHADOW_STALKERS || phase == EventPhase.SHADOW_LORD || phase == EventPhase.SHADOW_CONVERGENCE) {
                        if (tick % 20 == 0) {
                            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 3, 2, 1, 2, 0.02);
                            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 2, 1, 0.5, 1, 0.01);
                        }
                        if (tick % 60 == 0) {
                            int stacks = playerShadowStacks.getOrDefault(player.getUniqueId(), 0);
                            if (stacks < 5) {
                                playerShadowStacks.put(player.getUniqueId(), stacks + 1);
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.BLINDNESS, 120, Math.min(stacks, 2), true, false), true);
                            }
                        }
                    }

                    if (phase == EventPhase.SHADOW_CONVERGENCE) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.2f, 0.3f);
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
            plugin.getVisualEffectsManager().playEventPhaseTransition(player, next, AspectType.SHADOW);
        }
        
        switch (next) {
            case SHADOW_FRAGMENTS -> {
                Bukkit.broadcastMessage("\u00a77\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77\u00a7l   SHADOW FRAGMENTS HAVE FALLEN ACROSS THE WORLD");
                Bukkit.broadcastMessage("\u00a77\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77\u00a7l" + REQUIRED_FRAGMENTS + " Shadow Fragments \u00a77are scattered across the overworld.");
                Bukkit.broadcastMessage("\u00a77Defeat \u00a78Shadow Stalkers \u00a77guarding them to collect fragments.");
                Bukkit.broadcastMessage("\u00a77Rifts will open periodically, spawning more Stalkers.");
                startFragmentPhase();
                schedulePhaseTransition(20000L, EventPhase.SHADOW_RIFTS);
            }
            case SHADOW_RIFTS -> {
                Bukkit.broadcastMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a78\u00a7l   SHADOW RIFTS TEAR OPEN - DARKNESS SPREADS");
                Bukkit.broadcastMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Rifts open across the world! \u00a78Shadow Stalkers \u00a77pour through.");
                Bukkit.broadcastMessage("\u00a77Collect remaining fragments before the darkness consumes all!");
                openShadowRifts();
                schedulePhaseTransition(30000L, EventPhase.SHADOW_STALKERS);
            }
            case SHADOW_STALKERS -> {
                Bukkit.broadcastMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a78\u00a7l   THE SHADOW STALKERS HUNT THE LIVING");
                Bukkit.broadcastMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Elite shadow entities hunt players across the world.");
                Bukkit.broadcastMessage("\u00a77Blindness stacks accumulate in the dark. Stay in light!");
                startStalkers();
                schedulePhaseTransition(40000L, EventPhase.SHADOW_LORD);
            }
            case SHADOW_LORD -> {
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7c\u00a7l   THE SHADOW LORD EMERGES FROM THE VOID");
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77A colossal entity of pure darkness manifests!");
                Bukkit.broadcastMessage("\u00a77Rally together and defeat it to proceed!");
                spawnShadowLord();
            }
            case SHADOW_CONVERGENCE -> {
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7a\u00a7l   THE SHADOW CONVERGES - FINAL TRIAL");
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77The Shadow Lord falls... but the true trial begins.");
                Bukkit.broadcastMessage("\u00a77Stand at the convergence points and endure the final darkness!");
                startConvergence();
            }
            case SHADOW_COMPLETED -> {
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7a\u00a7l   THE SHADOW'S DESCENT HAS BEEN CONQUERED!");
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
                if (!active || phase != EventPhase.SHADOW_FRAGMENTS && phase != EventPhase.SHADOW_RIFTS) {
                    cancel();
                    mobSpamTask = null;
                    return;
                }
                if (mobsSpawned >= MAX_MOB_SPAWNS) return;
                spawnFragmentGuards();
            }
        }.runTaskTimer(plugin, 0L, 200L);
        schedulePhaseTransition(20000L, EventPhase.SHADOW_RIFTS);
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
            if (mobsSpawned >= MAX_MOB_SPAWNS) break;
            mobsSpawned++;
            // Shadow-themed mobs
            switch (new Random().nextInt(3)) {
                case 0 -> new CloudWraith(plugin, spawn); // Reuse as shadow wraith
                case 1 -> new StormSentinel(plugin, spawn); // Reuse as shadow sentinel
                case 2 -> new TempestPhantom(plugin, spawn); // Reuse as shadow phantom
            }
        }
    }

    private void openShadowRifts() {
        World overworld = Bukkit.getWorlds().get(0);
        riftLocations.clear();
        for (int i = 0; i < 10; i++) {
            Location rift = new Location(overworld,
                new Random().nextInt(2000) - 1000,
                0,
                new Random().nextInt(2000) - 1000
            );
            rift.setY(overworld.getHighestBlockYAt(rift) + 2);
            riftLocations.add(rift);
            overworld.spawnParticle(Particle.PORTAL, rift, 80, 4, 8, 4, 0.1);
            overworld.spawnParticle(Particle.SOUL, rift, 40, 3, 4, 3, 0.05);
            overworld.playSound(rift, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.3f);
        }
        Bukkit.broadcastMessage("\u00a78\u00a7l" + riftLocations.size() + " SHADOW RIFTS HAVE OPENED! \u00a77Find them and defeat the Stalkers within!");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.SHADOW_RIFTS) { cancel(); return; }
                ticks++;
                for (Location rift : riftLocations) {
                    if (Math.random() < 0.25 && mobsSpawned < MAX_MOB_SPAWNS) {
                        mobsSpawned++;
                        new CloudWraith(plugin, rift.clone().add(new Random().nextInt(10) - 5, 0, new Random().nextInt(10) - 5));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L);
    }

    private void startStalkers() {
        World overworld = Bukkit.getWorlds().get(0);
        stalkerLocations.clear();
        for (int i = 0; i < 8; i++) {
            Location stalker = new Location(overworld,
                new Random().nextInt(2000) - 1000,
                0,
                new Random().nextInt(2000) - 1000
            );
            stalker.setY(overworld.getHighestBlockYAt(stalker) + 2);
            stalkerLocations.add(stalker);
        }

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.SHADOW_STALKERS && phase != EventPhase.SHADOW_LORD) { cancel(); return; }
                tick++;
                stalkerIntensity = Math.min(3, tick / 2000);

                for (Location stalker : stalkerLocations) {
                    stalker.getWorld().spawnParticle(Particle.SOUL, stalker, 30 + stalkerIntensity * 15, 15, 5, 15, 0.03);
                    stalker.getWorld().spawnParticle(Particle.SMOKE, stalker, 20 + stalkerIntensity * 10, 10, 3, 10, 0.02);

                    if (tick % 40 == 0) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getWorld() == stalker.getWorld() && player.getLocation().distanceSquared(stalker) < 2500) {
                                int stacks = playerShadowStacks.getOrDefault(player.getUniqueId(), 0);
                                if (stacks < 5) {
                                    playerShadowStacks.put(player.getUniqueId(), stacks + 1);
                                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.BLINDNESS, 100, Math.min(stacks, 2), true, false), true);
                                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.SLOWNESS, 100, Math.min(stacks, 1), true, false), true);
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
                if (!active || phase != EventPhase.SHADOW_STALKERS) { cancel(); return; }
                ticks++;
                if (ticks % 300 == 0 && mobsSpawned < MAX_MOB_SPAWNS) {
                    Location stalker = stalkerLocations.get(new Random().nextInt(stalkerLocations.size()));
                    mobsSpawned += 2;
                    new StormSentinel(plugin, stalker.clone().add(new Random().nextInt(20) - 10, 0, new Random().nextInt(20) - 10));
                    new TempestPhantom(plugin, stalker.clone().add(new Random().nextInt(20) - 10, 0, new Random().nextInt(20) - 10));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnShadowLord() {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = new Location(overworld,
            new Random().nextInt(1000) - 500,
            0,
            new Random().nextInt(1000) - 500
        );
        spawn.setY(overworld.getHighestBlockYAt(spawn) + 50);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.sendTitle("\u00a78\u00a7lSHADOW LORD", "\u00a77Rises from the abyss...", 20, 100, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 0.3f);
            }
        }

        // Use a powerful mob as shadow lord
        com.aspectsmp.mobs.SkyGuardian guardian = new com.aspectsmp.mobs.SkyGuardian(plugin, spawn);
        shadowLordEntity = guardian.getEntity();
        
        // Customize for shadow theme
        shadowLordEntity.setCustomName("\u00a78\u00a7lThe Shadow Lord");
        shadowLordEntity.setCustomNameVisible(true);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!active || shadowLordEntity == null || shadowLordEntity.isDead()) { cancel(); return; }
                ticks++;
                if (ticks % 2400 == 0) {
                    double hpPercent = shadowLordEntity.getHealth() / shadowLordEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                    if (hpPercent < 0.5) {
                        shadowLordEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.STRENGTH, 2400, 1, true, false), true);
                        shadowLordEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.INVISIBILITY, 2400, 0, true, false), true);
                        shadowLordEntity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, shadowLordEntity.getLocation(), 15, 2, 2, 2, 0.2);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startConvergence() {
        World overworld = Bukkit.getWorlds().get(0);
        convergenceTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.SHADOW_CONVERGENCE) { cancel(); return; }
                tick++;

                if (tick % 600 == 0) {
                    int livingParticipants = (int) participants.stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .filter(p -> p.isOnline() && p.getWorld().getEnvironment() == World.Environment.NORMAL)
                        .count();

                    if (livingParticipants == 0) {
                        Bukkit.broadcastMessage("\u00a7cAll participants have fallen... The Shadow retreats.");
                        stopEvent();
                        cancel();
                        return;
                    }

                    Bukkit.broadcastMessage("\u00a77\u00a7lConvergence active: \u00a7a" + livingParticipants + " \u00a77survivors remain in the light.");
                }

                for (Location rift : riftLocations) {
                    rift.getWorld().spawnParticle(Particle.SOUL, rift, 80, 8, 15, 8, 0.15);
                    rift.getWorld().spawnParticle(Particle.SMOKE, rift, 40, 8, 8, 8, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.SHADOW_CONVERGENCE) { cancel(); return; }
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
                    Bukkit.broadcastMessage("\u00a7a\u00a7lTHE SHADOW CONVERGENCE IS COMPLETE!");
                    Bukkit.broadcastMessage("\u00a77The darkness stabilizes... The worthy shall claim their reward.");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        schedulePhaseTransition(60000L, EventPhase.SHADOW_COMPLETED);
    }

    public void collectFragment(Player player) {
        if (!active || (phase != EventPhase.SHADOW_FRAGMENTS && phase != EventPhase.SHADOW_RIFTS)) return;
        if (!participants.contains(player.getUniqueId())) return;
        fragmentsCollected++;
        player.sendMessage("\u00a7aShadow Fragment collected! \u00a77(" + fragmentsCollected + "/" + REQUIRED_FRAGMENTS + ")");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        if (fragmentsCollected >= REQUIRED_FRAGMENTS) {
            Bukkit.broadcastMessage("\u00a7e\u00a7lALL SHADOW FRAGMENTS RECOVERED!");
            Bukkit.broadcastMessage("\u00a77The Shadow Rifts begin to tear open...");
            if (mobSpamTask != null) { mobSpamTask.cancel(); mobSpamTask = null; }
            if (phase == EventPhase.SHADOW_FRAGMENTS) advancePhase(EventPhase.SHADOW_RIFTS);
        }
    }

    public void addParticipant(Player player) {
        if (active && (phase == EventPhase.SHADOW_AWAKENS || phase == EventPhase.SHADOW_FRAGMENTS || phase == EventPhase.SHADOW_RIFTS)) {
            participants.add(player.getUniqueId());
            player.sendMessage("\u00a7eYou have joined the Shadow's Descent!");
        }
    }

    public void awardWinner(Player winner) {
        if (winner == null || !winner.isOnline()) return;
        Heart heart = plugin.getHeartManager().getHeart(winner.getUniqueId()).orElse(null);
        if (heart == null) return;

        int previousTier = heart.getTier();
        heart.setAspect(AspectType.SHADOW);
        heart.setTier(previousTier >= 2 ? previousTier : 1);
        heart.setCloudUnlocked(true); // Reuse field for Shadow
        heart.setStability(100);
        heart.setDormant(false);
        heart.getCooldowns().clear();
        plugin.getHeartManager().saveHeart(heart);

        // Claim the aspect for this player
        plugin.claimAspect(AspectType.SHADOW, winner.getUniqueId());

        ItemStack shadowheart = createShadowheart();
        Map<Integer, ItemStack> leftover = winner.getInventory().addItem(shadowheart);
        if (!leftover.isEmpty()) {
            winner.getWorld().dropItemNaturally(winner.getLocation(), leftover.values().iterator().next());
        }

        winner.sendMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("\u00a78\u00a7l   YOU HAVE SURVIVED THE SHADOW'S DESCENT!");
        winner.sendMessage("\u00a78\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("\u00a7a\u00a7lYou are the Master of Shadows!");
        winner.sendMessage("\u00a77You have unlocked the \u00a78Shadow Aspect \u00a77and received a \u00a78Shadowheart\u00a77!");

        Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("\u00a7a\u00a7l" + winner.getName() + " \u00a7ahas conquered the Shadow's Descent!");
        Bukkit.broadcastMessage("\u00a77They are now the \u00a7bSOLE HOLDER \u00a77of the Shadow Aspect and carry the \u00a78Shadowheart\u00a77.");
        Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        winner.getWorld().strikeLightningEffect(winner.getLocation());
        for (int i = 0; i < 5; i++) {
            winner.getWorld().spawnParticle(Particle.SOUL, winner.getLocation().add(0, 2, 0), 50, 2, 2, 2, 0.1);
            winner.getWorld().spawnParticle(Particle.SMOKE, winner.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);
        }
        
        // Play visual effects for winner
        int tier = heart != null ? heart.getTier() : 2;
        plugin.getVisualEffectsManager().playHeartUpgradeVisuals(winner, tier);
        plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
            "MASTER OF SHADOWS",
            winner.getName() + " has conquered the Shadow's Descent!",
            AspectType.SHADOW
        );
    }

    private ItemStack createShadowheart() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a78\u00a7lShadowheart");
        meta.setLore(Arrays.asList(
            "\u00a77A crystallized fragment of pure darkness.",
            "\u00a77Grants the \u00a78Shadow Aspect \u00a77permanently.",
            "\u00a78\u00a7lRight-click to activate Shadow Meld."
        ));
        setCustomModelDataString(item, "shadowheart");
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCompleted() {
        return !active && phase == EventPhase.SHADOW_COMPLETED;
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


