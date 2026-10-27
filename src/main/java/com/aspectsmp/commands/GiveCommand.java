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
import java.util.concurrent.ConcurrentHashMap;

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000L; // 5 seconds

    public GiveCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
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

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /aspect-give <player> <catalyst|resonator|restoration|reforging|stability>");
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
            case "stability" -> new StabilityBottle().createItemStack();
            default -> null;
        };

        if (item == null) {
            sender.sendMessage("§cUsage: /aspect-give <player> <catalyst|resonator|restoration|reforging|stability>");
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
            String input = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(input)) {
                    completions.add(name);
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            for (String item : List.of("catalyst", "resonator", "restoration", "reforging", "stability")) {
                if (item.startsWith(input)) {
                    completions.add(item);
                }
            }
        }
        return completions;
    }
}
