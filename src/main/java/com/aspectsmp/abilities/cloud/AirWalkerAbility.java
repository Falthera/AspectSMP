package com.aspectsmp.abilities.cloud;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

public class AirWalkerAbility extends BaseAbility {

    private static final int MAX_CHARGES = 3;
    private static final long RECHARGE_DELAY_MS = 2000L;

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
        if (isOnCooldown(player, getId())) return false;

        Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(0.3);
        player.setVelocity(direction.multiply(2.0));

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.02);
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.2f);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return RECHARGE_DELAY_MS;
    }
}
