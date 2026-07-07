package com.aspectsmp.abilities.inferno;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class InfernalChainsAbility extends BaseAbility {

    @Override
    public String getId() {
        return "inferno:infernal_chains";
    }

    @Override
    public String getName() {
        return "Infernal Chains";
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
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        Entity target = player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .min((a, b) -> Double.compare(a.getLocation().distanceSquared(player.getLocation()),
                  b.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);

        if (target instanceof Player p) {
            p.damage(4 * getPowerMultiplier(heart), player);
            p.setFireTicks(p.getFireTicks() + 80);
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 4));
            player.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
            playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        }

        applyCooldown(player, getId(), 15000L);
        return true;
    }
}
