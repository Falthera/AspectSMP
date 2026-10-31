package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.FrostMonarch;
import com.aspectsmp.mobs.Frostborn;
import com.aspectsmp.mobs.GlacierTitan;
import com.aspectsmp.mobs.IceWraith;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FrozenEclipseEvent extends BaseEvent {

    private EventPhase phase = EventPhase.FIRST_FROST;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int guardiansDefeated = 0;
    private int guardiansRequired = 0;

    private final Map<UUID, Long> lastFrostDamage = new ConcurrentHashMap<>();
    private static final long FROST_DAMAGE_INTERVAL_MS = 1000;

    private UUID winner;
    private LivingEntity frostMonarchEntity;
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private final List<Location> brazierLocations = new ArrayList<>();
    private int braziersLit = 0;
    private int brazierAllLitTimer = 0;
    private boolean portalOpened = false;
    private int frostStage = 0;
    private int trialsElapsedTicks = 0;
    private int coldDamageInterval = 40;
    private int monarchStage = 1;
    private World overworld;
    private long eventStartTime = 0;
    private static final long EVENT_DURATION_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    private List<Location> frozenMonoliths = new ArrayList<>();
    private int monolithsActivated = 0;
    private static final int REQUIRED_MONOLITHS = 8;

    // Missing fields
    private boolean frostMonarchSpawned = false;
    private BukkitTask monarchStageTask;
    private BukkitTask crucibleBorderTask;

    public FrozenEclipseEvent(AspectSMP plugin) {
        super(plugin);
        this.overworld = Bukkit.getWorld("world");
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

    public LivingEntity getFrostMonarchEntity() {
        return frostMonarchEntity;
    }

    public List<Location> getFrozenMonoliths() {
        return frozenMonoliths;
    }

    public void addParticipant(Player player) {
        if (!active || phase != EventPhase.FIRST_FROST && phase != EventPhase.DEEP_FREEZE) return;
        if (!participants.contains(player.getUniqueId())) {
            participants.add(player.getUniqueId());
            player.sendMessage("\u00a7b\u00a7lYou have joined the Frozen Eclipse ritual!");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        }
    }

    public void startEvent() {
        if (active) return;
        
        // Check if Winter Aspect is already claimed
        if (plugin.isAspectClaimed(AspectType.WINTER)) {
            UUID holder = plugin.getAspectHolder(AspectType.WINTER);
            Player holderPlayer = Bukkit.getPlayer(holder);
            String holderName = holderPlayer != null ? holderPlayer.getName() : "Unknown";
            Bukkit.broadcastMessage("\u00a7c\u00a7lThe Winter Aspect has already been claimed by \u00a7f" + holderName + "\u00a7c!");
            Bukkit.broadcastMessage("\u00a7cThe Frozen Eclipse cannot begin.");
            return;
        }
        
        active = true;
        phase = EventPhase.FIRST_FROST;
        guardiansDefeated = 0;
        participants.clear();
        winner = null;
        frostMonarchEntity = null;
        brazierLocations.clear();
        braziersLit = 0;
        brazierAllLitTimer = 0;
        portalOpened = false;
        frostStage = 0;
        trialsElapsedTicks = 0;
        coldDamageInterval = 40;
        frostMonarchEntity = null;
        monarchStage = 1;
        frozenMonoliths.clear();
        monolithsActivated = 0;
        eventStartTime = System.currentTimeMillis();

        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a7b\u00a7l      THE FROZEN ECLIPSE HAS BEGUN");
        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");
        Bukkit.broadcastMessage("\u00a77An unnatural winter descends upon the overworld...");
        Bukkit.broadcastMessage("\u00a77The sun dims. Frost creeps across the land.");
        Bukkit.broadcastMessage("\u00a7e\u00a7lFind the Frozen Monoliths \u00a77and activate them to survive the Deep Freeze.");
        Bukkit.broadcastMessage("\u00a77Defeat the Ice Titans, brave the trials, and claim the \u00a7bWinter Aspect\u00a77!");
        Bukkit.broadcastMessage("\u00a7b\u00a7l═══════════════════════════════════════════════");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 0.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
                    "THE FROZEN ECLIPSE",
                    "An unnatural winter descends... The sun dims. Frost creeps across the land.",
                    AspectType.WINTER
                );
            }
        }

        startFrostEffects();
        schedulePhaseTransition(20L, this::sendJoinBroadcast);
        schedulePhaseTransition(200L, () -> {}); // Brief pause
        schedulePhaseTransition(180000L, EventPhase.DEEP_FREEZE); // 2.5 hours = 180000 ticks
    }

    private void sendJoinBroadcast() {
        Component joinMessage = Component.text("[THE FROZEN ECLIPSE HAS BEGUN]")
            .color(NamedTextColor.DARK_BLUE)
            .append(Component.newline())
            .append(Component.text("Will you fight the cold or freeze in obscurity?"))
            .color(NamedTextColor.GRAY)
            .append(Component.newline())
            .append(Component.text("[CLICK HERE TO JOIN THE RITUAL]"))
            .color(NamedTextColor.AQUA)
            .clickEvent(ClickEvent.runCommand("/eclipse join"))
            .append(Component.text(" (or type /eclipse join)"))
            .color(NamedTextColor.DARK_GRAY);

        Bukkit.getServer().sendMessage(joinMessage);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 0.5f);
            }
        }
    }

    private void startFrostEffects() {
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase == EventPhase.WINTER_COMPLETED) {
                    cancel();
                    return;
                }
                tick++;
                int elapsedSeconds = tick / 20;

                if (elapsedSeconds >= 10800) frostStage = 4; // 3 hours
                else if (elapsedSeconds >= 7200) frostStage = 3; // 2 hours
                else if (elapsedSeconds >= 3600) frostStage = 2; // 1 hour
                else if (elapsedSeconds >= 1800) frostStage = 1; // 30 min

                if (System.currentTimeMillis() - eventStartTime > EVENT_DURATION_MS) {
                    if (phase != EventPhase.WINTER_COMPLETED) {
                        advancePhase(EventPhase.WINTER_COMPLETED);
                    }
                    cancel();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

                    if (frostStage >= 0 && tick % 20 == 0) {
                        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE,
                            player.getLocation().add(0, 2, 0), 5, 3, 1, 3, 0.02);
                        if (frostStage >= 2) {
                            player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE,
                                player.getLocation().add(0, 1, 0), 3, 2, 1, 2, 0.01);
                        }
                    }

                    if (frostStage >= 1) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
                    }

                    if (frostStage >= 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 0, true, false));
                    }

                    if (frostStage >= 3) {
                        if (!participants.contains(player.getUniqueId()) || !isNearHeatSource(player)) {
                            if (System.currentTimeMillis() - lastFrostDamage.getOrDefault(player.getUniqueId(), 0L) > FROST_DAMAGE_INTERVAL_MS) {
                                player.damage(frostStage >= 4 ? 2.0 : 1.0);
                                lastFrostDamage.put(player.getUniqueId(), System.currentTimeMillis());
                            }
                        }
                    }

                    if (frostStage >= 4) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(task);
    }

    private boolean isNearHeatSource(Player player) {
        Location loc = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    org.bukkit.block.Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getType() == org.bukkit.Material.TORCH ||
                        block.getType() == org.bukkit.Material.CAMPFIRE ||
                        block.getType() == org.bukkit.Material.LANTERN ||
                        block.getType() == org.bukkit.Material.FIRE ||
                        block.getType() == org.bukkit.Material.SOUL_CAMPFIRE ||
                        block.getType() == org.bukkit.Material.LAVA ||
                        block.getType() == org.bukkit.Material.MAGMA_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void schedulePhaseTransition(long delay, EventPhase nextPhase) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                activeTasks.remove(this);
                if (!active) { cancel(); return; }
                advancePhase(nextPhase);
            }
        }.runTaskLater(plugin, delay);
        activeTasks.add(task);
    }

    private void schedulePhaseTransition(long delay, Runnable task) {
        BukkitTask t = new BukkitRunnable() {
            @Override
            public void run() {
                activeTasks.remove(this);
                if (!active) { cancel(); return; }
                task.run();
            }
        }.runTaskLater(plugin, delay);
        activeTasks.add(t);
    }

    public void advancePhase(EventPhase next) {
        this.phase = next;
        
        // Play visual effects for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getVisualEffectsManager().playEventPhaseTransition(player, next, AspectType.WINTER);
        }
        
        switch (next) {
            case DEEP_FREEZE -> {
                Bukkit.broadcastMessage("\u00a7e\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7e\u00a7l   THE DEEP FREEZE GRIPS THE WORLD");
                Bukkit.broadcastMessage("\u00a7e\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Temperature plummets. \u00a7b" + REQUIRED_MONOLITHS + " Frozen Monoliths \u00a77have awakened.");
                Bukkit.broadcastMessage("\u00a77Find and activate them with fire to stave off the cold!");
                guardiansRequired = 5 + (participants.size() * 3);
                startDeepFreeze();
            }
            case FROZEN_TITANS -> {
                Bukkit.broadcastMessage("\u00a7d\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7d\u00a7l   THE FROZEN TITANS AWAKEN FROM THEIR SLUMBER");
                Bukkit.broadcastMessage("\u00a7d\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77Colossal ice beings tear through the frozen wastes!");
                Bukkit.broadcastMessage("\u00a77Defeat them all to reach the Frost Monarch!");
                startFrozenTitans();
            }
            case FROST_MONARCH -> {
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7c\u00a7l   THE FROST MONARCH RISES FROM THE PERMAFROST");
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a77The ancient ruler of winter awakens!");
                Bukkit.broadcastMessage("\u00a77Defeat it to enter the Frozen Crucible!");
                spawnFrostMonarch();
            }
            case FROZEN_CRUCIBLE -> {
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7c\u00a7l   THE MONARCH IS DEAD. ONLY ONE MAY CLAIM THE WINTER.");
                Bukkit.broadcastMessage("\u00a7c\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                startFrozenCrucible();
            }
            case WINTER_COMPLETED -> {
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("\u00a7a\u00a7l   THE FROZEN ECLIPSE HAS BEEN CONQUERED!");
                Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                grantWinterAspect();
                stopEvent();
            }
            default -> {}
        }
    }

    private void startDeepFreeze() {
        // Create Frozen Monoliths across the world
        frozenMonoliths.clear();
        for (int i = 0; i < REQUIRED_MONOLITHS; i++) {
            Location monolith = new Location(overworld,
                ThreadLocalRandom.current().nextInt(3000) - 1500,
                0,
                ThreadLocalRandom.current().nextInt(3000) - 1500
            );
            monolith.setY(overworld.getHighestBlockYAt(monolith) + 1);
            frozenMonoliths.add(monolith);

            // Build monolith structure
            buildMonolith(monolith);

            // Particles and sound
            overworld.spawnParticle(org.bukkit.Particle.END_ROD, monolith.add(0, 3, 0), 50, 2, 3, 2, 0.05);
            overworld.spawnParticle(org.bukkit.Particle.SNOWFLAKE, monolith.add(0, 2, 0), 30, 2, 2, 2, 0.02);
            overworld.playSound(monolith, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
        }

        Bukkit.broadcastMessage("\u00a7b\u00a7l" + REQUIRED_MONOLITHS + " FROZEN MONOLITHS \u00a77have appeared across the world!");
        Bukkit.broadcastMessage("\u00a77Right-click with Flint and Steel or Fire Charge to activate them.");

        // Spawn guardians at monoliths
        guardianSpawnTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.DEEP_FREEZE) { cancel(); return; }
                tick++;
                if (tick % 200 == 0 && guardiansDefeated < guardiansRequired) {
                    for (Location monolith : frozenMonoliths) {
                        if (Math.random() < 0.4) {
                            Location spawn = monolith.clone().add(ThreadLocalRandom.current().nextInt(20) - 10, 0, ThreadLocalRandom.current().nextInt(20) - 10);
                            int wave = guardiansDefeated < guardiansRequired / 3 ? 0 :
                                       guardiansDefeated < (guardiansRequired * 2) / 3 ? 1 : 2;
                            switch (wave) {
                                case 0 -> new Frostborn(plugin, spawn);
                                case 1 -> new IceWraith(plugin, spawn);
                                case 2 -> new GlacierTitan(plugin, spawn);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(guardianSpawnTask);
    }

    private void buildMonolith(Location center) {
        // Central ice pillar
        for (int y = 0; y < 5; y++) {
            center.clone().add(0, y, 0).getBlock().setType(org.bukkit.Material.PACKED_ICE);
        }
        center.clone().add(0, 5, 0).getBlock().setType(org.bukkit.Material.BLUE_ICE);
        // Base ring
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    center.clone().add(x, 0, z).getBlock().setType(org.bukkit.Material.ICE);
                }
            }
        }
        // Mark as monolith
        center.clone().add(0, 6, 0).getBlock().setType(org.bukkit.Material.RESPAWN_ANCHOR);
    }

    public void activateMonolith(Player player, Location monolithLoc) {
        if (!active || phase != EventPhase.DEEP_FREEZE) return;
        if (!frozenMonoliths.contains(monolithLoc)) return;

        // Check if already activated (has fire on top)
        org.bukkit.block.Block top = monolithLoc.clone().add(0, 6, 0).getBlock();
        if (top.getType() == org.bukkit.Material.RESPAWN_ANCHOR) {
            // Check for fire on anchor
            boolean hasFire = false;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (monolithLoc.clone().add(x, 7, z).getBlock().getType() == org.bukkit.Material.FIRE ||
                        monolithLoc.clone().add(x, 7, z).getBlock().getType() == org.bukkit.Material.SOUL_FIRE) {
                        hasFire = true;
                    }
                }
            }
            if (hasFire) {
                player.sendMessage("\u00a7cThis monolith is already burning!");
                return;
            }
        }

        // Activate it
        top.setType(org.bukkit.Material.FIRE);
        monolithsActivated++;
        player.sendMessage("\u00a7aMonolith activated! (" + monolithsActivated + "/" + REQUIRED_MONOLITHS + ")");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        // Particles
        overworld.spawnParticle(org.bukkit.Particle.FLAME, monolithLoc.add(0, 6, 0), 50, 2, 2, 2, 0.1);
        overworld.spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, monolithLoc.add(0, 5, 0), 30, 1, 2, 1, 0.05);

        if (monolithsActivated >= REQUIRED_MONOLITHS) {
            Bukkit.broadcastMessage("\u00a7e\u00a7lALL MONOLITHS BURN! THE DEEP FREEZE BREAKS!");
            Bukkit.broadcastMessage("\u00a77But the Frozen Titans stir...");
            if (guardianSpawnTask != null) guardianSpawnTask.cancel();
            advancePhase(EventPhase.FROZEN_TITANS);
        }
    }

    private void startFrozenTitans() {
        BukkitTask titanSpawnTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_TITANS) { cancel(); return; }
                tick++;
                if (tick % 300 == 0 && guardiansDefeated < guardiansRequired) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().getEnvironment() == World.Environment.NORMAL && Math.random() < 0.2) {
                            Location spawn = player.getLocation().add(10 + ThreadLocalRandom.current().nextInt(20), 2, 10 + ThreadLocalRandom.current().nextInt(20));
                            spawn.getChunk().load();
                            int wave = guardiansDefeated < guardiansRequired / 3 ? 0 :
                                       guardiansDefeated < (guardiansRequired * 2) / 3 ? 1 : 2;
                            switch (wave) {
                                case 0 -> new Frostborn(plugin, spawn);
                                case 1 -> new IceWraith(plugin, spawn);
                                case 2 -> new GlacierTitan(plugin, spawn);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(titanSpawnTask);
    }

    public void onGuardianDefeated() {
        if (phase != EventPhase.DEEP_FREEZE && phase != EventPhase.FROZEN_TITANS) return;
        guardiansDefeated++;
        Bukkit.broadcastMessage("\u00a7aGuardian defeated! (" + guardiansDefeated + "/" + guardiansRequired + ")");
        if (guardiansDefeated >= guardiansRequired) {
            Bukkit.broadcastMessage("\u00a7eAll guardians have been defeated!");
            if (phase == EventPhase.DEEP_FREEZE) {
                Bukkit.broadcastMessage("\u00a77The Frozen Titans awaken...");
            } else if (phase == EventPhase.FROZEN_TITANS) {
                Bukkit.broadcastMessage("\u00a77The Frost Monarch stirs...");
                advancePhase(EventPhase.FROST_MONARCH);
            }
        }
    }

    private void spawnFrostMonarch() {
        Location spawn = new Location(overworld,
            ThreadLocalRandom.current().nextInt(1000) - 500,
            0,
            ThreadLocalRandom.current().nextInt(1000) - 500
        );
        spawn.setY(overworld.getHighestBlockYAt(spawn) + 20);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.sendTitle("\u00a7c\u00a7lFROST MONARCH", "\u00a77Awakens from the permafrost...", 20, 100, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
            }
        }

        FrostMonarch frostMonarch = new FrostMonarch(plugin, spawn);
        frostMonarchEntity = frostMonarch.getEntity();
        frostMonarchSpawned = true;
        monarchStage = 1;

        // Monarch stage changes
        monarchStageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROST_MONARCH || frostMonarchEntity == null || frostMonarchEntity.isDead()) { cancel(); return; }
                double healthPercent = frostMonarchEntity.getHealth() / frostMonarchEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                int newStage = healthPercent > 0.66 ? 1 : healthPercent > 0.33 ? 2 : 3;
                if (newStage != monarchStage) {
                    monarchStage = newStage;
                    onMonarchStageChange(newStage);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.add(monarchStageTask);
    }

    private void onMonarchStageChange(int stage) {
        if (frostMonarchEntity == null || frostMonarchEntity.isDead()) return;
        Bukkit.broadcastMessage("\u00a7c\u00a7lFrost Monarch enters Stage " + stage + "!");
        frostMonarchEntity.getWorld().playSound(frostMonarchEntity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

        if (stage == 2) {
            frostMonarchEntity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 3, true, false));
            for (int i = 0; i < 4; i++) {
                Location spawn = frostMonarchEntity.getLocation().add(ThreadLocalRandom.current().nextInt(20) - 10, 0, ThreadLocalRandom.current().nextInt(20) - 10);
                new GlacierTitan(plugin, spawn);
            }
        } else if (stage == 3) {
            frostMonarchEntity.removePotionEffect(PotionEffectType.RESISTANCE);
            Bukkit.broadcastMessage("\u00a74\u00a7lTHE FROST MONARCH ENRAGES!");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(frostMonarchEntity.getWorld())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2, true, false));
                }
            }
        }
    }

    public void awardWinner(Player killer) {
        if (killer == null || !killer.isOnline()) return;
        if (phase == EventPhase.FROST_MONARCH) {
            winner = killer.getUniqueId();
            advancePhase(EventPhase.FROZEN_CRUCIBLE);
        }
    }

    private void startFrozenCrucible() {
        if (frostMonarchEntity != null && !frostMonarchEntity.isDead()) {
            frostMonarchEntity.remove();
        }

        // Teleport all participants to the monarch's death location
        Location crucibleCenter = frostMonarchEntity != null ? frostMonarchEntity.getLocation() : overworld.getSpawnLocation();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4, true, false));
                player.teleport(crucibleCenter.clone().add(ThreadLocalRandom.current().nextInt(10) - 5, 0, ThreadLocalRandom.current().nextInt(10) - 5));
            }
        }

        Bukkit.getServer().sendMessage(
            Component.text("THE MONARCH IS DEAD. ONLY ONE MAY CLAIM THE WINTER.")
                .color(NamedTextColor.RED)
        );

        // Ice Core in the center
        Location iceCoreLoc = crucibleCenter.clone().add(0, 2, 0);
        org.bukkit.entity.Item iceCore = overworld.dropItem(iceCoreLoc, new org.bukkit.inventory.ItemStack(org.bukkit.Material.ICE));
        iceCore.setCustomName("\u00a7b\u00a7lIce Core");
        iceCore.setCustomNameVisible(true);
        iceCore.setGlowing(true);
        iceCore.setPickupDelay(Integer.MAX_VALUE);

        // Shrinking border
        crucibleBorderTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_CRUCIBLE) { cancel(); return; }
                tick++;
                if (tick % 600 == 0) { // Every 30 seconds
                    Bukkit.broadcastMessage("\u00a7c\u00a7lThe arena is shrinking! The void consumes the edges!");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (participants.contains(player.getUniqueId()) && player.getWorld().equals(overworld)) {
                            double dist = player.getLocation().distance(crucibleCenter);
                            double maxDist = 50.0 - (tick / 600) * 8.0;
                            if (dist > maxDist) {
                                player.damage(6.0);
                                player.sendMessage("\u00a7c\u00a7lThe void freezes your bones!");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(crucibleBorderTask);

        // Check winner
        BukkitTask checkWinnerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_CRUCIBLE) { cancel(); return; }
                List<UUID> alive = new ArrayList<>();
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getWorld().equals(overworld)) {
                        if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            alive.add(uuid);
                        }
                    }
                }
                if (alive.size() == 1) {
                    winner = alive.get(0);
                    advancePhase(EventPhase.WINTER_COMPLETED);
                } else if (alive.isEmpty()) {
                    Bukkit.broadcastMessage("\u00a7c\u00a7lNo one survived the Frozen Eclipse...");
                    stopEvent();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.add(checkWinnerTask);
    }

    private void grantWinterAspect() {
        if (winner == null) return;
        Player player = Bukkit.getPlayer(winner);
        if (player != null && player.isOnline()) {
            Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
            if (heart != null) {
                heart.setAspect(AspectType.WINTER);
                heart.setWinterUnlocked(true);
                heart.getCooldowns().clear();
                plugin.getHeartManager().saveHeart(heart);
                
                // Claim the aspect for this player
                plugin.claimAspect(AspectType.WINTER, player.getUniqueId());
            }
            
            // Create Frostheart item
            ItemStack frostheart = createFrostheart();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(frostheart);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover.values().iterator().next());
            }
            
            player.teleport(overworld.getSpawnLocation());
            player.getWorld().strikeLightningEffect(player.getLocation());
            
            player.sendMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("\u00a7b\u00a7l   YOU HAVE SURVIVED THE FROZEN ECLIPSE!");
            player.sendMessage("\u00a7b\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("\u00a7a\u00a7lYou are the Master of Winter!");
            player.sendMessage("\u00a77You have unlocked the \u00a7bWinter Aspect \u00a77and received a \u00a7bFrostheart\u00a77!");

            Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("\u00a7a\u00a7l" + player.getName() + " \u00a7ahas conquered the Frozen Eclipse!");
            Bukkit.broadcastMessage("\u00a77They are now the \u00a7bSOLE HOLDER \u00a77of the Winter Aspect and carry the \u00a7bFrostheart\u00a77.");
            Bukkit.broadcastMessage("\u00a7a\u00a7l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
// Play visual effects for winner
        int tier = heart != null ? heart.getTier() : 2;
        plugin.getVisualEffectsManager().playHeartUpgradeVisuals(player, tier);
            plugin.getVisualEffectsManager().playGlobalEventAnnouncement(
                "MASTER OF WINTER",
                player.getName() + " has conquered the Frozen Eclipse!",
                AspectType.WINTER
            );
        }
    }
    
    private ItemStack createFrostheart() {
        ItemStack item = new ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lFrostheart");
        meta.setLore(java.util.Arrays.asList(
            "\u00a77A crystallized fragment of eternal winter.",
            "\u00a77Grants the \u00a7bWinter Aspect \u00a77permanently.",
            "\u00a7b\u00a7lRight-click to activate Absolute Zero."
        ));
        meta.setCustomModelData(1002);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public void stopEvent() {
        active = false;
        for (BukkitTask task : activeTasks) {
            if (task != null) task.cancel();
        }
        activeTasks.clear();
        if (guardianSpawnTask != null) guardianSpawnTask.cancel();
        participants.clear();
        guardiansDefeated = 0;
        frostMonarchEntity = null;
        Bukkit.broadcastMessage("\u00a7cThe Frozen Eclipse event has ended.");
    }

    private BukkitTask guardianSpawnTask;

    @Override
    public void saveState() {
        // Save event state to storage
    }

    @Override
    public void loadState() {
        // Load event state from storage
    }
}
