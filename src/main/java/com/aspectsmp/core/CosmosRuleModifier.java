package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CosmosRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return damage * 0.5;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        if (player.getLocation().getY() > 100) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, true), true);
        }
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getY() > 100) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0, false, false, true), true);
        }
    }
}
