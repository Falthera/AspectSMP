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
import java.util.concurrent.ConcurrentHashMap;

public class GiveEventAspectCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000L; // 5 seconds

    public GiveEventAspectCommand(AspectSMP plugin) {
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
            sender.sendMessage("§cUsage: /give-event-aspect <player> <winter|shadow|storm|cloud>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        String aspectName = args[1].toLowerCase(Locale.ROOT);
        AspectType aspect;

        switch (aspectName) {
            case "winter" -> aspect = AspectType.WINTER;
            case "shadow" -> aspect = AspectType.SHADOW;
            case "storm" -> aspect = AspectType.STORM;
            case "cloud" -> aspect = AspectType.CLOUD;
            default -> {
                sender.sendMessage("§cUsage: /give-event-aspect <player> <winter|shadow|storm|cloud>");
                return true;
            }
        }

        Heart heart = plugin.getHeartManager().getHeart(target.getUniqueId()).orElse(null);
        if (heart == null) {
            sender.sendMessage("§cPlayer does not have a Heart.");
            return true;
        }

        heart.setAspect(aspect);
        if (aspect == AspectType.WINTER) heart.setWinterUnlocked(true);
        if (aspect == AspectType.CLOUD) heart.setCloudUnlocked(true);
        if (aspect == AspectType.SHADOW) heart.setCloudUnlocked(true);
        if (aspect == AspectType.STORM) heart.setWinterUnlocked(true);
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
            String input = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(input)) {
                    completions.add(name);
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            for (String aspect : List.of("winter", "shadow", "storm", "cloud")) {
                if (aspect.startsWith(input)) {
                    completions.add(aspect);
                }
            }
        }
        return completions;
    }
}
