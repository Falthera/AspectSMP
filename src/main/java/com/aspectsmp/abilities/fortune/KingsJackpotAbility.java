package com.aspectsmp.abilities.fortune;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import com.aspectsmp.AspectSMP;
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

        AspectSMP.getInstance().getVisualEffectsManager().playUltimateActivationVisuals(
            player, com.aspectsmp.core.AspectType.FORTUNE, getId());

        // Visuals: Gold/coin themed
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 150, 5, 5, 5);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
        
        // Essence gain: 30 * powerMultiplier (was 50)
        // Power cost: 25 essence * 2 min cd = utility value
        long essence = (long) (30 * getPowerMultiplier(player, heart));
        heart.addEssence(essence);
        
        // Utility buffs: Luck 1 + Hero of Village 1 for 30s
        // Power cost: 2 utility * 30s = 30 PP
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.LUCK, 600, 0),              // Luck 1 (30s)
            new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 600, 0) // Hero 1 (30s)
        );
        applyTemporaryEffects(player, effects, 600L);
        
        // Cooldown: 2 min (same)
        // Uptime: 30s/120s = 25%
        // Total cost: ~40 PP (utility-focused)
        
        player.sendMessage("§6+§e" + essence + "§6 Essence!");
        player.sendMessage("§7Fortune smiles upon you!");

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 120000L; // 2 minutes
    }
}