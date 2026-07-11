package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class BerserkSlashAbility extends BaseAbility {

    @Override
    public String getId() {
        return "war:berserk_slash";
    }

    @Override
    public String getName() {
        return "Berserk Slash";
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
        loc.getWorld().spawnParticle(Particle.CRIT, loc.add(loc.getDirection()), 30, 1, 1, 1);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 9)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target) {
                damageIgnoreArmor(target, 6 * getPowerMultiplier(player, heart), player);
            }
        }

        applyCooldown(player, getId(), 6000L);
        return true;
    }
}
