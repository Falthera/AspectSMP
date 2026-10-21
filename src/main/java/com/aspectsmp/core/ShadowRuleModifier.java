package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastStealth = new ConcurrentHashMap<>();
    private static final long STEALTH_COOLDOWN_MS = 3000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            return damage * 0.5;
        }
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return damage * 0.7;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastStealth.get(player.getUniqueId());
        if (last != null && now - last < STEALTH_COOLDOWN_MS) return;

        if (player.isSneaking() && player.getLocation().getBlock().getLightLevel() < 7) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, true, false), true);
            lastStealth.put(player.getUniqueId(), now);
        }
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getBlock().getLightLevel() < 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0, true, false), true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false), true);
        }
    }
}