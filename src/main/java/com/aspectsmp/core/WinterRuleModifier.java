package com.aspectsmp.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WinterRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastColdCheck = new ConcurrentHashMap<>();
    private static final long COLD_CHECK_COOLDOWN_MS = 1000;

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.FREEZE) {
            return damage * 0.3;
        }
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return damage * 0.5;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastColdCheck.get(player.getUniqueId());
        if (last != null && now - last < COLD_CHECK_COOLDOWN_MS) return;

        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = loc.getWorld();
        if (world == null) return;

        if (world.getBiome(loc).getKey().toString().equals("minecraft:snowy_plains") ||
            world.getBiome(loc).getKey().toString().equals("minecraft:snowy_tundra") ||
            world.getBiome(loc).getKey().toString().equals("minecraft:ice_spikes") ||
            world.getBiome(loc).getKey().toString().equals("minecraft:glacial_peaks") ||
            world.getBiome(loc).getKey().toString().equals("minecraft:frozen_peaks")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false, true), true);
        }
        lastColdCheck.put(player.getUniqueId(), now);
    }

    @Override
    public void applyPassive(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0, true, false, true), true);
    }
}
