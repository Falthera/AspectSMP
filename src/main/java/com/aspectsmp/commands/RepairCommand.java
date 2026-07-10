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

public class RepairCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public RepairCommand(AspectSMP plugin) {
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
        heart.setDormant(false);
        heart.setStability(50);
        plugin.getHeartManager().saveHeart(heart);

        sender.sendMessage("§aRepaired " + target.getName() + "'s Heart");
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
