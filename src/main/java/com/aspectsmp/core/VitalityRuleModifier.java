package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastHeal = new ConcurrentHashMap<>();
    private static final long HEAL_COOLDOWN_MS = 1000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.POISON ||
            cause == EntityDamageEvent.DamageCause.WITHER) {
            return 0;
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return damage * 0.9;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastHeal.get(player.getUniqueId());
        if (last != null && now - last < HEAL_COOLDOWN_MS) return;

        if (player.getHealth() < player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 24, 0, false, false, true));
            lastHeal.put(player.getUniqueId(), now);
        }
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback;
    }
}
