package com.aspectsmp.abilities.cloud;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AirWalkerAbility extends BaseAbility {

    private static final int MAX_CHARGES = 3;
    private static final long RECHARGE_DELAY_TICKS = 60L;
    private static final long RECHARGE_INTERVAL_TICKS = 60L;
    private static final long FULL_RECHARGE_MILLIS = (RECHARGE_DELAY_TICKS + (MAX_CHARGES - 1) * RECHARGE_INTERVAL_TICKS) * 50L;

    private final Map<UUID, Integer> charges = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> regenTasks = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "cloud:air_walker";
    }

    @Override
    public String getName() {
        return "Air Walker";
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;

        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, MAX_CHARGES);
        if (currentCharges <= 0) return false;

        charges.put(uuid, currentCharges - 1);
        heart.getCooldowns().put(getId(), System.currentTimeMillis() + FULL_RECHARGE_MILLIS);
        startRegenTask(player);

        Vector direction;
        if (player.isSneaking()) {
            direction = new Vector(0, 1.0, 0);
        } else {
            direction = player.getLocation().getDirection().normalize();
            direction.setY(0.3);
        }
        player.setVelocity(direction.multiply(2.0));

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.02);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.2f);

        return true;
    }

    private void startRegenTask(Player player) {
        UUID uuid = player.getUniqueId();
        regenTasks.computeIfAbsent(uuid, id -> new BukkitRunnable() {
            @Override
            public void run() {
                int current = charges.getOrDefault(uuid, 0);
                if (current >= MAX_CHARGES) {
                    cancel();
                    regenTasks.remove(uuid);
                    return;
                }
                charges.put(uuid, current + 1);
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), RECHARGE_DELAY_TICKS, RECHARGE_INTERVAL_TICKS));
    }

    public int getCurrentCharges(Player player) {
        return charges.getOrDefault(player.getUniqueId(), MAX_CHARGES);
    }

    public int getMaxCharges() {
        return MAX_CHARGES;
    }

    @Override
    protected long getCooldownBase() {
        return FULL_RECHARGE_MILLIS;
    }
}
