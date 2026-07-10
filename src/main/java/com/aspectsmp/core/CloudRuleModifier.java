package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CloudRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastWindBoost = new ConcurrentHashMap<>();
    private static final long WIND_COOLDOWN_MS = 1500;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return damage * 0.5;
        }
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return damage * 0.3;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastWindBoost.get(player.getUniqueId());
        if (last != null && now - last < WIND_COOLDOWN_MS) return;

        if (player.getLocation().getY() > player.getWorld().getHighestBlockYAt(player.getLocation()) + 10) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false, true), true);
            lastWindBoost.put(player.getUniqueId(), now);
        }
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getY() > 100) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true), true);
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 0, false, false, true), true);
    }
}
