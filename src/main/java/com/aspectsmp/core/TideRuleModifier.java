package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TideRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastHeal = new ConcurrentHashMap<>();
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
        if (!player.getLocation().getBlock().getType().toString().contains("WATER")) return;

        long now = System.currentTimeMillis();
        Long last = lastHeal.get(player.getUniqueId());
        if (last == null || now - last > HEAL_COOLDOWN_MS) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 24, 0, false, false, true));
            lastHeal.put(player.getUniqueId(), now);
        }
        player.setWalkSpeed(0.25f);
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.9;
    }
}
