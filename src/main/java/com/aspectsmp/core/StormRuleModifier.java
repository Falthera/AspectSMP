package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StormRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastWind = new ConcurrentHashMap<>();
    private static final long WIND_COOLDOWN_MS = 2000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return 0;
        }
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return 0;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastWind.get(player.getUniqueId());
        if (last != null && now - last < WIND_COOLDOWN_MS) return;

        if (player.getLocation().getY() > 100 || player.getWorld().hasStorm()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 40, 0, true, false), true);
            org.bukkit.util.Vector wind = player.getLocation().getDirection().multiply(0.3).setY(0.1);
            player.setVelocity(player.getVelocity().add(wind));
            lastWind.put(player.getUniqueId(), now);
        }
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getY() > 80 || player.getWorld().hasStorm()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.JUMP_BOOST, 60, 0, true, false), true);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 60, 0, true, false), true);
        }
    }
}