package com.aspectsmp.util;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ScoreboardManager {

    private final AspectSMP plugin;
    private final java.util.Map<Player, org.bukkit.scoreboard.Scoreboard> playerScoreboards;
    private final java.util.Map<Player, Set<String>> playerEntries;

    public ScoreboardManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new java.util.HashMap<>();
        this.playerEntries = new java.util.HashMap<>();
    }

    public void onPlayerJoin(Player player) {
        createScoreboard(player);
    }

    public void onPlayerQuit(Player player) {
        removeScoreboard(player);
    }

    public void updateAll() {
        playerScoreboards.keySet().removeIf(player -> {
            if (!player.isOnline()) {
                playerScoreboards.remove(player);
                playerEntries.remove(player);
                return true;
            }
            updatePlayer(player);
            return false;
        });
    }

    private void createScoreboard(Player player) {
        if (playerScoreboards.containsKey(player)) return;

        ScoreboardManager manager = player.getServer().getScoreboardManager();
        if (manager == null) return;

        org.bukkit.scoreboard.Scoreboard scoreboard = manager.getNewScoreboard();
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
        playerEntries.put(player, new HashSet<>());
    }

    private void removeScoreboard(Player player) {
        playerScoreboards.remove(player);
        playerEntries.remove(player);
        if (player.isOnline()) {
            ScoreboardManager manager = player.getServer().getScoreboardManager();
            if (manager != null) {
                player.setScoreboard(manager.getNewScoreboard());
            }
        }
    }

    private void updatePlayer(Player player) {
        org.bukkit.scoreboard.Scoreboard scoreboard = playerScoreboards.get(player);
        if (scoreboard == null) return;

        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) {
            Objective obj = scoreboard.getObjective("aspectsmp");
            if (obj != null) obj.unregister();
            return;
        }

        AspectType aspect = heart.getAspect();
        ChatColor color = getAspectColor(aspect);

        Objective obj = scoreboard.getObjective("aspectsmp");
        if (obj == null) {
            obj = scoreboard.registerNewObjective("aspectsmp", "dummy", Component.text(color + "" + ChatColor.BOLD + aspect.getDisplayName() + " Heart"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        Set<String> oldEntries = playerEntries.get(player);
        if (oldEntries == null) {
            oldEntries = new HashSet<>();
            playerEntries.put(player, oldEntries);
        }
        for (String entry : oldEntries) {
            Score score = obj.getScore(entry);
            if (score != null) {
                score.setScore(0);
            }
        }
        oldEntries.clear();

        String tierLine = color + "Tier: " + heart.getTier();
        String stabilityLine = ChatColor.RED + "Stability: " + heart.getStability();
        String essenceLine = ChatColor.AQUA + "Essence: " + heart.getEssence();

        obj.getScore(tierLine).setScore(3);
        obj.getScore(stabilityLine).setScore(2);
        obj.getScore(essenceLine).setScore(1);

        oldEntries.add(tierLine);
        oldEntries.add(stabilityLine);
        oldEntries.add(essenceLine);
    }

    private ChatColor getAspectColor(AspectType aspect) {
        return switch (aspect) {
            case INFERNO -> ChatColor.RED;
            case TIDE -> ChatColor.BLUE;
            case TEMPEST -> ChatColor.YELLOW;
            case RIFT -> ChatColor.DARK_PURPLE;
            case VITALITY -> ChatColor.GREEN;
            case WAR -> ChatColor.DARK_RED;
            case COSMOS -> ChatColor.LIGHT_PURPLE;
            case FORTUNE -> ChatColor.GOLD;
        };
    }
}
