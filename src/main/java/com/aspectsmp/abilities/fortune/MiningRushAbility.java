package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MiningRushAbility extends BaseAbility {

    @Override
    public String getId() {
        return "fortune:mining_rush";
    }

    @Override
    public String getName() {
        return "Mining Rush";
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

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, player.getLocation().add(0, 1, 0), 30);
        playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, 2));
        heart.addEssence((long) (10 * getPowerMultiplier(heart)));

        applyCooldown(player, getId(), 30000L);
        return true;
    }
}