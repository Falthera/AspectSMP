package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.UUID;

public interface RuleModifier {
    double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause);
    void modifyMovement(Player player);
    double modifyKnockback(Player player, double knockback);
    void applyPassive(Player player);
    default void recordCombat(UUID uuid) {}
}