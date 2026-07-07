package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class TempestRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return 0;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.setWalkSpeed(0.22f);
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.3;
    }
}
