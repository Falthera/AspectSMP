package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
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

public class GiveEventAspectCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public GiveEventAspectCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /give-event-aspect <player> <winter>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        String aspectName = args[1].toLowerCase(Locale.ROOT);
        AspectType aspect;

        if (aspectName.equals("winter")) {
            aspect = AspectType.WINTER;
        } else {
            sender.sendMessage("§cUsage: /give-event-aspect <player> <winter>");
            return true;
        }

        Heart heart = plugin.getHeartManager().getHeart(target.getUniqueId()).orElse(null);
        if (heart == null) {
            sender.sendMessage("§cPlayer does not have a Heart.");
            return true;
        }

        heart.setAspect(aspect);
        heart.setWinterUnlocked(aspect == AspectType.WINTER);
        heart.setTier(1);
        heart.setStability(100);
        heart.setDormant(false);
        heart.getCooldowns().clear();

        plugin.getHeartManager().saveHeart(heart);

        sender.sendMessage("§aGave " + aspect.getDisplayName() + " " + aspect.getEmoji() + " to " + target.getName());
        target.sendMessage("§aYou have received the " + aspect.getDisplayName() + " " + aspect.getEmoji() + "!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 2) {
            completions.add("winter");
        }
        return completions;
    }
}
