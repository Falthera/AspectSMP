package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.event.NinthSkyEvent;
import com.aspectsmp.event.FrozenEclipseEvent;
import com.aspectsmp.event.ShadowsDescentEvent;
import com.aspectsmp.event.TempestsFuryEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveEventCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public LeaveEventCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (plugin.getEventManager() == null) {
            player.sendMessage("§cEvent system is not available.");
            return true;
        }

        boolean left = false;
        String eventName = "";
        Location returnLoc = plugin.getEventJoinLocation(player.getUniqueId());

        NinthSkyEvent ninthSky = plugin.getEventManager().getNinthSkyEvent();
        if (ninthSky != null && ninthSky.isActive() && ninthSky.getParticipants().contains(player.getUniqueId())) {
            ninthSky.getParticipants().remove(player.getUniqueId());
            left = true;
            eventName = "Awakening of the Ninth Sky";
        }

        FrozenEclipseEvent frozenEclipse = plugin.getEventManager().getFrozenEclipseEvent();
        if (frozenEclipse != null && frozenEclipse.isActive() && frozenEclipse.getParticipants().contains(player.getUniqueId())) {
            frozenEclipse.getParticipants().remove(player.getUniqueId());
            left = true;
            eventName = "Frozen Eclipse";
        }

        ShadowsDescentEvent shadowsDescent = plugin.getEventManager().getShadowsDescentEvent();
        if (shadowsDescent != null && shadowsDescent.isActive() && shadowsDescent.getParticipants().contains(player.getUniqueId())) {
            shadowsDescent.getParticipants().remove(player.getUniqueId());
            left = true;
            eventName = "Shadow's Descent";
        }

        TempestsFuryEvent tempestsFury = plugin.getEventManager().getTempestsFuryEvent();
        if (tempestsFury != null && tempestsFury.isActive() && tempestsFury.getParticipants().contains(player.getUniqueId())) {
            tempestsFury.getParticipants().remove(player.getUniqueId());
            left = true;
            eventName = "Tempest's Fury";
        }

        if (!left) {
            player.sendMessage("§cYou are not participating in any active event.");
            return true;
        }

        player.sendMessage("§eYou have left the " + eventName + " event.");
        player.sendMessage("§7You cannot rejoin this event instance.");
        
        plugin.removeEventJoinLocation(player.getUniqueId());

        Location targetLoc = returnLoc != null ? returnLoc : getOverworldSpawn();
        if (targetLoc != null) {
            player.teleport(targetLoc);
            player.sendMessage("§aReturned to your previous location.");
        } else {
            player.sendMessage("§aReturned to spawn.");
        }

        return true;
    }

    private Location getOverworldSpawn() {
        World overworld = Bukkit.getWorld("world");
        if (overworld != null) {
            Location spawn = overworld.getSpawnLocation();
            int safeY = overworld.getHighestBlockYAt(spawn);
            spawn.setY(safeY + 1);
            return spawn;
        }
        return null;
    }
}