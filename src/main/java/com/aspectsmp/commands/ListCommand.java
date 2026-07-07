package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public ListCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§bAvailable Aspects:");
        for (AspectType aspect : AspectType.values()) {
            sender.sendMessage(aspect.getEmoji() + " " + aspect.getDisplayName());
        }
        return true;
    }
}
