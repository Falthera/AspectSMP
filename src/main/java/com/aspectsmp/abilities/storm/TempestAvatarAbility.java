package com.aspectsmp.abilities.storm;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Arrays;

public class TempestAvatarAbility extends BaseAbility {

    @Override
    public String getId() {
        return "storm:tempest_avatar";
    }

    @Override
    public String getName() {
        return "Tempest Avatar";
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

        org.bukkit.Location center = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 20, 3, 3, 3, 0.2);
        player.getWorld().spawnParticle(Particle.CLOUD, center.add(0, 1, 0), 100, 5, 5, 5, 0.1); // was 200
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.add(0, 1, 0), 100, 4, 3, 4, 0.1);
        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 3.0f, 0.4f);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);

        // Balanced buffs: Levitation 1, Speed 1, Strength 1, Resistance 2 (15s)
        // Was: Levitation, Speed 2, Strength 1, Resistance 1+2 for 15s
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.LEVITATION, 300, 0, true, false),
            new PotionEffect(PotionEffectType.SPEED, 300, 1),      // was 2
            new PotionEffect(PotionEffectType.STRENGTH, 300, 0),    // was 1
            new PotionEffect(PotionEffectType.RESISTANCE, 300, 1)   // was 1+2
        );
        applyTemporaryEffects(player, effects, 300L);

        // Persistent storm: 15s duration (was 15s)
        // Lightning: 6 dmg + Slow 1 (was 8 dmg + Slow 2)
        // Wind push: 1.0 force (was 1.2)
        // Radius: 6 blocks (was 8)
        // Cooldown: 3 min (was 3 min)
        // Uptime: 15s/180s = 8.3%
        // Total cost: ~48 PP (DPS/Control hybrid)
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 300) { cancel(); return; }
                ticks += 5;
                
                org.bukkit.Location pLoc = player.getLocation();
                
                // Storm particles
                pLoc.getWorld().spawnParticle(Particle.CLOUD, pLoc.add(0, 1, 0), 20, 4, 2, 4, 0.05); // was 30
                pLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, pLoc.add(0, 1, 0), 20, 3, 1.5, 3, 0.03);
                pLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc.add(0, 2, 0), 10, 2, 2, 2, 0.02);
                
                // Lightning strikes nearby enemies
                if (ticks % 20 == 0) {
                    for (Entity entity : pLoc.getWorld().getNearbyEntities(pLoc, 6, 6, 6)) { // was 8
                        if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                            target.getWorld().strikeLightningEffect(target.getLocation());
                            damageIgnoreArmor(target, 6.0 * getPowerMultiplier(player, heart), player); // was 8
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1), true); // was 2
                        }
                    }
                }
                
                // Wind push
                if (ticks % 40 == 0) {
                    for (Entity entity : pLoc.getWorld().getNearbyEntities(pLoc, 8, 5, 8)) { // was 10
                        if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                            Vector push = target.getLocation().toVector().subtract(pLoc.toVector()).normalize().multiply(1.0); // was 1.2
                            push.setY(0.5);
                            target.setVelocity(target.getVelocity().add(push));
                        }
                    }
                }
            }
        }.runTaskTimer(AspectSMP.getInstance(), 0L, 5L);

        // Passenger flight - player can fly for 15s
        player.setAllowFlight(true);
        player.setFlying(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }.runTaskLater(AspectSMP.getInstance(), 300L);

        // Cooldown: 3 min (same)
        // Uptime: 15s/180s = 8.3%
        // Total cost: ~48 PP (DPS/Control hybrid)
        applyCooldown(player, getId(), 180000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 180000L;
    }
}