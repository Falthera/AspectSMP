package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class CosmosRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback * 0.2;
    }
}
