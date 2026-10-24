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

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, player.getLocation().add(0, 1, 0), 25, 1, 1, 1);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        long essenceGain = (long) (5 * getPowerMultiplier(player, heart));
        heart.addEssence(essenceGain);
        
        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.LUCK, 200, 0),
            new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 200, 0)
        );
        applyTemporaryEffects(player, effects, 200L);
        
        player.sendMessage("§6§lLucky Strike! §fFortune favors you!");
        player.sendMessage("§7Gained §f" + essenceGain + " §7Essence.");
        player.sendMessage("§7Temporary luck buff applied for §a10 seconds§7.");

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 8000L;
    }
}