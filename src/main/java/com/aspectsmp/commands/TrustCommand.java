package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.trust.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TrustCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final TrustManager trustManager;

    public TrustCommand(AspectSMP plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /trust <add|remove|clear|list> [name]");
            return true;
        }

        UUID uid = player.getUniqueId();

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(player, uid, args);
            case "remove" -> handleRemove(player, uid, args);
            case "clear" -> handleClear(player, uid);
            case "list" -> handleList(player, uid);
            default -> sender.sendMessage("§cUsage: /trust <add|remove|clear|list> [name]");
        }
        return true;
    }

    private void handleAdd(Player player, UUID uid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /trust add <name>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (target.getUniqueId().equals(uid)) {
            player.sendMessage("§cYou cannot trust yourself.");
            return;
        }

        trustManager.trust(uid, target.getUniqueId());
        player.sendMessage("§aTrusted §f" + target.getName() + "§a.");
        if (target.isOnline()) {
            target.sendMessage("§aYou have been trusted by §f" + player.getName() + "§a.");
        }
    }

    private void handleRemove(Player player, UUID uid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /trust remove <name>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (!trustManager.isTrusted(uid, target.getUniqueId())) {
            player.sendMessage("§cYou have not trusted §f" + target.getName() + "§c.");
            return;
        }

        trustManager.untrust(uid, target.getUniqueId());
        player.sendMessage("§aRemoved trust for §f" + target.getName() + "§a.");
        if (target.isOnline()) {
            target.sendMessage("§aYou are no longer trusted by §f" + player.getName() + "§a.");
        }
    }

    private void handleClear(Player player, UUID uid) {
        trustManager.clear(uid);
        player.sendMessage("§aCleared all trusted players.");
    }

    private void handleList(Player player, UUID uid) {
        Set<UUID> trusted = trustManager.getTrusted(uid);
        if (trusted.isEmpty()) {
            player.sendMessage("§7You have no trusted players.");
            return;
        }

        player.sendMessage("§bTrusted Players:");
        for (UUID uuid : trusted) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = "Unknown";
            boolean online = Bukkit.getPlayer(uuid) != null;
            player.sendMessage("§f- " + name + (online ? " §a(online)" : " §7(offline)"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(List.of("add", "remove", "clear", "list"));
        } else if (args.length == 2 && ("add".equals(args[0].toLowerCase(Locale.ROOT)) || "remove".equals(args[0].toLowerCase(Locale.ROOT)))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getUniqueId().equals(((Player) sender).getUniqueId())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
