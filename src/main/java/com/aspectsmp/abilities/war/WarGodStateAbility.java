package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class WarGodStateAbility extends BaseAbility {

    @Override
    public String getId() {
        return "war:war_god_state";
    }

    @Override
    public String getName() {
        return "War God State";
    }

    @Override
    public int getTier() {
        return 3;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean isUltimate() {
        return true;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        Location center = player.getLocation();
        World world = player.getWorld();

        world.spawnParticle(Particle.FLAME, center, 100, 5.0, 2.0, 5.0, 0.2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 5);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 2));

        for (Entity entity : center.getWorld().getNearbyEntities(center, 12.0, 6.0, 12.0)) {
            if (entity instanceof LivingEntity target && target != player) {
                target.damage(10.0 + getPowerMultiplier(player, heart) * 2, player);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > 300) { cancel(); return; }
                world.spawnParticle(Particle.FLAME, center, 15, 4.0 - (ticks * 0.013), 1.0, 4.0 - (ticks * 0.013), 0.1);
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 2L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 470000L;
    }
}
