package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EventManager implements CommandExecutor {

    private final AspectSMP plugin;
    private NinthSkyEvent currentEvent;

    public EventManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void startNinthSkyEvent() {
        if (currentEvent == null || !currentEvent.isActive()) {
            currentEvent = new NinthSkyEvent(plugin);
            currentEvent.startEvent();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /event start <event_name>");
            return true;
        }
        String eventName = String.join("_", args);
        if (eventName.equalsIgnoreCase("The_Awakening_of_the_Ninth_Sky")) {
            startNinthSkyEvent();
            sender.sendMessage("§aStarted The Awakening of the Ninth Sky event!");
        } else {
            sender.sendMessage("§cUnknown event: " + eventName);
        }
        return true;
    }
}
