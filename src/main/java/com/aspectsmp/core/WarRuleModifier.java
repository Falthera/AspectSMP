package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastKnockback = new ConcurrentHashMap<>();

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return damage * 1.2;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.setWalkSpeed(0.25f);
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        long now = System.currentTimeMillis();
        Long last = lastKnockback.get(player.getUniqueId());
        if (last != null && now - last < 1000) {
            return 0;
        }
        lastKnockback.put(player.getUniqueId(), now);
        return knockback * 0.2;
    }
}
