package com.aspectsmp.abilities.war;

import com.aspectsmp.abilities.BaseAbility;
import com.aspectsmp.core.Heart;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.List;
import java.util.Arrays;

public class RageModeAbility extends BaseAbility {

    @Override
    public String getId() {
        return "war:rage_mode";
    }

    @Override
    public String getName() {
        return "Rage Mode";
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public boolean execute(Player player, Heart heart, Event triggerEvent) {
        if (!canExecute(player, heart)) return false;
        if (isOnCooldown(player, getId())) return false;

        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 35, 1, 1, 1);
        playSound(player, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f);

        List<PotionEffect> effects = Arrays.asList(
            new PotionEffect(PotionEffectType.STRENGTH, 80, 0),
            new PotionEffect(PotionEffectType.SPEED, 80, 0),
            new PotionEffect(PotionEffectType.RESISTANCE, 80, 0)
        );
        applyTemporaryEffects(player, effects, 80L);

        applyCooldown(player, getId(), getCooldownDuration(heart));
        return true;
    }

    @Override
    protected long getCooldownBase() {
        return 25000L;
    }
}
