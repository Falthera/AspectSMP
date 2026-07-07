package com.aspectsmp.abilities.rift;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class VoidSwapAbility extends BaseAbility {

    private static final double MAX_RANGE = 200.0;
    private static final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;

    @Override
    public String getId() {
        return "rift:void_swap";
    }

    @Override
    public String getName() {
        return "Void Swap";
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

        org.bukkit.entity.Entity target = player.getWorld().getEntities().stream()
            .filter(e -> e instanceof Player && e != player)
            .filter(e -> e.getLocation().distanceSquared(player.getLocation()) <= MAX_RANGE_SQ)
            .min((a, b) -> Double.compare(a.getLocation().distanceSquared(player.getLocation()),
                  b.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);

        if (target instanceof Player p) {
            if (com.aspectsmp.AspectSMP.getInstance().getTrustManager().isTrusted(player.getUniqueId(), p.getUniqueId())) {
                player.sendMessage("§cYou cannot Void Swap with a trusted player!");
                return false;
            }

            Location playerLoc = player.getLocation().clone();
            Location targetLoc = p.getLocation().clone();

            player.getWorld().spawnParticle(Particle.PORTAL, playerLoc.add(0, 1, 0), 50);
            player.getWorld().spawnParticle(Particle.PORTAL, targetLoc.add(0, 1, 0), 50);

            playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            player.teleport(targetLoc);
            p.teleport(playerLoc);
        } else {
            player.sendMessage("§cNo target within §f" + (int) MAX_RANGE + " §cblocks!");
            return false;
        }

        applyCooldown(player, getId(), 15000L);
        return true;
    }
}
