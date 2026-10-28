package com.aspectsmp.core;

import com.aspectsmp.AspectSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowRuleModifier implements RuleModifier {

    private final Map<UUID, Long> lastStealth = new ConcurrentHashMap<>();
    private static final long STEALTH_COOLDOWN_MS = 3000;
    private final Map<UUID, Team> stealthTeams = new ConcurrentHashMap<>();

    @Override
    public double modifyDamage(Player player, double damage, EntityDamageEvent.DamageCause cause) {
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            return damage * 0.5; // 50% void reduction
        }
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            return damage * 0.7; // 30% fall reduction
        }
        if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return damage * 1.05; // 5% damage bonus (was none)
        }
        return damage;
    }

    @Override
    public void modifyMovement(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastStealth.get(player.getUniqueId());
        if (last != null && now - last < STEALTH_COOLDOWN_MS) return;

        if (player.isSneaking() && player.getLocation().getBlock().getLightLevel() < 7) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, true, false), true);
            applyStealthEffects(player);
            lastStealth.put(player.getUniqueId(), now);
        } else {
            removeStealthEffects(player);
        }
    }

    @Override
    public void applyPassive(Player player) {
        if (player.getLocation().getBlock().getLightLevel() < 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0, true, false), true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false), true); // Speed 1
        }
    }

    private void applyStealthEffects(Player player) {
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("shadow_stealth_" + player.getUniqueId());
        
        if (team == null) {
            team = scoreboard.registerNewTeam("shadow_stealth_" + player.getUniqueId());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, Team.OptionStatus.NEVER);
        }
        
        team.addEntry(player.getName());
        
        // Hide held item by setting it to air in offhand temporarily
        // This is handled client-side via the invisibility effect
    }

    private void removeStealthEffects(Player player) {
        Team team = stealthTeams.get(player.getUniqueId());
        if (team != null) {
            team.removeEntry(player.getName());
            try {
                team.unregister();
            } catch (IllegalStateException ignored) {}
        }
    }
}