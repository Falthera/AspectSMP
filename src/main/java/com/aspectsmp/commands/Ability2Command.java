package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Ability2Command implements CommandExecutor {

    private final AspectSMP plugin;

    public Ability2Command(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this.");
            return true;
        }
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant() || heart.getTier() < 2) {
            player.sendMessage("§cYou need Tier 2 to use this ability!");
            return true;
        }
        plugin.getAbilityManager().handleTier2Ability(player, heart);
        return true;
    }
}
