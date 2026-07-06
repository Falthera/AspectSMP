package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public interface RuleModifier {
    double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause);
    void modifyMovement(Player player);
    double modifyKnockback(Player player, double knockback);
}