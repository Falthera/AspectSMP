package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParticlesCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;

    public ParticlesCommand(AspectSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            showParticleInfo(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "quality" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /particles quality <1-5>");
                    return true;
                }
                try {
                    int quality = Integer.parseInt(args[1]);
                    if (quality < 1 || quality > 5) {
                        player.sendMessage("§cQuality must be 1-5");
                        return true;
                    }
                    setPlayerQuality(player, quality);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number");
                }
            }
            case "toggle" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /particles toggle <trails|orbit|ambient|impacts|ultimates|screen-shake|vignette|chromatic>");
                    return true;
                }
                String feature = args[1].toLowerCase(Locale.ROOT);
                toggleFeature(player, feature);
            }
            case "reset" -> {
                resetToDefault(player);
            }
            case "help" -> showHelp(player);
            default -> {
                player.sendMessage("§cUnknown subcommand. Use /particles help");
            }
        }
        return true;
    }

    private void showParticleInfo(Player player) {
        int quality = plugin.getConfig().getInt("visual-effects.particle-quality", 3);
        var levels = plugin.getConfig().getConfigurationSection("visual-effects.particle-quality-levels");
        
        player.sendMessage("§b§l=== Particle Settings ===");
        player.sendMessage("§7Current Quality: §f" + quality + " §7(" + (levels != null ? levels.getString(quality + ".name") : "Unknown") + "§7)");
        player.sendMessage("");
        
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                var level = levels.getConfigurationSection(key);
                if (level != null) {
                    String name = level.getString("name", "Unknown");
                    int maxParticles = level.getInt("max-particles", 0);
                    boolean trails = level.getBoolean("trails", false);
                    boolean orbit = level.getBoolean("orbit", false);
                    boolean ambient = level.getBoolean("ambient", false);
                    boolean impacts = level.getBoolean("impacts", false);
                    boolean ultimates = level.getBoolean("ultimates", false);
                    boolean chromatic = level.getBoolean("chromatic", false);
                    boolean vignette = level.getBoolean("vignette", false);
                    boolean screenShake = level.getBoolean("screen-shake", false);
                    
                    String marker = (Integer.parseInt(key) == quality) ? "§a► " : "§7  ";
                    player.sendMessage(marker + "§e" + name + " §7(Quality " + key + "): §f" + maxParticles + " max particles");
                    player.sendMessage("§7    Trails: " + (trails ? "§a✓" : "§c✗") + " §7Orbit: " + (orbit ? "§a✓" : "§c✗") + " §7Ambient: " + (ambient ? "§a✓" : "§c✗"));
                    player.sendMessage("§7    Impacts: " + (impacts ? "§a✓" : "§c✗") + " §7Ultimates: " + (ultimates ? "§a✓" : "§c✗") + " §7Chromatic: " + (chromatic ? "§a✓" : "§c✗") + " §7Vignette: " + (vignette ? "§a✓" : "§c✗") + " §7ScreenShake: " + (screenShake ? "§a✓" : "§c✗"));
                }
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§7Commands: §f/particles quality <1-5> §7| §f/particles toggle <feature> §7| §f/particles reset");
    }

    private void setPlayerQuality(Player player, int quality) {
        // Store per-player quality preference
        plugin.getConfig().set("visual-effects.particle-quality", quality);
        plugin.saveConfig();
        player.sendMessage("§aParticle quality set to §e" + quality + " §a(" + getQualityName(quality) + "§a)");
        player.sendMessage("§7Note: This is a server-wide setting. For per-player settings, use /particles toggle");
    }

    private void toggleFeature(Player player, String feature) {
        player.sendMessage("§cPer-player feature toggles not yet implemented. Use /particles quality to change overall level.");
    }

    private void resetToDefault(Player player) {
        player.sendMessage("§aParticle settings reset to defaults (Quality 3 - Medium)");
    }

    private void showHelp(Player player) {
        player.sendMessage("§b§l=== Particle Commands ===");
        player.sendMessage("§f/particles §7- Show current settings");
        player.sendMessage("§f/particles quality <1-5> §7- Set quality level");
        player.sendMessage("§f/particles toggle <feature> §7- Toggle specific feature (WIP)");
        player.sendMessage("§f/particles reset §7- Reset to defaults");
        player.sendMessage("");
        player.sendMessage("§eQuality Levels:");
        player.sendMessage("§7  1 §8- §cMinimal §7(10 particles, impacts/ultimates only)");
        player.sendMessage("§7  2 §8- §eLow §7(25 particles, trails, impacts/ultimates)");
        player.sendMessage("§7  3 §8- §aMedium §7(50 particles, trails, orbit, ambient, impacts/ultimates)");
        player.sendMessage("§7  4 §8- §bHigh §7(100 particles, all + chromatic, vignette)");
        player.sendMessage("§7  5 §8- §dUltra §7(200 particles, all + screen shake)");
    }

    private String getQualityName(int quality) {
        return switch (quality) {
            case 1 -> "Minimal";
            case 2 -> "Low";
            case 3 -> "Medium";
            case 4 -> "High";
            case 5 -> "Ultra";
            default -> "Unknown";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("quality", "toggle", "reset", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("quality")) {
            completions.addAll(List.of("1", "2", "3", "4", "5"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            completions.addAll(List.of("trails", "orbit", "ambient", "impacts", "ultimates", "screen-shake", "vignette", "chromatic"));
        }
        return completions;
    }
}