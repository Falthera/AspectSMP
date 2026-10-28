package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InfernoRuleModifier implements RuleModifier {

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FIRE ||
            cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
            cause == EntityDamageEvent.DamageCause.LAVA) {
            return 0; // Full fire immunity (20 PP)
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return damage * 1.1; // 10% damage bonus (was 15%)
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        player.setFireTicks(0);
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getBlock().getBiome().getKey().toString().contains("nether") ||
            player.getLocation().getBlock().getBiome().getKey().toString().contains("desert") ||
            player.getLocation().getBlock().getBiome().getKey().toString().contains("badlands")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false, true), true); // Str 1
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, false, false, true), true); // Fire Res 1 (was 80 ticks)
        }
    }
}
