package com.aspectsmp.abilities.cosmos;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class GravityPullAbility extends BaseAbility {

    @Override
    public String getId() {
        return "cosmos:gravity_pull";
    }

    @Override
    public String getName() {
        return "Gravity Well";
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

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 80, 3, 3, 3);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 2, 2, 2);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_HURT, 1.2f, 0.8f);
        playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e != player && e.getLocation().distanceSquared(loc) <= 36)
            .toList();

        for (Entity entity : nearby) {
            Vector vec = loc.clone().add(0, 1, 0).toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.7);
            entity.setVelocity(entity.getVelocity().add(vec));
            if (entity instanceof Player) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, false, true));
            }
        }

        applyCooldown(player, getId(), 5000L);
        return true;
    }
}
