package com.aspectsmp.commands;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.team.Team;
import com.aspectsmp.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final AspectSMP plugin;
    private final TeamManager teamManager;

    public TeamCommand(AspectSMP plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§b§l       TEAM COMMANDS");
        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§e/team create <name> §7- Create a team (max 6 members)");
        player.sendMessage("§e/team invite <player> §7- Invite a player to your team");
        player.sendMessage("§e/team join <team> §7- Join a team you're invited to");
        player.sendMessage("§e/team leave §7- Leave your current team");
        player.sendMessage("§e/team kick <player> §7- Kick a member (leader only)");
        player.sendMessage("§e/team disband §7- Disband your team (leader only)");
        player.sendMessage("§e/team info §7- View your team info");
        player.sendMessage("§e/team list §7- List all teams");
        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§7Teams auto-trust members. No alliances allowed.");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team create <name>");
            return;
        }

        String name = args[1];
        if (teamManager.createTeam(player, name)) {
            player.sendMessage("§aTeam §f" + name + " §acreated! You are the leader.");
        } else {
            player.sendMessage("§cFailed to create team. Name taken, invalid, or you're already in a team.");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team invite <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot invite yourself.");
            return;
        }

        if (teamManager.inviteToTeam(player, target)) {
            player.sendMessage("§aInvited §f" + target.getName() + " §ato your team.");
            target.sendMessage("§eYou have been invited to join §f" + teamManager.getTeam(player.getUniqueId()).getName() + "§e! Use §f/team join " + teamManager.getTeam(player.getUniqueId()).getName() + "§e to join.");
        } else {
            player.sendMessage("§cFailed to invite. Team full, player already in a team, or already invited.");
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team join <team>");
            return;
        }

        String teamName = args[1];
        if (teamManager.joinTeam(player, teamName)) {
            Team team = teamManager.getTeamByName(teamName);
            player.sendMessage("§aJoined team §f" + team.getName() + "§a!");
            
            // Notify team members
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                    p.sendMessage("§a" + player.getName() + " §ahas joined the team!");
                }
            }
        } else {
            player.sendMessage("§cFailed to join. Team not found, full, or you're not invited.");
        }
    }

    private void handleLeave(Player player) {
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return;
        }

        String teamName = team.getName();
        if (teamManager.leaveTeam(player)) {
            player.sendMessage("§aLeft team §f" + teamName + "§a.");
            
            // Notify remaining members
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && p.isOnline()) {
                    p.sendMessage("§c" + player.getName() + " §chas left the team.");
                }
            }
        } else {
            player.sendMessage("§cFailed to leave team.");
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team kick <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return;
        }

        if (teamManager.kickFromTeam(player, target)) {
            player.sendMessage("§aKicked §f" + target.getName() + " §afrom the team.");
            target.sendMessage("§cYou have been kicked from §f" + team.getName() + "§c!");
            
            // Notify remaining members
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && p.isOnline()) {
                    p.sendMessage("§c" + target.getName() + " §chas been kicked from the team.");
                }
            }
        } else {
            player.sendMessage("§cFailed to kick. Not leader, player not in team, or trying to kick leader.");
        }
    }

    private void handleDisband(Player player) {
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return;
        }

        if (!teamManager.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the leader can disband the team.");
            return;
        }

        String teamName = team.getName();
        if (teamManager.disbandTeam(player)) {
            player.sendMessage("§aDisbanded team §f" + teamName + "§a.");
            
            // Notify former members
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null && p.isOnline()) {
                    p.sendMessage("§cTeam §f" + teamName + " §chas been disbanded.");
                }
            }
        } else {
            player.sendMessage("§cFailed to disband team.");
        }
    }

    private void handleInfo(Player player) {
        Team team = teamManager.getTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return;
        }

        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§b§l       TEAM: §f" + team.getName());
        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§eLeader: §f" + Bukkit.getOfflinePlayer(team.getLeader()).getName());
        player.sendMessage("§eMembers §7(" + team.getSize() + "/6§7):");
        
        for (UUID member : team.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            String status = (p != null && p.isOnline()) ? "§a●" : "§c○";
            String role = team.isLeader(member) ? " §d(Leader)" : "";
            player.sendMessage("  §f- " + Bukkit.getOfflinePlayer(member).getName() + role + " " + status);
        }

        if (!team.getInvites().isEmpty()) {
            player.sendMessage("§ePending Invites:");
            for (UUID invite : team.getInvites()) {
                player.sendMessage("  §f- " + Bukkit.getOfflinePlayer(invite).getName());
            }
        }
        player.sendMessage("§b§l══════════════════════════════════");
    }

    private void handleList(Player player) {
        List<Team> teams = new ArrayList<>(teamManager.getAllTeams());
        if (teams.isEmpty()) {
            player.sendMessage("§7No teams exist yet.");
            return;
        }

        player.sendMessage("§b§l══════════════════════════════════");
        player.sendMessage("§b§l       ALL TEAMS §7(" + teams.size() + " total§7)");
        player.sendMessage("§b§l══════════════════════════════════");
        
        for (Team team : teams) {
            String leaderName = Bukkit.getOfflinePlayer(team.getLeader()).getName();
            player.sendMessage("§f" + team.getName() + " §7- §eLeader: §f" + leaderName + " §7- §eMembers: §f" + team.getSize() + "/6");
        }
        player.sendMessage("§b§l══════════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(List.of("create", "invite", "join", "leave", "kick", "disband", "info", "list"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("invite") || sub.equals("kick")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getUniqueId().equals(((Player) sender).getUniqueId())) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("join")) {
                for (Team team : teamManager.getAllTeams()) {
                    completions.add(team.getName());
                }
            }
        }

        return completions;
    }
}