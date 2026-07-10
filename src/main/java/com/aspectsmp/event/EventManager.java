package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EventManager implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private NinthSkyEvent ninthSkyEvent;
    private FrozenEclipseEvent frozenEclipseEvent;

    public EventManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public NinthSkyEvent getNinthSkyEvent() {
        return ninthSkyEvent;
    }

    public FrozenEclipseEvent getFrozenEclipseEvent() {
        return frozenEclipseEvent;
    }

    public void startNinthSkyEvent() {
        if (ninthSkyEvent == null || !ninthSkyEvent.isActive()) {
            ninthSkyEvent = new NinthSkyEvent(plugin);
            ninthSkyEvent.startEvent();
        }
    }

    public void startFrozenEclipseEvent() {
        if (frozenEclipseEvent == null || !frozenEclipseEvent.isActive()) {
            frozenEclipseEvent = new FrozenEclipseEvent(plugin);
            frozenEclipseEvent.startEvent();
        }
    }

    public void stopActiveEvents() {
        if (ninthSkyEvent != null && ninthSkyEvent.isActive()) {
            ninthSkyEvent.stopEvent();
        }
        if (frozenEclipseEvent != null && frozenEclipseEvent.isActive()) {
            frozenEclipseEvent.stopEvent();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /event <event_name|stop>");
            return true;
        }
        String eventName = String.join("_", args);
        if (eventName.equalsIgnoreCase("The_Awakening_of_the_Ninth_Sky")) {
            startNinthSkyEvent();
            sender.sendMessage("§aStarted The Awakening of the Ninth Sky event!");
        } else if (eventName.equalsIgnoreCase("The_Frozen_Eclipse")) {
            startFrozenEclipseEvent();
            sender.sendMessage("§aStarted The Frozen Eclipse event!");
        } else if (eventName.equalsIgnoreCase("stop")) {
            stopActiveEvents();
            sender.sendMessage("§cStopped all active events.");
        } else {
            sender.sendMessage("§cUnknown event: " + eventName);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        completions.add("The_Awakening_of_the_Ninth_Sky");
        completions.add("The_Frozen_Eclipse");
        completions.add("stop");
        return completions;
    }
}
