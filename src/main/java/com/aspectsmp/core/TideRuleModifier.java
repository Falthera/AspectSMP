package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class TideRuleModifier implements RuleModifier {

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
        if (player.getLocation().getBlock().getType().toString().contains("WATER")) {
            player.setWalkSpeed(Math.min(player.getWalkSpeed() + 0.02f, 0.45f));
            player.setHealth(Math.min(player.getHealth() + 0.5, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
        }
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.9;
    }
}