package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class KingsJackpotAbility extends BaseAbility {

    @Override
    public String getId() {
        return "fortune:kings_jackpot";
    }

    @Override
    public String getName() {
        return "King's Fortune";
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

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, player.getLocation(), 200, 5, 5, 5);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        
        long essence = (long) (25 * getPowerMultiplier(player, heart));
        heart.addEssence(essence);
        
        player.sendMessage("§6+§e" + essence + "§6 Essence!");

        applyCooldown(player, getId(), 120000L);
        return true;
    }
}