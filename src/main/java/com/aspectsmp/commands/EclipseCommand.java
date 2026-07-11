package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.event.FrozenEclipseEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EclipseCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public EclipseCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        FrozenEclipseEvent eclipse = plugin.getEventManager().getFrozenEclipseEvent();
        if (eclipse == null || !eclipse.isActive()) {
            player.sendMessage("§cThere is no active Frozen Eclipse event.");
            return true;
        }

        if (eclipse.getPhase() != com.aspectsmp.event.EventPhase.FIRST_FROST) {
            player.sendMessage("§cThe join period has ended.");
            return true;
        }

        eclipse.addParticipant(player);
        return true;
    }
}
