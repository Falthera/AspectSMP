package com.aspectsmp.abilities.tide;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class MaelstromAbility extends BaseAbility {

    @Override
    public String getId() {
        return "tide:maelstrom";
    }

    @Override
    public String getName() {
        return "Leviathan's Blessing";
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

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 600, 0));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 600, 0));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DEPTH_STRIDER, 600, 0));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 600, 0));
        
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 40, 1, 1, 1);
        playSound(player, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);

        applyCooldown(player, getId(), 60000L);
        return true;
    }
}
