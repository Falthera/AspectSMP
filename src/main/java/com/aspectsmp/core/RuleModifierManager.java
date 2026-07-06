package com.aspectsmp.core;

import com.aspectsmp.AspectSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.EnumMap;
import java.util.Map;

public class RuleModifierManager implements Listener {

    private final AspectSMP plugin;
    private final Map<AspectType, RuleModifier> modifiers;

    public RuleModifierManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.modifiers = new EnumMap<>(AspectType.class);
        initializeModifiers();
    }

    private void initializeModifiers() {
        modifiers.put(AspectType.INFERNO, new InfernoRuleModifier());
        modifiers.put(AspectType.TIDE, new TideRuleModifier());
        modifiers.put(AspectType.TEMPEST, new TempestRuleModifier());
        modifiers.put(AspectType.RIFT, new RiftRuleModifier());
        modifiers.put(AspectType.VITALITY, new VitalityRuleModifier());
        modifiers.put(AspectType.WAR, new WarRuleModifier());
        modifiers.put(AspectType.COSMOS, new CosmosRuleModifier());
        modifiers.put(AspectType.FORTUNE, new FortuneRuleModifier());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        RuleModifier modifier = modifiers.get(heart.getAspect());
        if (modifier != null) {
            event.setDamage(modifier.modifyDamage(player, event.getDamage(), event.getCause()));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(event.getPlayer().getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        RuleModifier modifier = modifiers.get(heart.getAspect());
        if (modifier != null) {
            modifier.modifyMovement(event.getPlayer());
        }
    }

    public RuleModifier getModifier(AspectType type) {
        return modifiers.get(type);
    }
}