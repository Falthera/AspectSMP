package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.attribute.Attribute;

public class RageModeAbility extends BaseAbility {

    @Override
    public String getId() {
        return "war:rage_mode";
    }

    @Override
    public String getName() {
        return "Rage Mode";
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 50, 1, 1, 1);
        playSound(player, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f);

        double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * getPowerMultiplier(heart);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(
            player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue() * 1.5);

        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(
                    player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue() / 1.5);
            }
        };
        runnable.runTaskLater(com.aspectsmp.AspectSMP.getInstance(), 100L);

        applyCooldown(player, getId(), 30000L);
        return true;
    }
}
