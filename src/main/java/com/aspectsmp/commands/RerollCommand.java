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

public class RerollCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public RerollCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        var heartOptional = plugin.getHeartManager().getHeart(target.getUniqueId());
        if (heartOptional.isEmpty()) {
            sender.sendMessage("§cPlayer has no Heart.");
            return true;
        }

        Heart heart = heartOptional.get();
        
        // Filter out event aspects
        var aspects = java.util.List.of(com.aspectsmp.core.AspectType.values()).stream()
                .filter(a -> !a.isEventAspect())
                .toList();
                
        AspectType current = heart.getAspect();
        com.aspectsmp.core.AspectType newAspect;
        do {
            newAspect = aspects.get(new java.util.Random().nextInt(aspects.size()));
        } while (newAspect == current && aspects.size() > 1);

        AspectType oldAspect = heart.getAspect();
        heart.setAspect(newAspect);
        heart.getCooldowns().clear();
        plugin.getListenerManager().clearAspectEffects(target);
        plugin.getHeartManager().saveHeart(heart);

        if (target.isOnline()) {
            // Update visual effects - stop old aura, start new one
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
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }
        return completions;
    }
}
