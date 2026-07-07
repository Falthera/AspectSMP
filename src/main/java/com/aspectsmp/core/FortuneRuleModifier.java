package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class FortuneRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.CACTUS ||
            cause == EntityDamageEvent.DamageCause.SWEET_BERRY_BUSH) {
            return damage * 0.3;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
    }

    @Override
    public double modifyKnockback(Player player, double knockback) {
        return knockback;
    }
}
