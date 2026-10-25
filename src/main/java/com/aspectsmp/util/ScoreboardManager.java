package com.aspectsmp.util;

import com.aspectsmp.abilities.Ability;
import com.aspectsmp.abilities.AbilityManager;
import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.core.StabilityState;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final AspectSMP plugin;
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerEntries = new ConcurrentHashMap<>();

    // Small caps character mapping
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private static final String REGULAR = "abcdefghijklmnopqrstuvwxyz";

    public ScoreboardManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void onPlayerJoin(Player player) {
        createScoreboard(player);
    }

    public void onPlayerQuit(Player player) {
        removeScoreboard(player);
    }

    public void updateAll() {
        playerScoreboards.keySet().removeIf(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                playerScoreboards.remove(uuid);
                playerEntries.remove(uuid);
                return true;
            }
            updatePlayer(player);
            return false;
        });
    }

    public void updateActionBar(Player player) {
        if (!player.isOnline()) return;
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;

        StringBuilder actionBar = new StringBuilder();
        actionBar.append(getAspectColor(heart.getAspect())).append("§l").append(heart.getAspect().getDisplayName()).append(" §7| §6Tier ").append(heart.getTier()).append(" §7| ");

        Set<Ability> abilities = plugin.getAbilityManager().getAbilitiesForAspect(heart.getAspect());
        boolean first = true;
        for (Ability ability : abilities) {
            if (ability.isPassive()) continue;
            if (ability.getTier() > heart.getTier()) continue;
            long cooldownEnd = heart.getCooldowns().getOrDefault(ability.getId(), 0L);
            String status;
            if (cooldownEnd > System.currentTimeMillis()) {
                long seconds = Math.max(1, (cooldownEnd - System.currentTimeMillis() + 999) / 1000);
                status = "§c" + seconds + "s";
            } else {
                status = "§aREADY";
            }
            if (!first) actionBar.append(" §8| ");
            first = false;
            actionBar.append(toSmallCaps(ability.getName())).append(": ").append(status);
        }

        player.sendActionBar(net.kyori.adventure.text.Component.text(actionBar.toString()));
    }

    private void createScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerScoreboards.containsKey(uuid)) return;

        org.bukkit.scoreboard.ScoreboardManager manager = player.getServer().getScoreboardManager();
        if (manager == null) return;

        org.bukkit.scoreboard.Scoreboard scoreboard = manager.getNewScoreboard();
        player.setScoreboard(scoreboard);
        playerScoreboards.put(uuid, scoreboard);
        playerEntries.put(uuid, ConcurrentHashMap.newKeySet());
    }

    private void removeScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        playerScoreboards.remove(uuid);
        playerEntries.remove(uuid);
        if (player.isOnline()) {
            org.bukkit.scoreboard.ScoreboardManager manager = player.getServer().getScoreboardManager();
            if (manager != null) {
                player.setScoreboard(manager.getNewScoreboard());
            }
        }
    }

    public void updatePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        org.bukkit.scoreboard.Scoreboard scoreboard = playerScoreboards.get(uuid);
        if (scoreboard == null) return;

        Heart heart = plugin.getHeartManager().getHeart(uuid).orElse(null);
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

        Set<String> oldEntries = playerEntries.get(uuid);
        if (oldEntries == null) {
            oldEntries = ConcurrentHashMap.newKeySet();
            playerEntries.put(uuid, oldEntries);
        }
        for (String entry : oldEntries) {
            Score score = obj.getScore(entry);
            if (score != null) {
                score.setScore(0);
            }
        }
        oldEntries.clear();

        int scoreValue = 6;
        String tierLine = ChatColor.GOLD + "Tier: " + heart.getTier();
        String stabilityLine = getStabilityColor(heart) + getStabilityStateDisplay(heart) + ": " + heart.getStability();
        String essenceLine = ChatColor.AQUA + "Essence: " + heart.getEssence();

        obj.getScore(tierLine).setScore(scoreValue--);
        obj.getScore(stabilityLine).setScore(scoreValue--);
        obj.getScore(essenceLine).setScore(scoreValue--);

        oldEntries.add(tierLine);
        oldEntries.add(stabilityLine);
        oldEntries.add(essenceLine);
    }

    private String getStabilityStateDisplay(Heart heart) {
        return heart.getStabilityState().getDisplayName();
    }

    private ChatColor getStabilityColor(Heart heart) {
        return switch (heart.getStabilityState()) {
            case AWAKENED -> ChatColor.GREEN;
            case STABLE -> ChatColor.DARK_GREEN;
            case FRACTURED -> ChatColor.YELLOW;
            case FADED -> ChatColor.GOLD;
            case CRITICAL -> ChatColor.RED;
            case DORMANT -> ChatColor.DARK_GRAY;
        };
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
            case CLOUD -> ChatColor.AQUA;
            case WINTER -> ChatColor.DARK_AQUA;
            case SHADOW -> ChatColor.DARK_GRAY;
            case STORM -> ChatColor.GRAY;
        };
    }

    private String toSmallCaps(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            int index = REGULAR.indexOf(c);
            if (index >= 0) {
                result.append(SMALL_CAPS.charAt(index));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
