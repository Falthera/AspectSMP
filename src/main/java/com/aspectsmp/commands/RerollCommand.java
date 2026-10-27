package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.items.HeartOfTheSea;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RerollCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000L; // 5 seconds

    public RerollCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        String senderName = sender.getName();
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(senderName);
        if (lastUse != null && now - lastUse < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastUse)) / 1000;
            sender.sendMessage("§cCommand on cooldown. Wait " + remaining + "s.");
            return true;
        }
        cooldowns.put(senderName, now);

        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        Heart heart = plugin.getHeartManager().getHeart(target.getUniqueId()).orElse(null);
        if (heart == null) {
            sender.sendMessage("§cPlayer has no Heart.");
            return true;
        }

        // Filter out event aspects
        var aspects = java.util.List.of(com.aspectsmp.core.AspectType.values()).stream()
                .filter(a -> !a.isEventAspect())
                .toList();

        AspectType current = heart.getAspect();
        com.aspectsmp.core.AspectType newAspect;
        do {
            newAspect = aspects.get(ThreadLocalRandom.current().nextInt(aspects.size()));
        } while (newAspect == current && aspects.size() > 1);

        AspectType oldAspect = heart.getAspect();
        heart.setAspect(newAspect);
        heart.getCooldowns().clear();
        plugin.getListenerManager().clearAspectEffects(target);
        plugin.getHeartManager().saveHeart(heart);

        if (target.isOnline()) {
            plugin.getVisualEffectsManager().stopPassiveEffects(target);
            HeartOfTheSea.ensureBound(target);
            plugin.getVisualEffectsManager().startPassiveEffects(target);

            target.sendMessage("§eYour Aspect has been rerolled to " + newAspect.getDisplayName());
        }

        sender.sendMessage("§eRerolled " + target.getName() + "'s Aspect from " + oldAspect.getDisplayName() + " to " + newAspect.getDisplayName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase(java.util.Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name.toLowerCase(java.util.Locale.ROOT).startsWith(input)) {
                    completions.add(name);
                }
            }
        }
        return completions;
    }
}
