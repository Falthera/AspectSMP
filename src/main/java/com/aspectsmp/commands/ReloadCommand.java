package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.HeartManager;
import com.aspectsmp.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public ReloadCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        sender.sendMessage("§eReloading Aspect SMP...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.reload(sender);
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§aAspect SMP reloaded successfully!")
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Reload failed: " + e.getMessage());
                e.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§cReload failed: " + e.getMessage())
                );
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}