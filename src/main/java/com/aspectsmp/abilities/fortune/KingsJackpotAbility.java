package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.List;
import java.util.Arrays;

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

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, player.getLocation(), 150, 5, 5, 5);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        
        long essence = (long) (50 * getPowerMultiplier(player, heart));
        heart.addEssence(essence);
        
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.LUCK, 600, 1),
            new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 600, 0)
        );
        applyTemporaryEffects(player, effects, 600L);
        
        player.sendMessage("§6+§e" + essence + "§6 Essence!");
        player.sendMessage("§7Fortune smiles upon you!");

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 120000L;
    }
}