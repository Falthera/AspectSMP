package com.aspectsmp.combat;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.trust.TrustManager;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CombatManager {

    private final AspectSMP plugin;
    private final TrustManager trustManager;
    private final Map<UUID, Long> combatTags = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private static final long COMBAT_DURATION = 15000L;

    public CombatManager(AspectSMP plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> iterator = combatTags.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    UUID playerId = entry.getKey();
                    long expiry = entry.getValue();
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        hideBossBar(player);
                        iterator.remove();
                        continue;
                    }
                    if (now > expiry) {
                        hideBossBar(player);
                        iterator.remove();
                        continue;
                    }
                    BossBar bar = bossBars.get(playerId);
                    if (bar == null) continue;
                    long remaining = expiry - now;
                    double progress = Math.max(0.0, Math.min(1.0, (double) remaining / COMBAT_DURATION));
                    double healthPercent = Math.max(0.0, Math.min(1.0, player.getHealth() / player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
                    bar.setProgress(healthPercent);
                    bar.setTitle(String.format("§c§lIN COMBAT §7| §c%.1fs §7| §c❤ %.0f/%.0f", remaining / 1000.0, player.getHealth(), player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void tagPlayer(UUID playerId) {
        combatTags.put(playerId, System.currentTimeMillis() + COMBAT_DURATION);
        showBossBar(plugin.getServer().getPlayer(playerId));
    }

    public boolean isInCombat(UUID playerId) {
        Long expiry = combatTags.get(playerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatTags.remove(playerId);
            hideBossBar(plugin.getServer().getPlayer(playerId));
            return false;
        }
        return true;
    }

    public void handleDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent damageByEntity && damageByEntity.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (trustManager.isTrusted(victim.getUniqueId(), attacker.getUniqueId())) return;

        tagPlayer(victim.getUniqueId());
        tagPlayer(attacker.getUniqueId());
    }

    public void handleLogout(Player player) {
        if (isInCombat(player.getUniqueId())) {
            plugin.getServer().broadcastMessage("§c" + player.getName() + " §7has been suspected of combat logging.");
        }
        hideBossBar(player);
        combatTags.remove(player.getUniqueId());
    }

    private void showBossBar(Player player) {
        if (player == null || !player.isOnline()) return;
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), id -> plugin.getServer().createBossBar("§c§lIN COMBAT", BarColor.RED, BarStyle.SOLID));
        bar.addPlayer(player);
        bar.setVisible(true);
    }

    private void hideBossBar(Player player) {
        if (player == null) return;
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.setVisible(false);
        }
    }

    public void cleanup() {
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            entry.getValue().removeAll();
        }
        bossBars.clear();
        combatTags.clear();
    }
}
