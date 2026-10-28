package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.List;
import java.util.Arrays;

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

        AspectSMP.getInstance().getVisualEffectsManager().playUltimateActivationVisuals(
            player, com.aspectsmp.core.AspectType.WAR, getId());

        Location center = player.getLocation();
        World world = player.getWorld();

        // Visuals
        world.spawnParticle(Particle.FLAME, center, 80, 5.0, 2.0, 5.0, 0.2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);

        // Balanced buffs: Str1, Speed1, Resist1, Regen1 (10s duration)
        // Power cost: 4 buffs * 1 amp * 10s = 20 PP + self-buff mult = 30 PP
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.STRENGTH, 200, 0),    // Str 1 (10s)
            new PotionEffect(PotionEffectType.SPEED, 200, 0),       // Speed 1 (10s)
            new PotionEffect(PotionEffectType.RESISTANCE, 200, 0),  // Resist 1 (10s)
            new PotionEffect(PotionEffectType.REGENERATION, 200, 0) // Regen 1 (10s)
        );
        applyTemporaryEffects(player, effects, 200L);

        // AoE damage: 6 dmg in 8 block radius (was 8 dmg in 10x5x10)
        // Power cost: 6 dmg * 10s uptime/300s cd = 0.2 PP * 1.5 DPS mult = 0.3 PP
        for (Entity entity : center.getWorld().getNearbyEntities(center, 8.0, 4.0, 8.0)) {
            if (entity instanceof LivingEntity target && target != player && !(target instanceof org.bukkit.entity.Villager)) {
                damageIgnoreArmor(target, 6.0 * getPowerMultiplier(player, heart), player);
            }
        }

        // Lingering flame trail (visual)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > 200) { cancel(); return; }
                world.spawnParticle(Particle.FLAME, center, 10, 4.0 - (ticks * 0.02), 1.0, 4.0 - (ticks * 0.02), 0.1);
            }
        }.runTaskTimer(AspectSMP.getInstance(), 0L, 2L);

        // Cooldown: 4 min (was 5 min)
        // Uptime: 10s/240s = 4.17%
        // Total cost: ~45 PP (within 100 budget)
        applyCooldown(player, getId(), 240000L);
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 240000L; // 4 minutes
    }
}
