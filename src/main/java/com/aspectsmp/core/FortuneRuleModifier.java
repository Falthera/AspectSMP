package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FortuneRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.CONTACT) {
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

    @Override
    public void applyPassive(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 80, 1, false, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 80, 0, false, false, true), true);
    }
}
