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

        player.getWorld().spawnParticle(Particle.DRIPPING_HONEY, player.getLocation().add(0, 1, 0), 30, 1, 1, 1);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        heart.addEssence((long) (5 * getPowerMultiplier(player, heart)));
        
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 200, 1));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE, 200, 0));
        
        player.sendMessage("§6§lLucky Strike! §fFortune favors you!");
        player.sendMessage("§7Gained §f" + (long)(5 * getPowerMultiplier(player, heart)) + " §7Essence.");
        player.sendMessage("§7Temporary luck buff applied for §a10 seconds§7.");

        applyCooldown(player, getId(), 10000L);
        return true;
    }
}