package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.mobs.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.BossBar;
import org.bukkit.scoreboard.BarColor;
import org.bukkit.scoreboard.BarStyle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FrozenEclipseEvent {

    private final AspectSMP plugin;
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
    private boolean frostMonarchSpawned = false;
    private int monarchStage = 1;
    private BossBar approachBossBar;
    private BossBar guardianBossBar;
    private BukkitTask guardianSpawnTask;
    private BukkitTask trialsDamageTask;
    private BukkitTask brazierCheckTask;
    private BukkitTask frostbornStompTask;
    private BukkitTask monarchStageTask;
    private BukkitTask crucibleBorderTask;
    private BukkitTask frostTrailTask;

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

    public LivingEntity getFrostMonarchEntity() {
        return frostMonarchEntity;
    }

    public void addParticipant(Player player) {
        if (!active || phase != EventPhase.FIRST_FROST) return;
        if (!participants.contains(player.getUniqueId())) {
            participants.add(player.getUniqueId());
            player.sendMessage("§b§lYou have joined the Frozen Eclipse ritual!");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        }
    }

    public void startEvent() {
        if (active) return;
        active = true;
        phase = EventPhase.FIRST_FROST;
        guardiansDefeated = 0;
        participants.clear();
        winner = null;
        frostMonarchEntity = null;
        braziersLit = 0;
        brazierAllLitTimer = 0;
        portalOpened = false;
        frostStage = 0;
        trialsElapsedTicks = 0;
        coldDamageInterval = 40;
        frostMonarchSpawned = false;
        monarchStage = 1;

        Bukkit.broadcastMessage("§b§lThe Frozen Eclipse has begun...");
        Bukkit.broadcastMessage("§7An unnatural winter descends upon the world.");
        Bukkit.broadcastMessage("§7Find the path to the Frozen Kingdom.");

        startFrostEffects();
        schedulePhaseTransition(20L, () -> sendJoinBroadcast());
        schedulePhaseTransition(200L, () -> {}); 
        schedulePhaseTransition(18000L, EventPhase.FROZEN_KINGDOM);
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
        approachBossBar = Bukkit.createBossBar("§b§lThe Frozen Eclipse Approaches...", BarColor.BLUE, BarStyle.SOLID);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                approachBossBar.addPlayer(player);
            }
        }
        approachBossBar.setProgress(1.0);

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.FIRST_FROST) {
                    cancel();
                    return;
                }
                tick++;
                int elapsedSeconds = tick / 20;

                if (elapsedSeconds >= 540) frostStage = 3;
                else if (elapsedSeconds >= 360) frostStage = 2;
                else if (elapsedSeconds >= 180) frostStage = 1;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

                    if (frostStage >= 0 && tick % 20 == 0) {
                        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE,
                            player.getLocation().add(0, 2, 0), 5, 2, 1, 2, 0.01);
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
                                player.damage(1.0);
                                lastFrostDamage.put(player.getUniqueId(), System.currentTimeMillis());
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(task);

        BukkitTask trailTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD,
                            player.getLocation().add(0, 0.1, 0), 3, 0.5, 0.1, 0.5, 0.02);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
        activeTasks.add(trailTask);
    }

    private boolean isNearHeatSource(Player player) {
        Location loc = player.getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    org.bukkit.block.Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getType() == org.bukkit.Material.TORCH ||
                        block.getType() == org.bukkit.Material.CAMPFIRE ||
                        block.getType() == org.bukkit.Material.LANTERN ||
                        block.getType() == org.bukkit.Material.FIRE ||
                        block.getType() == org.bukkit.Material.SOUL_CAMPFIRE ||
                        block.getType() == org.bukkit.Material.LAVA) {
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
                if (!active) {
                    cancel();
                    return;
                }
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
                if (!active) {
                    cancel();
                    return;
                }
                task.run();
            }
        }.runTaskLater(plugin, delay);
        activeTasks.add(t);
    }

    public void advancePhase(EventPhase next) {
        this.phase = next;
        switch (next) {
            case FROZEN_KINGDOM -> {
                if (approachBossBar != null) {
                    approachBossBar.removeAll();
                    approachBossBar = null;
                }
                Bukkit.broadcastMessage("§e§lThe Frozen Kingdom has appeared!");
                Bukkit.broadcastMessage("§7Enter the ancient frozen civilization.");
                guardiansRequired = 5 + (participants.size() * 2);
                createGuardianBossBar();
                startFrozenKingdom();
            }
            case TRIALS -> {
                if (guardianBossBar != null) {
                    guardianBossBar.removeAll();
                    guardianBossBar = null;
                }
                Bukkit.broadcastMessage("§d§lThe Trials of Winter begin!");
                Bukkit.broadcastMessage("§7Survive the absolute cold.");
                startTrials();
            }
            case FROST_MONARCH -> {
                Bukkit.broadcastMessage("§c§lThe Frost Monarch awakens!");
                Bukkit.broadcastMessage("§7Defeat it to claim the Aspect of Winter.");
                spawnFrostMonarch();
            }
            case FROZEN_CRUCIBLE -> {
                Bukkit.broadcastMessage("§c§lTHE MONARCH IS DEAD. ONLY ONE MAY CLAIM THE WINTER.");
                startFrozenCrucible();
            }
            case COMPLETED -> {
                Bukkit.broadcastMessage("§a§lThe Frozen Eclipse has been conquered!");
                grantWinterAspect();
                stopEvent();
            }
            default -> {}
        }
    }

    private void createGuardianBossBar() {
        guardianBossBar = Bukkit.createBossBar("§b§lGuardians Remaining: " + guardiansDefeated + " / " + guardiansRequired, BarColor.BLUE, BarStyle.SEGMENTED_10);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                guardianBossBar.addPlayer(player);
            }
        }
    }

    private void updateGuardianBossBar() {
        if (guardianBossBar != null) {
            guardianBossBar.setTitle("§b§lGuardians Remaining: " + guardiansDefeated + " / " + guardiansRequired);
            guardianBossBar.setProgress(Math.max(0.0, (guardiansRequired - guardiansDefeated) / (double) guardiansRequired));
        }
    }

    private void startFrozenKingdom() {
        guardianSpawnTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_KINGDOM) {
                    cancel();
                    return;
                }
                tick++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() == World.Environment.NORMAL && Math.random() < 0.15) {
                        Location spawn = player.getLocation().add(5 + new Random().nextInt(10), 2, 5 + new Random().nextInt(10));
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
        }.runTaskTimer(plugin, 0L, 100L);
        activeTasks.add(guardianSpawnTask);
    }

    public void onGuardianDefeated() {
        if (phase != EventPhase.FROZEN_KINGDOM) return;
        guardiansDefeated++;
        updateGuardianBossBar();
        Bukkit.broadcastMessage("§aGuardian defeated! (" + guardiansDefeated + "/" + guardiansRequired + ")");
        if (guardiansDefeated >= guardiansRequired) {
            Bukkit.broadcastMessage("§eAll guardians have been defeated!");
            if (guardianSpawnTask != null) {
                guardianSpawnTask.cancel();
            }
            schedulePhaseTransition(100L, EventPhase.TRIALS);
        }
    }

    private void startTrials() {
        World trialsWorld = Bukkit.getWorld("frozen_trials");
        if (trialsWorld == null) {
            trialsWorld = Bukkit.getWorld("winter_kingdom");
        }
        if (trialsWorld == null) {
            Bukkit.getLogger().warning("No trials world found!");
            advancePhase(EventPhase.FROST_MONARCH);
            return;
        }
        this.trialsWorld = trialsWorld;

        Location trialsSpawn = trialsWorld.getSpawnLocation().add(0, 10, 0);
        brazierLocations.clear();
        int spread = 15;
        brazierLocations.add(trialsSpawn.clone().add(spread, 0, 0));
        brazierLocations.add(trialsSpawn.clone().add(-spread, 0, 0));
        brazierLocations.add(trialsSpawn.clone().add(0, 0, spread));
        brazierLocations.add(trialsSpawn.clone().add(0, 0, -spread));
        brazierLocations.add(trialsSpawn.clone().add(spread * 0.7, 0, spread * 0.7));
        braziersLit = 0;
        brazierAllLitTimer = 0;
        portalOpened = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId())) {
                player.teleport(trialsSpawn);
                player.sendMessage("§d§lYou have been transported to the Trials Realm!");
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!participants.contains(player.getUniqueId()) && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                player.getWorld().setStorm(false);
                player.getWorld().setThundering(false);
            }
        }

        trialsDamageTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                tick++;
                trialsElapsedTicks = tick;
                
                int damage = 1;
                if (tick > 6000) damage = 3;
                else if (tick > 3000) damage = 2;

                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;
                    if (!isInWarmZone(player)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1, true, false));
                        if (tick % coldDamageInterval == 0) {
                            player.damage(damage);
                        }
                    } else {
                        if (player.getHealth() < 20.0) {
                            player.setHealth(Math.min(20.0, player.getHealth() + 0.5));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(trialsDamageTask);

        brazierCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                checkBraziers();
            }
        }.runTaskTimer(plugin, 0L, 10L);
        activeTasks.add(brazierCheckTask);

        frostbornStompTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                tick++;
                if (tick % 40 == 0) {
                    for (Location brazier : brazierLocations) {
                        if (Math.random() < 0.3) {
                            Location spawn = brazier.clone().add(new Random().nextInt(10) - 5, 0, new Random().nextInt(10) - 5);
                            if (spawn.getWorld() != null && spawn.getChunk().isLoaded()) {
                                new Frostborn(plugin, spawn);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(frostbornStompTask);

        schedulePhaseTransition(12000L, () -> {
            if (!portalOpened) {
                Bukkit.broadcastMessage("§c§lThe Trials have failed! The braziers were not lit in time.");
                advancePhase(EventPhase.FROST_MONARCH);
            }
        });
    }

    private boolean isInWarmZone(Player player) {
        Location loc = player.getLocation();
        for (Location brazier : brazierLocations) {
            if (loc.distanceSquared(brazier) <= 25.0) {
                return true;
            }
        }
        return false;
    }

    private void checkBraziers() {
        int litCount = 0;
        for (Location brazier : brazierLocations) {
            boolean lit = false;
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        org.bukkit.block.Block block = brazier.clone().add(x, y, z).getBlock();
                        if (block.getType() == org.bukkit.Material.FIRE ||
                            block.getType() == org.bukkit.Material.SOUL_FIRE) {
                            lit = true;
                            break;
                        }
                    }
                }
            }
            if (lit) litCount++;
        }
        braziersLit = litCount;
        if (braziersLit >= 5) {
            brazierAllLitTimer++;
            if (brazierAllLitTimer >= 1200 && !portalOpened) {
                portalOpened = true;
                Bukkit.broadcastMessage("§e§lAll braziers burn bright! The portal opens!");
                openTrialsPortal();
            }
        } else {
            brazierAllLitTimer = Math.max(0, brazierAllLitTimer - 1);
        }
    }

    private void openTrialsPortal() {
        if (trialsWorld == null) return;
        Location portalLoc = trialsWorld.getSpawnLocation().add(0, 1, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    org.bukkit.block.Block block = portalLoc.clone().add(x, y, z).getBlock();
                    if (y == 0) {
                        block.setType(org.bukkit.Material.NETHER_PORTAL);
                    } else {
                        block.setType(org.bukkit.Material.AIR);
                    }
                }
            }
        }

        BukkitTask portalTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.TRIALS) {
                    cancel();
                    return;
                }
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getWorld().equals(trialsWorld)) {
                        if (player.getLocation().distanceSquared(portalLoc) <= 16.0) {
                            teleportToWinterKingdom(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
        activeTasks.add(portalTask);

        schedulePhaseTransition(1200L, () -> {
            if (!portalOpened) return;
            Bukkit.broadcastMessage("§e§lThe portal draws you in...");
            for (UUID uuid : participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    teleportToWinterKingdom(player);
                }
            }
            advancePhase(EventPhase.FROST_MONARCH);
        });
    }

    private void teleportToWinterKingdom(Player player) {
        World winterWorld = Bukkit.getWorld("winter_kingdom");
        if (winterWorld != null) {
            player.teleport(winterWorld.getSpawnLocation().add(0, 2, 0));
        }
    }

    private void spawnFrostMonarch() {
        World winterWorld = Bukkit.getWorld("winter_kingdom");
        if (winterWorld == null) {
            Bukkit.getLogger().warning("Winter Kingdom world not found!");
            advancePhase(EventPhase.FROST_MONARCH);
            return;
        }
        Location spawn = winterWorld.getSpawnLocation().add(0, 15, 0);
        FrostMonarch frostMonarch = new FrostMonarch(plugin, spawn);
        frostMonarchEntity = frostMonarch.getEntity();
        frostMonarchSpawned = true;
        monarchStage = 1;
        Bukkit.broadcastMessage("§c§lFROST MONARCH SPAWNED!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId())) {
                player.teleport(spawn.clone().add(10, 0, 10));
            }
        }

        monarchStageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROST_MONARCH || frostMonarchEntity == null || frostMonarchEntity.isDead()) {
                    cancel();
                    return;
                }
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
        Bukkit.broadcastMessage("§c§lFrost Monarch enters Stage " + stage + "!");
        frostMonarchEntity.getWorld().playSound(frostMonarchEntity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

        if (stage == 2) {
            frostMonarchEntity.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 4, true, false));
            for (int i = 0; i < 4; i++) {
                Location spawn = frostMonarchEntity.getLocation().add(new Random().nextInt(20) - 10, 0, new Random().nextInt(20) - 10);
                new GlacierTitan(plugin, spawn);
            }
        } else if (stage == 3) {
            frostMonarchEntity.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            Bukkit.broadcastMessage("§4§lTHE FROST MONARCH ENRAGES!");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(frostMonarchEntity.getWorld())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, true, false));
                }
            }
        }
    }

    private void startFrozenCrucible() {
        if (frostMonarchEntity != null && !frostMonarchEntity.isDead()) {
            frostMonarchEntity.remove();
        }

        World winterWorld = Bukkit.getWorld("winter_kingdom");
        if (winterWorld == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId())) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4, true, false));
                player.teleport(winterWorld.getSpawnLocation());
            }
        }

        Bukkit.getServer().sendMessage(
            Component.text("THE MONARCH IS DEAD. ONLY ONE MAY CLAIM THE WINTER.")
                .color(NamedTextColor.RED)
                .build()
        );

        Location iceCoreLoc = winterWorld.getSpawnLocation().add(0, 2, 0);
        org.bukkit.entity.Item iceCore = winterWorld.dropItem(iceCoreLoc, new org.bukkit.inventory.ItemStack(org.bukkit.Material.ICE));
        iceCore.setCustomName("§b§lIce Core");
        iceCore.setCustomNameVisible(true);
        iceCore.setGlowing(true);
        iceCore.setPickupDelay(Integer.MAX_VALUE);

        crucibleBorderTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_CRUCIBLE) {
                    cancel();
                    return;
                }
                tick++;
                if (tick % 600 == 0) {
                    Bukkit.broadcastMessage("§c§lThe arena is shrinking! The cold void consumes the edges!");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (participants.contains(player.getUniqueId()) && player.getWorld().equals(winterWorld)) {
                            Location center = winterWorld.getSpawnLocation();
                            double dist = player.getLocation().distance(center);
                            double maxDist = 30.0 - (tick / 600) * 5.0;
                            if (dist > maxDist) {
                                player.damage(4.0);
                                player.sendMessage("§c§lThe void freezes your bones!");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        activeTasks.add(crucibleBorderTask);

        BukkitTask checkWinnerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.FROZEN_CRUCIBLE) {
                    cancel();
                    return;
                }
                List<UUID> alive = new ArrayList<>();
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getWorld().equals(winterWorld)) {
                        if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            alive.add(uuid);
                        }
                    }
                }
                if (alive.size() == 1) {
                    winner = alive.get(0);
                    advancePhase(EventPhase.COMPLETED);
                } else if (alive.isEmpty()) {
                    Bukkit.broadcastMessage("§c§lNo one survived the Frozen Eclipse...");
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
                player.sendMessage("§b§lYou have unlocked the Aspect of Winter!");
            }
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.getWorld().strikeLightning(player.getLocation());
            Bukkit.broadcastMessage("§e§l" + player.getName() + " has conquered the Frozen Eclipse!");
        }
    }

    public void stopEvent() {
        active = false;
        for (BukkitTask task : activeTasks) {
            if (task != null) task.cancel();
        }
        activeTasks.clear();
        if (guardianSpawnTask != null) {
            guardianSpawnTask.cancel();
            guardianSpawnTask = null;
        }
        if (trialsDamageTask != null) {
            trialsDamageTask.cancel();
            trialsDamageTask = null;
        }
        if (brazierCheckTask != null) {
            brazierCheckTask.cancel();
            brazierCheckTask = null;
        }
        if (frostbornStompTask != null) {
            frostbornStompTask.cancel();
            frostbornStompTask = null;
        }
        if (monarchStageTask != null) {
            monarchStageTask.cancel();
            monarchStageTask = null;
        }
        if (crucibleBorderTask != null) {
            crucibleBorderTask.cancel();
            crucibleBorderTask = null;
        }
        if (approachBossBar != null) {
            approachBossBar.removeAll();
            approachBossBar = null;
        }
        if (guardianBossBar != null) {
            guardianBossBar.removeAll();
            guardianBossBar = null;
        }
        participants.clear();
        guardiansDefeated = 0;
        frostMonarchEntity = null;
        frostMonarchSpawned = false;
        Bukkit.broadcastMessage("§cThe Frozen Eclipse event has ended.");
    }
}
