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
    private ShadowsDescentEvent shadowsDescentEvent;
    private TempestsFuryEvent tempestsFuryEvent;

    public EventManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public NinthSkyEvent getNinthSkyEvent() {
        return ninthSkyEvent;
    }

    public FrozenEclipseEvent getFrozenEclipseEvent() {
        return frozenEclipseEvent;
    }

    public ShadowsDescentEvent getShadowsDescentEvent() {
        return shadowsDescentEvent;
    }

    public TempestsFuryEvent getTempestsFuryEvent() {
        return tempestsFuryEvent;
    }

    public List<com.aspectsmp.event.BaseEvent> getActiveEvents() {
        List<com.aspectsmp.event.BaseEvent> active = new ArrayList<>();
        if (ninthSkyEvent != null && ninthSkyEvent.isActive()) active.add(ninthSkyEvent);
        if (frozenEclipseEvent != null && frozenEclipseEvent.isActive()) active.add(frozenEclipseEvent);
        if (shadowsDescentEvent != null && shadowsDescentEvent.isActive()) active.add(shadowsDescentEvent);
        if (tempestsFuryEvent != null && tempestsFuryEvent.isActive()) active.add(tempestsFuryEvent);
        return active;
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

    public void startShadowsDescentEvent() {
        if (shadowsDescentEvent == null || !shadowsDescentEvent.isActive()) {
            shadowsDescentEvent = new ShadowsDescentEvent(plugin);
            shadowsDescentEvent.startEvent();
        }
    }

    public void startTempestsFuryEvent() {
        if (tempestsFuryEvent == null || !tempestsFuryEvent.isActive()) {
            tempestsFuryEvent = new TempestsFuryEvent(plugin);
            tempestsFuryEvent.startEvent();
        }
    }

    public void stopActiveEvents() {
        if (ninthSkyEvent != null && ninthSkyEvent.isActive()) {
            ninthSkyEvent.stopEvent();
        }
        if (frozenEclipseEvent != null && frozenEclipseEvent.isActive()) {
            frozenEclipseEvent.stopEvent();
        }
        if (shadowsDescentEvent != null && shadowsDescentEvent.isActive()) {
            shadowsDescentEvent.stopEvent();
        }
        if (tempestsFuryEvent != null && tempestsFuryEvent.isActive()) {
            tempestsFuryEvent.stopEvent();
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
            sender.sendMessage("§eEvents: the_awakening, the_frozen_eclipse, shadows_descent, tempests_fury");
            return true;
        }
        String eventName = String.join("_", args).toLowerCase();
        if (eventName.equals("the_awakening") || eventName.equals("awakening") || eventName.equals("ninth_sky")) {
            startNinthSkyEvent();
            sender.sendMessage("§aStarted The Awakening of the Ninth Sky event!");
        } else if (eventName.equals("the_frozen_eclipse") || eventName.equals("frozen_eclipse") || eventName.equals("eclipse")) {
            startFrozenEclipseEvent();
            sender.sendMessage("§aStarted The Frozen Eclipse event!");
        } else if (eventName.equals("shadows_descent") || eventName.equals("shadow_descent") || eventName.equals("shadow")) {
            startShadowsDescentEvent();
            sender.sendMessage("§aStarted The Shadow's Descent event!");
        } else if (eventName.equals("tempests_fury") || eventName.equals("tempest_fury") || eventName.equals("storm")) {
            startTempestsFuryEvent();
            sender.sendMessage("§aStarted The Tempest's Fury event!");
        } else if (eventName.equals("stop")) {
            stopActiveEvents();
            sender.sendMessage("§cStopped all active events.");
        } else {
            sender.sendMessage("§cUnknown event: " + eventName);
            sender.sendMessage("§eAvailable: the_awakening, the_frozen_eclipse, shadows_descent, tempests_fury, stop");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("the_awakening");
            completions.add("the_frozen_eclipse");
            completions.add("shadows_descent");
            completions.add("tempests_fury");
            completions.add("stop");
        }
        return completions;
    }
}