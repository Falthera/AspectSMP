package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.items.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public GiveCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /aspect-give <player> <catalyst|resonator|restoration|reforging>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        String itemType = args[1].toLowerCase(Locale.ROOT);
        ItemStack item = switch (itemType) {
            case "catalyst" -> new HeartCatalyst().createItemStack();
            case "resonator" -> new HeartResonator().createItemStack();
            case "restoration" -> new HeartRestorationKit().createItemStack();
            case "reforging" -> new ReforgingCore().createItemStack();
            default -> null;
        };

        if (item == null) {
            sender.sendMessage("§cUsage: /aspect-give <player> <catalyst|resonator|restoration|reforging>");
            return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aGave " + itemType + " to " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 2) {
            completions.addAll(List.of("catalyst", "resonator", "restoration", "reforging"));
        }
        return completions;
    }
}
