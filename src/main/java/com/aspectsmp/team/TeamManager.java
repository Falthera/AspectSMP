package com.aspectsmp.team;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.trust.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {

    private final AspectSMP plugin;
    private final TrustManager trustManager;
    private final File file;
    private FileConfiguration config;
    
    private final Map<UUID, Team> playerTeam = new ConcurrentHashMap<>();
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private static final int MAX_TEAM_SIZE = 6;

    public TeamManager(AspectSMP plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create teams.yml: " + e.getMessage());
            }
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            Team team = Team.load(config.getConfigurationSection(key), plugin);
            if (team != null) {
                teams.put(team.getName().toLowerCase(), team);
                for (UUID member : team.getMembers()) {
                    playerTeam.put(member, team);
                }
            }
        }
    }

    public void save() {
        try {
            config = new YamlConfiguration();
            for (Team team : teams.values()) {
                team.save(config.createSection(team.getName()));
            }
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save teams.yml: " + e.getMessage());
        }
    }

    public boolean createTeam(Player leader, String name) {
        if (playerTeam.containsKey(leader.getUniqueId())) {
            return false;
        }
        if (teams.containsKey(name.toLowerCase())) {
            return false;
        }
        if (name.length() > 16 || name.length() < 2) {
            return false;
        }
        if (!name.matches("[a-zA-Z0-9_]+")) {
            return false;
        }

        Team team = new Team(name, leader.getUniqueId());
        teams.put(name.toLowerCase(), team);
        playerTeam.put(leader.getUniqueId(), team);
        
        // Auto-trust all team members
        trustManager.trust(leader.getUniqueId(), leader.getUniqueId());
        
        save();
        return true;
    }

    public boolean inviteToTeam(Player leader, Player target) {
        Team team = playerTeam.get(leader.getUniqueId());
        if (team == null) return false;
        if (!team.isLeader(leader.getUniqueId())) return false;
        if (team.getMembers().size() >= MAX_TEAM_SIZE) return false;
        if (playerTeam.containsKey(target.getUniqueId())) return false;
        if (team.hasInvite(target.getUniqueId())) return false;

        team.addInvite(target.getUniqueId());
        save();
        return true;
    }

    public boolean joinTeam(Player player, String teamName) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) return false;
        if (playerTeam.containsKey(player.getUniqueId())) return false;
        if (team.getMembers().size() >= MAX_TEAM_SIZE) return false;
        if (!team.hasInvite(player.getUniqueId())) return false;

        team.removeInvite(player.getUniqueId());
        team.addMember(player.getUniqueId());
        playerTeam.put(player.getUniqueId(), team);

        // Auto-trust all team members
        for (UUID member : team.getMembers()) {
            trustManager.trust(player.getUniqueId(), member);
            trustManager.trust(member, player.getUniqueId());
        }

        save();
        return true;
    }

    public boolean leaveTeam(Player player) {
        Team team = playerTeam.get(player.getUniqueId());
        if (team == null) return false;

        boolean wasLeader = team.isLeader(player.getUniqueId());
        team.removeMember(player.getUniqueId());
        playerTeam.remove(player.getUniqueId());

        // Remove trust with former team members
        for (UUID member : team.getMembers()) {
            trustManager.untrust(player.getUniqueId(), member);
            trustManager.untrust(member, player.getUniqueId());
        }

        if (team.getMembers().isEmpty()) {
            teams.remove(team.getName().toLowerCase());
        } else if (wasLeader) {
            // Promote oldest member
            team.setLeader(team.getMembers().iterator().next());
        }

        save();
        return true;
    }

    public boolean kickFromTeam(Player leader, Player target) {
        Team team = playerTeam.get(leader.getUniqueId());
        if (team == null) return false;
        if (!team.isLeader(leader.getUniqueId())) return false;
        if (!team.getMembers().contains(target.getUniqueId())) return false;
        if (team.isLeader(target.getUniqueId())) return false;

        team.removeMember(target.getUniqueId());
        playerTeam.remove(target.getUniqueId());

        // Remove trust
        for (UUID member : team.getMembers()) {
            trustManager.untrust(target.getUniqueId(), member);
            trustManager.untrust(member, target.getUniqueId());
        }

        save();
        return true;
    }

    public boolean disbandTeam(Player leader) {
        Team team = playerTeam.get(leader.getUniqueId());
        if (team == null) return false;
        if (!team.isLeader(leader.getUniqueId())) return false;

        for (UUID member : team.getMembers()) {
            playerTeam.remove(member);
            for (UUID other : team.getMembers()) {
                trustManager.untrust(member, other);
            }
        }

        teams.remove(team.getName().toLowerCase());
        save();
        return true;
    }

    public Team getTeam(UUID playerId) {
        return playerTeam.get(playerId);
    }

    public Team getTeamByName(String name) {
        return teams.get(name.toLowerCase());
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public int getTeamSize(UUID playerId) {
        Team team = playerTeam.get(playerId);
        return team != null ? team.getMembers().size() : 0;
    }

    public boolean isInTeam(UUID playerId) {
        return playerTeam.containsKey(playerId);
    }

    public boolean isLeader(UUID playerId) {
        Team team = playerTeam.get(playerId);
        return team != null && team.isLeader(playerId);
    }

    public boolean areTeammates(UUID player1, UUID player2) {
        Team team1 = playerTeam.get(player1);
        Team team2 = playerTeam.get(player2);
        return team1 != null && team1 == team2;
    }

    public List<Player> getOnlineTeammates(Player player) {
        Team team = playerTeam.get(player.getUniqueId());
        if (team == null) return Collections.emptyList();
        
        List<Player> online = new ArrayList<>();
        for (UUID member : team.getMembers()) {
            if (!member.equals(player.getUniqueId())) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && p.isOnline()) {
                    online.add(p);
                }
            }
        }
        return online;
    }

    public void shutdown() {
        save();
    }
}