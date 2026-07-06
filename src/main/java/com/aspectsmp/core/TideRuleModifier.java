package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TideRuleModifier implements RuleModifier {

    private final Map<java.util.UUID, Long> lastHeal = new ConcurrentHashMap<>();
    private static final long HEAL_COOLDOWN_MS = 1000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return damage * 1.1;
        }
        if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            return damage * 0.7;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        if (player.getLocation().getBlock().getType().toString().contains("WATER")) {
            player.setWalkSpeed(0.25f);
            long now = System.currentTimeMillis();
            Long last = lastHeal.get(player.getUniqueId());
            if (last == null || now - last > HEAL_COOLDOWN_MS) {
                player.setHealth(Math.min(player.getHealth() + 1, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
                lastHeal.put(player.getUniqueId(), now);
            }
        }
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.9;
    }
}