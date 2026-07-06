package com.aspectsmp.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class ParticleUtil {

    public static void spawnLine(Location start, Location end, Particle particle, double spacing) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize().multiply(spacing);
        
        for (double d = 0; d < distance; d += spacing) {
            Location loc = start.clone().add(direction.clone().multiply(d / spacing));
            start.getWorld().spawnParticle(particle, loc, 1);
        }
    }

    public static void spawnSphere(Location center, Particle particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.random() * Math.PI;
            double x = radius * Math.cos(theta) * Math.sin(phi);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(theta) * Math.sin(phi);
            center.getWorld().spawnParticle(particle, center.clone().add(x, y, z), 1);
        }
    }

    public static void damageEntities(Collection<Entity> entities, Player attacker, double damage) {
        for (Entity entity : entities) {
            if (entity instanceof org.bukkit.entity.Damageable target) {
                target.damage(damage, attacker);
            }
        }
    }
}