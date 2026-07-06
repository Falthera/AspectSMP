package com.aspectsmp.abilities;

import com.aspectsmp.core.Heart;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface Ability {
    String getId();
    String getName();
    int getTier();
    boolean isPassive();
    default boolean isUltimate() { return false; }
    
    default boolean canExecute(Player player, Heart heart) {
        if (heart.isDormant()) return false;
        if (heart.getTier() < getTier()) return false;
        return true;
    }
    
    boolean execute(Player player, Heart heart, Event triggerEvent);
    
    default void applyPassive(Player player, Heart heart) {}
}