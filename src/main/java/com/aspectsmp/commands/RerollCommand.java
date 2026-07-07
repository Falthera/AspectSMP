package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
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
        var aspects = java.util.List.of(com.aspectsmp.core.AspectType.values());
        com.aspectsmp.core.AspectType newAspect;
        do {
            newAspect = aspects.get(new java.util.Random().nextInt(aspects.size()));
        } while (newAspect == heart.getAspect() && aspects.size() > 1);

        heart.setAspect(newAspect);
        plugin.getHeartManager().saveHeart(heart);

        if (target.isOnline()) {
            HeartOfTheSea.ensureBound(target);
            target.sendMessage("§eYour Aspect has been rerolled to " + newAspect.getDisplayName());
        }

        sender.sendMessage("§eRerolled " + target.getName() + "'s Aspect to " + newAspect.getDisplayName());
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
