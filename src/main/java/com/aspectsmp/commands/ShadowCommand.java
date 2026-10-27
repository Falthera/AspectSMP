package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.event.ShadowsDescentEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShadowCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public ShadowCommand(AspectSMP plugin) {
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

        ShadowsDescentEvent shadow = plugin.getEventManager().getShadowsDescentEvent();
        if (shadow == null || !shadow.isActive()) {
            player.sendMessage("§cThere is no active Shadow's Descent event.");
            return true;
        }

        if (shadow.getPhase() != com.aspectsmp.event.EventPhase.SHADOW_AWAKENS &&
            shadow.getPhase() != com.aspectsmp.event.EventPhase.SHADOW_FRAGMENTS &&
            shadow.getPhase() != com.aspectsmp.event.EventPhase.SHADOW_RIFTS) {
            player.sendMessage("§cThe join period has ended.");
            return true;
        }

        if (plugin.hasEventJoinLocation(player.getUniqueId())) {
            player.sendMessage("§cYou have already left this event and cannot rejoin.");
            return true;
        }

        plugin.setEventJoinLocation(player.getUniqueId(), player.getLocation());
        
        shadow.addParticipant(player);
        return true;
    }
}