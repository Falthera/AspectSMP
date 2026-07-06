package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class WarRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        return damage * 1.2;
    }

    @Override
    public void modifyMovement(Player player) {
        player.setWalkSpeed(0.25f);
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback;
    }
}