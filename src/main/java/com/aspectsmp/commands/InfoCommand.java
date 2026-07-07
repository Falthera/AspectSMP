package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public InfoCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
        if (!(sender instanceof Player player) && target == null) {
            sender.sendMessage("§cUsage: /info [player]");
            return true;
        }

        Player subject = target != null ? target : (Player) sender;
        Heart heart = plugin.getHeartManager().getHeart(subject);

        if (heart == null) {
            sender.sendMessage("§cPlayer has no Heart.");
            return true;
        }

        sender.sendMessage("§d=== " + subject.getName() + "'s Heart ===");
        sender.sendMessage("§bAspect: " + heart.getAspect().getDisplayName());
        sender.sendMessage("§6Tier: " + heart.getTier());
        sender.sendMessage("§cStability: " + heart.getStability());
        sender.sendMessage("§eEssence: " + heart.getEssence());
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
