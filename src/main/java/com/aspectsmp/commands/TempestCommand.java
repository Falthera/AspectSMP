package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.event.TempestsFuryEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempestCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public TempestCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        TempestsFuryEvent tempest = plugin.getEventManager().getTempestsFuryEvent();
        if (tempest == null || !tempest.isActive()) {
            player.sendMessage("§cThere is no active Tempest's Fury event.");
            return true;
        }

        if (tempest.getPhase() != com.aspectsmp.event.EventPhase.STORM_AWAKENS &&
            tempest.getPhase() != com.aspectsmp.event.EventPhase.STORM_FRAGMENTS &&
            tempest.getPhase() != com.aspectsmp.event.EventPhase.STORM_GUSTS) {
            player.sendMessage("§cThe join period has ended.");
            return true;
        }

        // Check if player already left this event
        if (plugin.hasEventJoinLocation(player.getUniqueId())) {
            player.sendMessage("§cYou have already left this event and cannot rejoin.");
            return true;
        }

        // Save player's current location for /leaveevent
        plugin.setEventJoinLocation(player.getUniqueId(), player.getLocation());
        
        tempest.addParticipant(player);
        return true;
    }
}