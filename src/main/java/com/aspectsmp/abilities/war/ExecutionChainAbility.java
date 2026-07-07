package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class ExecutionChainAbility extends BaseAbility {

    @Override
    public String getId() {
        return "war:execution_chain";
    }

    @Override
    public String getName() {
        return "Execution Chain";
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

        org.bukkit.Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.CRIT, loc, 100, 2, 2, 2);
        playSound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

        List<Entity> nearby = loc.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e instanceof Player p && p.getHealth() <= 8)
            .filter(e -> e.getLocation().distanceSquared(loc) <= 25)
            .toList();

        for (Entity entity : nearby) {
            if (entity instanceof Player target && target.getHealth() <= 8) {
                target.damage(20 * getPowerMultiplier(heart), player);
            }
        }

        applyCooldown(player, getId(), 25000L);
        return true;
    }
}
