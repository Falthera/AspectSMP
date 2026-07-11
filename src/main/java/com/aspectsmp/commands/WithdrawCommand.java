package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;
import com.aspectsmp.items.StabilityBottle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WithdrawCommand implements CommandExecutor {

    private final AspectSMP plugin;

    public WithdrawCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("stability")) {
            player.sendMessage("§cUsage: /withdraw stability");
            return true;
        }

        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) {
            player.sendMessage("§cYou do not have a Heart.");
            return true;
        }

        int withdrawAmount = 10;
        if (heart.getStability() < withdrawAmount) {
            player.sendMessage("§cYou need at least " + withdrawAmount + " Stability to withdraw.");
            return true;
        }

        heart.addStability(-withdrawAmount);
        plugin.getHeartManager().saveHeart(heart);

        ItemStack bottle = new StabilityBottle().createItemStack(withdrawAmount);
        player.getInventory().addItem(bottle);
        player.sendMessage("§aWithdrew §b" + withdrawAmount + " Stability §ainto a bottle.");
        plugin.getScoreboardManager().updatePlayer(player);

        return true;
    }
}
