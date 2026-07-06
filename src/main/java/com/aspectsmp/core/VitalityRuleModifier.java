package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityRuleModifier implements RuleModifier {

    private final Map<java.util.UUID, Long> lastHeal = new ConcurrentHashMap<>();
    private static final long HEAL_COOLDOWN_MS = 1000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        if (player.getHealth() < player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()) {
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
        return knockback;
    }
}