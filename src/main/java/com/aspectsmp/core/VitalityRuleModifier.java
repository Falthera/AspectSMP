package com.aspectsmp.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VitalityRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastSpeed = new ConcurrentHashMap<>();
    private static final long SPEED_COOLDOWN_MS = 4000;
    private final java.util.Set<Material> naturalBlocks = java.util.Set.of(
        Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
        Material.SAND, Material.RED_SAND, Material.STONE, Material.DEEPSLATE,
        Material.NETHERRACK, Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM,
        Material.MUD, Material.PODZOL, Material.MYCELIUM, Material.SOUL_SAND,
        Material.SOUL_SOIL, Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.CHERRY_LOG, Material.DARK_OAK_LOG
    );

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.POISON ||
            cause == EntityDamageEvent.DamageCause.WITHER) {
            return 0;
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return damage * 0.9;
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastSpeed.get(player.getUniqueId());
        if (last != null && now - last < SPEED_COOLDOWN_MS) return;

        Block feet = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (!naturalBlocks.contains(feet.getType())) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true), true);
        lastSpeed.put(player.getUniqueId(), now);
    }

    @Override
    public void applyPassive(Player player) {
        Block feet = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (naturalBlocks.contains(feet.getType())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false, true), true);
        }
    }
}
