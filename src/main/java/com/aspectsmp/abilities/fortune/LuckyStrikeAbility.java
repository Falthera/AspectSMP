package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class LuckyStrikeAbility extends BaseAbility {

    @Override
    public String getId() {
        return "fortune:lucky_strike";
    }

    @Override
    public String getName() {
        return "Lucky Strike";
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

        player.getWorld().spawnParticle(Particle.HAPPY_HONEY, player.getLocation().add(0, 1, 0), 30, 1, 1, 1);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        heart.addEssence(5 * getPowerMultiplier(heart));

        applyCooldown(player, getId(), 10000L);
        return true;
    }
}