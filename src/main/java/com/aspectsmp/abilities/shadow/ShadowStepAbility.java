package com.aspectsmp.abilities.shadow;

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
import java.util.List;
import java.util.Arrays;

public class ShadowStepAbility extends BaseAbility {

    @Override
    public String getId() {
        return "shadow:shadow_step";
    }

    @Override
    public String getName() {
        return "Shadow Step";
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

        org.bukkit.Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(6));
        if (loc.getBlock().getType().isSolid()) return false;
        
        loc.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        
        player.teleport(loc);
        
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false)
        );
        applyTemporaryEffects(player, effects, 60L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 8000L;
    }
}