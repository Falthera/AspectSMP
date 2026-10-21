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

        world.spawnParticle(Particle.FLAME, center, 80, 5.0, 2.0, 5.0, 0.2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 0));

        for (Entity entity : center.getWorld().getNearbyEntities(center, 10.0, 5.0, 10.0)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                damageIgnoreArmor(target, 8.0 + getPowerMultiplier(player, heart), player);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > 200) { cancel(); return; }
                world.spawnParticle(Particle.FLAME, center, 10, 4.0 - (ticks * 0.02), 1.0, 4.0 - (ticks * 0.02), 0.1);
            }
        }.runTaskTimer(com.aspectsmp.AspectSMP.getInstance(), 0L, 2L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 300000L;
    }
}
