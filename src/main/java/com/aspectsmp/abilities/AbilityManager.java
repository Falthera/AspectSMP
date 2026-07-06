package com.aspectsmp.abilities;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.abilities.inferno.FlameStepAbility;
import com.aspectsmp.abilities.inferno.LavaBurstAbility;
import com.aspectsmp.abilities.inferno.InfernalChainsAbility;
import com.aspectsmp.abilities.inferno.SunCoreCollapseAbility;
import com.aspectsmp.abilities.tide.HydroPushAbility;
import com.aspectsmp.abilities.tide.TidalPrisonAbility;
import com.aspectsmp.abilities.tide.OceanSurgeAbility;
import com.aspectsmp.abilities.tide.MaelstromAbility;
import com.aspectsmp.abilities.tempest.DashChainAbility;
import com.aspectsmp.abilities.tempest.StaticBoltAbility;
import com.aspectsmp.abilities.tempest.StormRushAbility;
import com.aspectsmp.abilities.tempest.ThunderExecutionAbility;
import com.aspectsmp.abilities.rift.BlinkAbility;
import com.aspectsmp.abilities.rift.VoidSwapAbility;
import com.aspectsmp.abilities.rift.DimensionalTearAbility;
import com.aspectsmp.abilities.rift.RealityCollapseAbility;
import com.aspectsmp.abilities.vitality.HealPulseAbility;
import com.aspectsmp.abilities.vitality.LifeLinkAbility;
import com.aspectsmp.abilities.vitality.RevivalShieldAbility;
import com.aspectsmp.abilities.vitality.GenesisFieldAbility;
import com.aspectsmp.abilities.war.BerserkSlashAbility;
import com.aspectsmp.abilities.war.RageModeAbility;
import com.aspectsmp.abilities.war.ExecutionChainAbility;
import com.aspectsmp.abilities.war.WarGodStateAbility;
import com.aspectsmp.abilities.cosmos.GravityPullAbility;
import com.aspectsmp.abilities.cosmos.OrbitalShieldAbility;
import com.aspectsmp.abilities.cosmos.MeteorCallAbility;
import com.aspectsmp.abilities.cosmos.BlackStarCollapseAbility;
import com.aspectsmp.abilities.fortune.LuckyStrikeAbility;
import com.aspectsmp.abilities.fortune.OreSenseAbility;
import com.aspectsmp.abilities.fortune.MiningRushAbility;
import com.aspectsmp.abilities.fortune.KingsJackpotAbility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {

    private final AspectSMP plugin;
    private final Map<String, Ability> registeredAbilities;
    private final Map<AspectType, Set<Ability>> aspectAbilities;
    private final Map<UUID, Set<Ability>> activePassives;

    public AbilityManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.registeredAbilities = new ConcurrentHashMap<>();
        this.aspectAbilities = new EnumMap<>(AspectType.class);
        this.activePassives = new ConcurrentHashMap<>();
        
        for (AspectType type : AspectType.values()) {
            aspectAbilities.put(type, ConcurrentHashMap.newKeySet());
        }
    }

    public void initialize() {
        registerAspectAbilities();
    }

    private void registerAspectAbilities() {
        for (AspectType type : AspectType.values()) {
            Set<Ability> abilities = aspectAbilities.get(type);
            switch (type) {
                case INFERNO -> {
                    abilities.add(new FlameStepAbility());
                    abilities.add(new LavaBurstAbility());
                    abilities.add(new InfernalChainsAbility());
                    abilities.add(new SunCoreCollapseAbility());
                }
                case TIDE -> {
                    abilities.add(new HydroPushAbility());
                    abilities.add(new TidalPrisonAbility());
                    abilities.add(new OceanSurgeAbility());
                    abilities.add(new MaelstromAbility());
                }
                case TEMPEST -> {
                    abilities.add(new DashChainAbility());
                    abilities.add(new StaticBoltAbility());
                    abilities.add(new StormRushAbility());
                    abilities.add(new ThunderExecutionAbility());
                }
                case RIFT -> {
                    abilities.add(new BlinkAbility());
                    abilities.add(new VoidSwapAbility());
                    abilities.add(new DimensionalTearAbility());
                    abilities.add(new RealityCollapseAbility());
                }
                case VITALITY -> {
                    abilities.add(new HealPulseAbility());
                    abilities.add(new LifeLinkAbility());
                    abilities.add(new RevivalShieldAbility());
                    abilities.add(new GenesisFieldAbility());
                }
                case WAR -> {
                    abilities.add(new BerserkSlashAbility());
                    abilities.add(new RageModeAbility());
                    abilities.add(new ExecutionChainAbility());
                    abilities.add(new WarGodStateAbility());
                }
                case COSMOS -> {
                    abilities.add(new GravityPullAbility());
                    abilities.add(new OrbitalShieldAbility());
                    abilities.add(new MeteorCallAbility());
                    abilities.add(new BlackStarCollapseAbility());
                }
                case FORTUNE -> {
                    abilities.add(new LuckyStrikeAbility());
                    abilities.add(new OreSenseAbility());
                    abilities.add(new MiningRushAbility());
                    abilities.add(new KingsJackpotAbility());
                }
            }
            abilities.forEach(ability -> registeredAbilities.put(ability.getId(), ability));
        }
    }

    public void registerAbility(Ability ability) {
        registeredAbilities.put(ability.getId(), ability);
    }

    public Optional<Ability> getAbility(String id) {
        return Optional.ofNullable(registeredAbilities.get(id));
    }

    public Set<Ability> getAbilitiesForAspect(AspectType type) {
        return new HashSet<>(aspectAbilities.getOrDefault(type, Collections.emptySet()));
    }

    public void handleAbilityTrigger(Player player, Heart heart, EquipmentSlot slot) {
        if (slot != EquipmentSlot.OFF_HAND) return;
        if (!heart.canUseAbilities()) return;
        
        Set<Ability> abilities = aspectAbilities.get(heart.getAspect());
        if (abilities == null) return;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            abilities.stream()
                .filter(a -> !a.isPassive())
                .filter(a -> a.getTier() <= heart.getTier())
                .forEach(ability -> {
                    if (canUseAbility(player, ability)) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            ability.execute(player, heart, null));
                    }
                });
        });
    }

    public void handleTier1Ability(Player player, Heart heart) {
        if (!heart.canUseAbilities()) return;
        
        Set<Ability> abilities = aspectAbilities.get(heart.getAspect());
        if (abilities == null) return;
        
        Ability tier1Ability = abilities.stream()
            .filter(a -> !a.isPassive())
            .filter(a -> a.getTier() == 1)
            .findFirst()
            .orElse(null);
            
        if (tier1Ability != null && canUseAbility(player, tier1Ability)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                tier1Ability.execute(player, heart, null);
                heart.addEssence(5);
                player.sendMessage("§b+5 Essence");
            });
        } else if (tier1Ability != null) {
            player.sendMessage("§cAbility is on cooldown!");
        }
    }

    public void handleTier2Ability(Player player, Heart heart) {
        if (!heart.canUseAbilities() || heart.getTier() < 2) return;
        
        Set<Ability> abilities = aspectAbilities.get(heart.getAspect());
        if (abilities == null) return;
        
        Ability tier2Ability = abilities.stream()
            .filter(a -> !a.isPassive())
            .filter(a -> a.getTier() == 2)
            .findFirst()
            .orElse(null);
            
        if (tier2Ability != null && canUseAbility(player, tier2Ability)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                tier2Ability.execute(player, heart, null);
                heart.addEssence(10);
                player.sendMessage("§b+10 Essence");
            });
        } else if (tier2Ability != null) {
            player.sendMessage("§cAbility is on cooldown!");
        }
    }

    public void handleTier3Ability(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant() || heart.getTier() < 3) {
            player.sendMessage("§cYou need Tier 3 to use this ability!");
            return;
        }
        
        Set<Ability> abilities = aspectAbilities.get(heart.getAspect());
        if (abilities == null) return;
        
        Ability tier3Ability = abilities.stream()
            .filter(a -> !a.isPassive())
            .filter(a -> a.getTier() == 3)
            .filter(a -> !a.isUltimate())
            .findFirst()
            .orElse(null);
            
        if (tier3Ability != null && canUseAbility(player, tier3Ability)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                tier3Ability.execute(player, heart, null);
                heart.addEssence(15);
                player.sendMessage("§b+15 Essence");
            });
        } else if (tier3Ability != null) {
            player.sendMessage("§cAbility is on cooldown or not unlocked!");
        } else {
            player.sendMessage("§cNo Tier 3 ability available!");
        }
    }

    public void handleUltimate(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant() || heart.getTier() < 3) {
            player.sendMessage("§cYour Heart is not responsive or not Tier 3!");
            return;
        }
        
        Set<Ability> abilities = aspectAbilities.get(heart.getAspect());
        if (abilities == null) return;
        
        Ability ultimate = abilities.stream()
            .filter(a -> !a.isPassive())
            .filter(a -> a.isUltimate())
            .findFirst()
            .orElse(null);
            
        if (ultimate != null && canUseAbility(player, ultimate)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                ultimate.execute(player, heart, null);
                heart.addEssence(25);
                player.sendMessage("§b+25 Essence");
            });
        } else {
            player.sendMessage("§cUltimate is on cooldown or not unlocked!");
        }
    }

    private boolean canUseAbility(Player player, Ability ability) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return false;
        
        long cooldownEnd = heart.getCooldowns().getOrDefault(ability.getId(), 0L);
        return System.currentTimeMillis() >= cooldownEnd;
    }

    public void setActivePassives(Player player, Set<Ability> passives) {
        activePassives.put(player.getUniqueId(), passives);
    }

    public Set<Ability> getActivePassives(Player player) {
        return activePassives.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public void processPassives(Player player) {
        Set<Ability> passives = activePassives.get(player.getUniqueId());
        if (passives == null) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        passives.stream()
            .filter(Ability::isPassive)
            .forEach(ability -> ability.applyPassive(player, heart));
    }
}