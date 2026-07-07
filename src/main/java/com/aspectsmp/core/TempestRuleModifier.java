package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TempestRuleModifier implements RuleModifier {

    private final Map<UUID, Integer> momentum = new ConcurrentHashMap<>();

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return 0;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true));
        if (player.isSprinting()) {
            int level = momentum.getOrDefault(player.getUniqueId(), 0);
            if (level < 5) {
                momentum.put(player.getUniqueId(), level + 1);
            }
        } else {
            momentum.remove(player.getUniqueId());
        }
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.3;
    }

    @Override
    public void applyPassive(Player player) {
        int level = momentum.getOrDefault(player.getUniqueId(), 0);
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, level, false, false, true));
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true));
        }
    }
}
