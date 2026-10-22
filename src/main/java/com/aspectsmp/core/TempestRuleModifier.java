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
        
        // Tempest deals more damage when sprinting/momentum
        int level = momentum.getOrDefault(player.getUniqueId(), 0);
        if (level > 0) {
            return damage * (1.0 + level * 0.1); // +10% per momentum level
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true)); // Speed 2
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
    public void applyPassive(Player player) {
        int level = momentum.getOrDefault(player.getUniqueId(), 0);
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, level + 1, false, false, true)); // Speed 2+
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true)); // Speed 2 base
        }
    }
}
