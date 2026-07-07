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
    private static final long COMBAT_DURATION = 30000L;

    public CombatManager(AspectSMP plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
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
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (trustManager.isTrusted(victim.getUniqueId(), attacker.getUniqueId())) return;

        tagPlayer(victim.getUniqueId());
        tagPlayer(attacker.getUniqueId());
    }

    public void handleLogout(Player player) {
        if (isInCombat(player.getUniqueId())) {
            player.setHealth(0.0);
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
