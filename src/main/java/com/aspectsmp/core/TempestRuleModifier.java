package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class TempestRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.setWalkSpeed(Math.min(player.getWalkSpeed() + 0.01f, 0.45f));
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback;
    }
}