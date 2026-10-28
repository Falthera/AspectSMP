package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CloudRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return 0; // Full fall immunity (20 PP)
        }
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return damage * 0.3; // 70% lightning reduction (was 30% dmg = 70% reduction)
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getY() > 100) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true), true); // Speed 1
        }
    }
}
