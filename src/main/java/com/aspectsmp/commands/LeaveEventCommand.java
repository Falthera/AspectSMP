package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
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

        var ninthSky = plugin.getEventManager().getNinthSkyEvent();
        if (ninthSky == null || !ninthSky.isActive()) {
            player.sendMessage("§cThere is no active Ninth Sky event.");
            return true;
        }

        if (!ninthSky.getParticipants().contains(player.getUniqueId())) {
            player.sendMessage("§cYou are not participating in the Ninth Sky event.");
            return true;
        }

        ninthSky.getParticipants().remove(player.getUniqueId());
        player.sendMessage("§eYou have left the Ninth Sky event.");

        World overworld = Bukkit.getWorlds().get(0);
        if (overworld != null) {
            Location spawn = overworld.getSpawnLocation();
            int safeY = overworld.getHighestBlockYAt(spawn);
            spawn.setY(safeY + 1);
            player.teleport(spawn);
        }

        return true;
    }
}
