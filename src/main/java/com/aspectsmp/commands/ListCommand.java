package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public ListCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.list")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        sender.sendMessage("§bAvailable Aspects:");
        for (AspectType aspect : AspectType.values()) {
            sender.sendMessage(aspect.getEmoji() + " " + aspect.getDisplayName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            for (AspectType aspect : AspectType.values()) {
                String name = aspect.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(input)) {
                    completions.add(aspect.name());
                }
            }
        }
        return completions;
    }
}
