package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastKnockback = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCombat = new ConcurrentHashMap<>();
    private static final long COMBAT_DURATION = 5000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        long now = System.currentTimeMillis();
        if (lastCombat.containsKey(player.getUniqueId()) && now - lastCombat.get(player.getUniqueId()) < COMBAT_DURATION) {
            return damage * 0.9;
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return damage * 1.2;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 40, 0, false, false, true), true);
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

    @Override
    public void applyPassive(Player player) {
    }

    @Override
    public void recordCombat(UUID uuid) {
        lastCombat.put(uuid, System.currentTimeMillis());
    }
}
