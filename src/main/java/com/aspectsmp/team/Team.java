package com.aspectsmp.team;

import com.aspectsmp.AspectSMP;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Team {

    private final String name;
    private UUID leader;
    private final Set<UUID> members = ConcurrentHashMap.newKeySet();
    private final Set<UUID> invites = ConcurrentHashMap.newKeySet();
    private long createdAt;

    public Team(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.createdAt = System.currentTimeMillis();
        members.add(leader);
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isLeader(UUID playerId) {
        return leader != null && leader.equals(playerId);
    }

    public boolean addMember(UUID playerId) {
        return members.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public void addInvite(UUID playerId) {
        invites.add(playerId);
    }

    public void removeInvite(UUID playerId) {
        invites.remove(playerId);
    }

    public boolean hasInvite(UUID playerId) {
        return invites.contains(playerId);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("leader", leader.toString());
        section.set("created_at", createdAt);
        
        List<String> memberList = new ArrayList<>();
        for (UUID uuid : members) {
            memberList.add(uuid.toString());
        }
        section.set("members", memberList);
        
        List<String> inviteList = new ArrayList<>();
        for (UUID uuid : invites) {
            inviteList.add(uuid.toString());
        }
        section.set("invites", inviteList);
    }

    public static Team load(ConfigurationSection section, AspectSMP plugin) {
        if (section == null) return null;
        
        String name = section.getString("name");
        String leaderStr = section.getString("leader");
        long createdAt = section.getLong("created_at", System.currentTimeMillis());
        
        if (name == null || leaderStr == null) return null;
        
        UUID leader;
        try {
            leader = UUID.fromString(leaderStr);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Team team = new Team(name, leader);
        team.createdAt = createdAt;

        List<String> memberList = section.getStringList("members");
        for (String uuidStr : memberList) {
            try {
                team.members.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }

        List<String> inviteList = section.getStringList("invites");
        for (String uuidStr : inviteList) {
            try {
                team.invites.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }

        return team;
    }
}