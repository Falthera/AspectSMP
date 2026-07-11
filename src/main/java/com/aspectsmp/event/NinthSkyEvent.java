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
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class NinthSkyEvent {

    private final AspectSMP plugin;
    private EventPhase phase = EventPhase.SKY_AWAKENS;
    private boolean active = false;
    private final Set<UUID> participants = new HashSet<>();
    private int fragmentsCollected = 0;
    private static final int REQUIRED_FRAGMENTS = 5;
    private static final int MAX_MOB_SPAWNS = 20;
    private int mobsSpawned = 0;
    private LivingEntity skyGuardianEntity;
    private BukkitTask skyEffectsTask;
    private BukkitTask mobSpamTask;
    private final List<BukkitTask> scheduledTransitions = new ArrayList<>();

    public NinthSkyEvent(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return active;
    }

    public EventPhase getPhase() {
        return phase;
    }

    public LivingEntity getSkyGuardianEntity() {
        return skyGuardianEntity;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public void startEvent() {
        if (active) return;
        active = true;
        phase = EventPhase.SKY_AWAKENS;
        fragmentsCollected = 0;
        mobsSpawned = 0;
        participants.clear();
        skyGuardianEntity = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                participants.add(player.getUniqueId());
            }
        }

        Bukkit.broadcastMessage("§b§lThe Ninth Sky is awakening...");
        Bukkit.broadcastMessage("§7The clouds have begun to move. Something ancient stirs above.");
        Bukkit.broadcastMessage("§7You have been marked as a participant. Good luck.");

        schedulePhaseTransition(200L, EventPhase.SKY_FRAGMENTS);
    }

    public void stopEvent() {
        active = false;
        if (skyEffectsTask != null) {
            skyEffectsTask.cancel();
            skyEffectsTask = null;
        }
        if (mobSpamTask != null) {
            mobSpamTask.cancel();
            mobSpamTask = null;
        }
        for (BukkitTask task : scheduledTransitions) {
            task.cancel();
        }
        scheduledTransitions.clear();
        participants.clear();
        fragmentsCollected = 0;
        mobsSpawned = 0;
        skyGuardianEntity = null;
        Bukkit.broadcastMessage("§cThe Ninth Sky event has been stopped.");
    }

    private void schedulePhaseTransition(long delay, EventPhase nextPhase) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                scheduledTransitions.remove(this);
                if (!active) {
                    cancel();
                    return;
                }
                advancePhase(nextPhase);
            }
        }.runTaskLater(plugin, delay);
        scheduledTransitions.add(task);
    }

    public void advancePhase(EventPhase next) {
        for (BukkitTask task : scheduledTransitions) {
            task.cancel();
        }
        scheduledTransitions.clear();

        this.phase = next;
        switch (next) {
            case SKY_FRAGMENTS -> {
                Bukkit.broadcastMessage("§e§lSky Fragments have fallen across the world!");
                Bukkit.broadcastMessage("§7Find and recover them before they are claimed.");
                startFragmentPhase();
            }
            case SKY_GATE -> {
                Bukkit.broadcastMessage("§d§lThe Sky Gate opens!");
                Bukkit.broadcastMessage("§7A portal to the Ninth Sky has formed.");
                openSkyGate();
                schedulePhaseTransition(300L, EventPhase.NINTH_SKY_REALM);
            }
            case NINTH_SKY_REALM -> {
                Bukkit.broadcastMessage("§b§lEntering the Ninth Sky Realm...");
                Bukkit.broadcastMessage("§7Prove yourselves worthy.");
                teleportParticipantsToSkyRealm();
                schedulePhaseTransition(100L, EventPhase.TRIALS);
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
                Bukkit.broadcastMessage("§7The Sky Guardian has been defeated.");
                active = false;
            }
            default -> {}
        }
    }

    private void startFragmentPhase() {
        mobSpamTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || phase != EventPhase.SKY_FRAGMENTS) {
                    cancel();
                    mobSpamTask = null;
                    return;
                }
                if (mobsSpawned >= MAX_MOB_SPAWNS) return;
                spawnFragmentGuards();
            }
        }.runTaskTimer(plugin, 0L, 100L);
        schedulePhaseTransition(600L, EventPhase.SKY_GATE);
    }

    private void spawnFragmentGuards() {
        List<? extends Player> overworldPlayers = Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().getEnvironment() == World.Environment.NORMAL)
            .filter(p -> participants.contains(p.getUniqueId()))
            .toList();
        if (overworldPlayers.isEmpty()) return;

        int spawnsThisTick = Math.min(3, MAX_MOB_SPAWNS - mobsSpawned);
        for (int i = 0; i < spawnsThisTick; i++) {
            Player randomPlayer = overworldPlayers.get(new Random().nextInt(overworldPlayers.size()));
            Location base = randomPlayer.getLocation();
            Location spawn = new Location(
                base.getWorld(),
                base.getX() + 10 + new Random().nextInt(20) - 10,
                0,
                base.getZ() + 10 + new Random().nextInt(20) - 10
            );
            int safeY = spawn.getWorld().getHighestBlockYAt(spawn);
            spawn.setY(safeY + 2);
            if (spawn.getBlock().getType().isSolid()) {
                spawn.setY(safeY + 3);
            }
            if (mobsSpawned >= MAX_MOB_SPAWNS) break;
            mobsSpawned++;
            switch (new Random().nextInt(3)) {
                case 0 -> new CloudWraith(plugin, spawn);
                case 1 -> new StormSentinel(plugin, spawn);
                case 2 -> new TempestPhantom(plugin, spawn);
            }
        }
    }

    private void openSkyGate() {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                Location gateLoc = player.getWorld().getHighestBlockAt(player.getLocation()).getLocation().add(0, 30, 0);
                player.sendTitle("§b§lSky Gate Opened", "§7Look to the heavens", 20, 100, 20);
            }
        }
    }

    private void teleportParticipantsToSkyRealm() {
        plugin.getSkyRealmManager().ensureNinthSkyLoaded();
        World skyWorld = Bukkit.getWorld("ninth_sky");
        if (skyWorld == null) {
            plugin.getLogger().warning("Ninth Sky world 'ninth_sky' not found! Teleportation cancelled.");
            Bukkit.broadcastMessage("§cThe Ninth Sky realm is not ready. Please contact an admin.");
            return;
        }
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Location spawn = skyWorld.getSpawnLocation().add(0, 20, 0);
                int safeY = skyWorld.getHighestBlockYAt(spawn);
                spawn.setY(safeY + 2);
                if (spawn.getBlock().getType().isSolid()) {
                    spawn.setY(safeY + 3);
                }
                player.teleport(spawn);
                player.sendMessage("§aYou have been transported to the Ninth Sky Realm.");
            }
        }
    }

    private void startTrials() {
        schedulePhaseTransition(400L, EventPhase.SKY_GUARDIAN);
    }

    private void spawnSkyGuardian() {
        plugin.getSkyRealmManager().ensureNinthSkyLoaded();
        World skyWorld = Bukkit.getWorld("ninth_sky");
        if (skyWorld == null) {
            plugin.getLogger().warning("Ninth Sky world 'ninth_sky' not found! Cannot spawn Sky Guardian.");
            return;
        }
        Location spawn = skyWorld.getSpawnLocation().add(0, 15, 0);
        int safeY = skyWorld.getHighestBlockYAt(spawn);
        spawn.setY(safeY + 2);
        SkyGuardian guardian = new SkyGuardian(plugin, spawn);
        skyGuardianEntity = guardian.getEntity();
        Bukkit.broadcastMessage("§c§lSKY GUARDIAN SPAWNED!");
        Bukkit.broadcastMessage("§7Defeat it to claim the Aspect of the Cloud!");
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
        winner.sendMessage("§b§lThe Sky Guardian has been defeated!");
        winner.sendMessage("§a§lYou are the champion of the Ninth Sky!");
        winner.sendMessage("§7You have unlocked the Aspect of the Cloud and received a Cloudheart!");

        ItemStack cloudheart = new Cloudheart().createItemStack();
        java.util.HashMap<Integer, ItemStack> leftover = winner.getInventory().addItem(cloudheart);
        if (!leftover.isEmpty()) {
            winner.getWorld().dropItemNaturally(winner.getLocation(), leftover.get(0));
        }

        Bukkit.broadcastMessage("§a§l" + winner.getName() + " §ahas conquered the Ninth Sky!");
        Bukkit.broadcastMessage("§7They are now bound to the §bCloud Aspect §7and carry the §bCloudheart§7.");
    }

    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    public boolean isCompleted() {
        return !active && phase == EventPhase.COMPLETED;
    }

    public void collectFragment(Player player) {
        if (!active || phase != EventPhase.SKY_FRAGMENTS) return;
        if (!participants.contains(player.getUniqueId())) return;
        fragmentsCollected++;
        player.sendMessage("§aSky Fragment collected! (" + fragmentsCollected + "/" + REQUIRED_FRAGMENTS + ")");
        if (fragmentsCollected >= REQUIRED_FRAGMENTS) {
            Bukkit.broadcastMessage("§eAll Sky Fragments have been recovered!");
            if (mobSpamTask != null) {
                mobSpamTask.cancel();
                mobSpamTask = null;
            }
            advancePhase(EventPhase.SKY_GATE);
        }
    }
}
