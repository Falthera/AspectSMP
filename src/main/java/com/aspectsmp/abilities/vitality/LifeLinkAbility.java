package com.aspectsmp.abilities.vitality;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class LifeLinkAbility extends BaseAbility {

    @Override
    public String getId() {
        return "vitality:life_link";
    }

    @Override
    public String getName() {
        return "Life Link";
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

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1, 0), 50);
        playSound(player, Sound.ENTITY_WITCH_CAST, 1.0f, 1.0f);

        org.bukkit.entity.Entity target = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 9)
            .findFirst()
            .orElse(null);

        if (target instanceof Player p) {
            player.setHealth(Math.min(player.getHealth() + 4, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            p.damage(2 * getPowerMultiplier(heart));
        }

        applyCooldown(player, getId(), 12000L);
        return true;
    }
}