package com.aspectsmp.core;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RiftRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastVoidProtect = new ConcurrentHashMap<>();
    private static final long VOID_PROTECTION_DURATION = 5000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            long now = System.currentTimeMillis();
            Long lastActivation = lastVoidProtect.get(player.getUniqueId());
            if (lastActivation != null && now - lastActivation < VOID_PROTECTION_DURATION) {
                return 0;
            }
            if (damage >= player.getHealth()) {
                player.setHealth(Math.max(1, player.getHealth() - damage + 1));
                lastVoidProtect.put(player.getUniqueId(), now);
                return 0;
            }
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        if (player.getLocation().getY() < 0) {
            lastVoidProtect.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.5;
    }

    @Override
    public void applyPassive(Player player) {
    }
}
