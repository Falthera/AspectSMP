package com.aspectsmp.abilities.vitality;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;

public class RevivalShieldAbility extends BaseAbility {

    @Override
    public String getId() {
        return "vitality:revival_shield";
    }

    @Override
    public String getName() {
        return "Revival Shield";
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

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 100, 1, 1, 1);
        playSound(player, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        
        player.setMetadata("revival_shield", new FixedMetadataValue(com.aspectsmp.AspectSMP.getInstance(), 
            System.currentTimeMillis() + 300000));
        
        applyCooldown(player, getId(), 180000L);
        return true;
    }
}