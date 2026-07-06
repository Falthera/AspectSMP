package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
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

public class CommandManager implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public CommandManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        plugin.getCommand("aspect").setExecutor(this);
        plugin.getCommand("aspect").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender);
            case "give" -> handleGive(sender, args);
            case "reroll" -> handleReroll(sender, args);
            case "repair" -> handleRepair(sender, args);
            case "reload" -> handleReload(sender);
            case "gui" -> handleGui(sender);
            case "ability3" -> handleAbility3(sender);
            case "ultimate" -> handleUltimate(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
        if (!(sender instanceof Player player) && target == null) {
            sender.sendMessage("§cUsage: /aspect info [player]");
            return;
        }
        
        Player subject = target != null ? target : (Player) sender;
        Heart heart = plugin.getHeartManager().getHeart(subject);
        
        if (heart == null) {
            sender.sendMessage("§cPlayer has no Heart.");
            return;
        }
        
        sender.sendMessage("§d=== " + subject.getName() + "'s Heart ===");
        sender.sendMessage("§bAspect: " + heart.getAspect().getDisplayName());
        sender.sendMessage("§6Tier: " + heart.getTier());
        sender.sendMessage("§cStability: " + heart.getStability());
        sender.sendMessage("§eEssence: " + heart.getEssence());
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage("§bAvailable Aspects:");
        for (AspectType aspect : AspectType.values()) {
            sender.sendMessage(aspect.getEmoji() + " " + aspect.getDisplayName());
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        
        String itemType = args.length > 2 ? args[2] : "";
        ItemStack item = switch (itemType) {
            case "catalyst" -> new HeartCatalyst().createItemStack();
            case "resonator" -> new HeartResonator().createItemStack();
            case "restoration" -> new HeartRestorationKit().createItemStack();
            case "reforging" -> new ReforgingCore().createItemStack();
            default -> {
                sender.sendMessage("§cUsage: /aspect give <player> <catalyst|resonator|restoration|reforging>");
                yield null;
            }
        };
        
        if (item != null) {
            target.getInventory().addItem(item);
            sender.sendMessage("§aGave " + itemType + " to " + target.getName());
        }
    }

    private void handleReroll(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        
        java.util.Optional<Heart> heartOptional = plugin.getHeartManager().getHeart(target.getUniqueId());
        if (heartOptional.isEmpty()) {
            sender.sendMessage("§cPlayer has no Heart.");
            return;
        }
        
        Heart heart = heartOptional.get();
        AspectType newAspect;
        do {
            newAspect = AspectType.values()[(int) (Math.random() * AspectType.values().length)];
        } while (newAspect == heart.getAspect() && AspectType.values().length > 1);
        
        heart.setAspect(newAspect);
        plugin.getHeartManager().saveHeart(heart);
        
        if (target.isOnline()) {
            HeartOfTheSea.ensureBound(target);
            target.sendMessage("§eYour Aspect has been rerolled to " + newAspect.getDisplayName());
        }
        
        sender.sendMessage("§eRerolled " + target.getName() + "'s Aspect to " + newAspect.getDisplayName());
    }

    private void handleRepair(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : null;
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        
        java.util.Optional<Heart> heartOptional = plugin.getHeartManager().getHeart(target.getUniqueId());
        if (heartOptional.isEmpty()) {
            sender.sendMessage("§cPlayer has no Heart.");
            return;
        }
        
        Heart heart = heartOptional.get();
        heart.setDormant(false);
        heart.setStability(50);
        plugin.getHeartManager().saveHeart(heart);
        
        sender.sendMessage("§aRepaired " + target.getName() + "'s Heart");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage("§aAspect SMP reloaded.");
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this.");
            return;
        }
        plugin.getGuiManager().openAspectListGUI(player);
    }

    private void handleAbility3(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this.");
            return;
        }
        plugin.getAbilityManager().handleTier3Ability(player);
    }

    private void handleUltimate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this.");
            return;
        }
        plugin.getAbilityManager().handleUltimate(player);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§bAspect SMP Commands:");
        sender.sendMessage("§f/aspect info [player] - View Heart info");
        sender.sendMessage("§f/aspect list - List Aspects");
        sender.sendMessage("§f/aspect gui - Open Heart GUI");
        sender.sendMessage("§f/aspect ability3 - Use Tier 3 ability");
        sender.sendMessage("§f/aspect ultimate - Use Ultimate ability");
        if (sender.hasPermission("aspect.admin")) {
            sender.sendMessage("§f/aspect give <player> <item> - Give item");
            sender.sendMessage("§f/aspect reroll <player> - Reroll Aspect");
            sender.sendMessage("§f/aspect repair <player> - Repair Heart");
            sender.sendMessage("§f/aspect reload - Reload plugin");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(List.of("info", "list", "gui", "ability3", "ultimate"));
            if (sender.hasPermission("aspect.admin")) {
                completions.addAll(List.of("give", "reroll", "repair", "reload"));
            }
        } else if (args.length == 2 && sender.hasPermission("aspect.admin") && 
                   List.of("give", "reroll", "repair").contains(args[0].toLowerCase(Locale.ROOT))) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        } else if (args.length == 3 && "give".equals(args[0].toLowerCase(Locale.ROOT))) {
            completions.addAll(List.of("catalyst", "resonator", "restoration", "reforging"));
        }
        
        return completions;
    }
}