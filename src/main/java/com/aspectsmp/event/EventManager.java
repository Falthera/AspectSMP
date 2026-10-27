package com.aspectsmp.event;

import com.aspectsmp.AspectSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class EventManager implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final ConcurrentHashMap<String, BaseEvent> activeEvents = new ConcurrentHashMap<>();
    private final AtomicReference<BaseEvent> currentEvent = new AtomicReference<>();
    private final AtomicLong globalCooldownEnd = new AtomicLong(0);
    private static final long DEFAULT_COOLDOWN_MS = 30 * 60 * 1000; // 30 minutes

    public EventManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public NinthSkyEvent getNinthSkyEvent() {
        return (NinthSkyEvent) activeEvents.get("ninth_sky");
    }

    public FrozenEclipseEvent getFrozenEclipseEvent() {
        return (FrozenEclipseEvent) activeEvents.get("frozen_eclipse");
    }

    public ShadowsDescentEvent getShadowsDescentEvent() {
        return (ShadowsDescentEvent) activeEvents.get("shadows_descent");
    }

    public TempestsFuryEvent getTempestsFuryEvent() {
        return (TempestsFuryEvent) activeEvents.get("tempests_fury");
    }

    public List<BaseEvent> getActiveEvents() {
        return new ArrayList<>(activeEvents.values());
    }

    public boolean isAnyEventActive() {
        return currentEvent.get() != null;
    }

    public BaseEvent getCurrentEvent() {
        return currentEvent.get();
    }

    public long getGlobalCooldownRemaining() {
        long remaining = globalCooldownEnd.get() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public boolean isGlobalCooldownActive() {
        return getGlobalCooldownRemaining() > 0;
    }

    private void setGlobalCooldown() {
        long cooldownMs = plugin.getConfig().getLong("events.global-cooldown-minutes", 30) * 60 * 1000;
        globalCooldownEnd.set(System.currentTimeMillis() + cooldownMs);
    }

    public boolean startNinthSkyEvent() {
        if (!tryStartEvent("ninth_sky")) return false;
        NinthSkyEvent event = new NinthSkyEvent(plugin);
        if (!currentEvent.compareAndSet(null, event)) return false;
        activeEvents.put("ninth_sky", event);
        event.startEvent();
        return true;
    }

    public boolean startFrozenEclipseEvent() {
        if (!tryStartEvent("frozen_eclipse")) return false;
        FrozenEclipseEvent event = new FrozenEclipseEvent(plugin);
        if (!currentEvent.compareAndSet(null, event)) return false;
        activeEvents.put("frozen_eclipse", event);
        event.startEvent();
        return true;
    }

    public boolean startShadowsDescentEvent() {
        if (!tryStartEvent("shadows_descent")) return false;
        ShadowsDescentEvent event = new ShadowsDescentEvent(plugin);
        if (!currentEvent.compareAndSet(null, event)) return false;
        activeEvents.put("shadows_descent", event);
        event.startEvent();
        return true;
    }

    public boolean startTempestsFuryEvent() {
        if (!tryStartEvent("tempests_fury")) return false;
        TempestsFuryEvent event = new TempestsFuryEvent(plugin);
        if (!currentEvent.compareAndSet(null, event)) return false;
        activeEvents.put("tempests_fury", event);
        event.startEvent();
        return true;
    }

    private boolean tryStartEvent(String eventName) {
        if (currentEvent.get() != null) {
            return false;
        }
        if (isGlobalCooldownActive()) {
            return false;
        }
        if (!activeEvents.isEmpty()) {
            return false;
        }
        // Note: We don't set currentEvent here; the specific start method will do it after creating the event
        return true;
    }

    public void stopActiveEvents() {
        List<BaseEvent> events = new ArrayList<>(activeEvents.values());
        for (BaseEvent event : events) {
            if (event != null && event.isActive()) {
                event.stopEvent();
            }
        }
        activeEvents.clear();
        currentEvent.set(null);
        setGlobalCooldown();
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
            if (startNinthSkyEvent()) {
                sender.sendMessage("§aStarted The Awakening of the Ninth Sky event!");
            } else {
                sender.sendMessage("§cAnother event is active or global cooldown is in effect.");
            }
        } else if (eventName.equals("the_frozen_eclipse") || eventName.equals("frozen_eclipse") || eventName.equals("eclipse")) {
            if (startFrozenEclipseEvent()) {
                sender.sendMessage("§aStarted The Frozen Eclipse event!");
            } else {
                sender.sendMessage("§cAnother event is active or global cooldown is in effect.");
            }
        } else if (eventName.equals("shadows_descent") || eventName.equals("shadow_descent") || eventName.equals("shadow")) {
            if (startShadowsDescentEvent()) {
                sender.sendMessage("§aStarted The Shadow's Descent event!");
            } else {
                sender.sendMessage("§cAnother event is active or global cooldown is in effect.");
            }
        } else if (eventName.equals("tempests_fury") || eventName.equals("tempest_fury") || eventName.equals("storm")) {
            if (startTempestsFuryEvent()) {
                sender.sendMessage("§aStarted The Tempest's Fury event!");
            } else {
                sender.sendMessage("§cAnother event is active or global cooldown is in effect.");
            }
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

    public void shutdown() {
        stopActiveEvents();
        if (plugin.getVisualEffectsManager() != null) {
            plugin.getVisualEffectsManager().shutdown();
        }
    }
}