package com.aspectsmp;

import com.aspectsmp.abilities.AbilityManager;
import com.aspectsmp.commands.Ability1Command;
import com.aspectsmp.commands.Ability2Command;
import com.aspectsmp.commands.Ability3Command;
import com.aspectsmp.commands.GiveCommand;
import com.aspectsmp.commands.GiveEventAspectCommand;
import com.aspectsmp.commands.InfoCommand;
import com.aspectsmp.commands.EclipseCommand;
import com.aspectsmp.commands.LeaveEventCommand;
import com.aspectsmp.commands.ListCommand;
import com.aspectsmp.commands.ParticlesCommand;
import com.aspectsmp.commands.ReloadCommand;
import com.aspectsmp.commands.RepairCommand;
import com.aspectsmp.commands.RerollCommand;
import com.aspectsmp.commands.ShadowCommand;
import com.aspectsmp.commands.TempestCommand;
import com.aspectsmp.commands.TeamCommand;
import com.aspectsmp.commands.TrustCommand;
import com.aspectsmp.commands.UltimateCommand;
import com.aspectsmp.commands.WithdrawCommand;
import com.aspectsmp.crafting.CraftingManager;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.RuleModifierManager;
import com.aspectsmp.event.EventManager;
import com.aspectsmp.event.EventPhase;
import com.aspectsmp.gui.GuiManager;
import com.aspectsmp.listeners.ListenerManager;
import com.aspectsmp.storage.StorageManager;
import com.aspectsmp.team.TeamManager;
import com.aspectsmp.trust.TrustManager;
import com.aspectsmp.util.MusicManager;
import com.aspectsmp.util.ResourcePackManager;
import com.aspectsmp.util.ScoreboardManager;
import com.aspectsmp.visual.VisualEffectsManager;
import com.aspectsmp.combat.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class AspectSMP extends JavaPlugin implements Listener {

    private static AspectSMP instance;
    private com.aspectsmp.core.HeartManager heartManager;
    private AbilityManager abilityManager;
    private StorageManager storageManager;
    private CraftingManager craftingManager;
    private GuiManager guiManager;
    private ListenerManager listenerManager;
    private RuleModifierManager ruleModifierManager;
    private ScoreboardManager scoreboardManager;
    private TrustManager trustManager;
    private TeamManager teamManager;
    private CombatManager combatManager;
    private EventManager eventManager;
    private VisualEffectsManager visualEffectsManager;
    private ResourcePackManager resourcePackManager;
    private MusicManager musicManager;
    
    // Event aspect exclusivity tracking
    private UUID cloudAspectHolder = null;
    private UUID winterAspectHolder = null;
    private UUID shadowAspectHolder = null;
    private UUID stormAspectHolder = null;
    
    // Event participant locations for /leaveevent
    private final java.util.Map<UUID, Location> eventJoinLocations = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.storageManager = new StorageManager(this);
        this.heartManager = new com.aspectsmp.core.HeartManager(this);
        this.abilityManager = new AbilityManager(this);
        this.craftingManager = new CraftingManager(this);
        this.guiManager = new GuiManager(this);
        this.listenerManager = new ListenerManager(this);
        this.ruleModifierManager = new RuleModifierManager(this);
        this.trustManager = new TrustManager(this);
        this.teamManager = new TeamManager(this, trustManager);
        this.combatManager = new CombatManager(this, trustManager);
        this.eventManager = new EventManager(this);
        this.visualEffectsManager = new VisualEffectsManager(this);
        this.resourcePackManager = new ResourcePackManager(this);
        this.musicManager = new MusicManager(this);
        
        // Load claimed aspects from storage
        loadClaimedAspects();
        
        storageManager.initialize();
        abilityManager.initialize();
        craftingManager.initialize();
        
        listenerManager.registerAll();
        scoreboardManager = new ScoreboardManager(this);
        getServer().getPluginManager().registerEvents(ruleModifierManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
        registerTrustCommand();
        registerTeamCommand();
        registerWithdrawCommand();
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, heartManager::saveAll, 6000L, 6000L);
        getServer().getScheduler().runTaskTimer(this, scoreboardManager::updateAll, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::updateAllActionBars, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, trustManager::save, 6000L, 6000L);
        getServer().getScheduler().runTaskTimer(this, this::enforceCloudheartRules, 20L, 1200L);
        
        // Send resource pack to online players
        resourcePackManager.sendToAll();
        
        // Start global music playlist
        musicManager.playGlobalPlaylist(musicManager.getCurrentGlobalPlaylist() != null 
            ? musicManager.getCurrentGlobalPlaylist() : "ambient");
        
        getLogger().info("Aspect SMP enabled!");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (visualEffectsManager != null) {
            visualEffectsManager.shutdown();
        }
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (ruleModifierManager != null) {
            ruleModifierManager.shutdown();
        }
        if (trustManager != null) {
            trustManager.shutdown();
        }
        if (teamManager != null) {
            teamManager.shutdown();
        }
        if (heartManager != null) {
            heartManager.saveAll();
        }
        if (combatManager != null) {
            combatManager.cleanup();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (musicManager != null) {
            musicManager.shutdown();
        }
        // Cancel all remaining scheduler tasks
        getServer().getScheduler().cancelTasks(this);
        saveClaimedAspects();
        
        getLogger().info("Aspect SMP disabled!");
    }

    public static AspectSMP getInstance() {
        return instance;
    }

    public com.aspectsmp.core.HeartManager getHeartManager() {
        return heartManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public RuleModifierManager getRuleModifierManager() {
        return ruleModifierManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

public VisualEffectsManager getVisualEffectsManager() {
        return visualEffectsManager;
    }
    
    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
    
    public MusicManager getMusicManager() {
        return musicManager;
    }
    
    // Event aspect exclusivity methods
    public boolean isAspectClaimed(AspectType aspect) {
        return switch (aspect) {
            case CLOUD -> cloudAspectHolder != null;
            case WINTER -> winterAspectHolder != null;
            case SHADOW -> shadowAspectHolder != null;
            case STORM -> stormAspectHolder != null;
            default -> false;
        };
    }

    public UUID getAspectHolder(AspectType aspect) {
        return switch (aspect) {
            case CLOUD -> cloudAspectHolder;
            case WINTER -> winterAspectHolder;
            case SHADOW -> shadowAspectHolder;
            case STORM -> stormAspectHolder;
            default -> null;
        };
    }

    public void claimAspect(AspectType aspect, UUID player) {
        switch (aspect) {
            case CLOUD -> cloudAspectHolder = player;
            case WINTER -> winterAspectHolder = player;
            case SHADOW -> shadowAspectHolder = player;
            case STORM -> stormAspectHolder = player;
        }
        saveClaimedAspects();
    }

    public void releaseAspect(AspectType aspect) {
        switch (aspect) {
            case CLOUD -> cloudAspectHolder = null;
            case WINTER -> winterAspectHolder = null;
            case SHADOW -> shadowAspectHolder = null;
            case STORM -> stormAspectHolder = null;
        }
        saveClaimedAspects();
    }

    private void loadClaimedAspects() {
        FileConfiguration config = getConfig();
        String cloud = config.getString("claimed-aspects.cloud");
        String winter = config.getString("claimed-aspects.winter");
        String shadow = config.getString("claimed-aspects.shadow");
        String storm = config.getString("claimed-aspects.storm");
        
        if (cloud != null) cloudAspectHolder = UUID.fromString(cloud);
        if (winter != null) winterAspectHolder = UUID.fromString(winter);
        if (shadow != null) shadowAspectHolder = UUID.fromString(shadow);
        if (storm != null) stormAspectHolder = UUID.fromString(storm);
    }

    private void saveClaimedAspects() {
        FileConfiguration config = getConfig();
        if (cloudAspectHolder != null) config.set("claimed-aspects.cloud", cloudAspectHolder.toString());
        if (winterAspectHolder != null) config.set("claimed-aspects.winter", winterAspectHolder.toString());
        if (shadowAspectHolder != null) config.set("claimed-aspects.shadow", shadowAspectHolder.toString());
        if (stormAspectHolder != null) config.set("claimed-aspects.storm", stormAspectHolder.toString());
        saveConfig();
    }

    // Event location tracking for /leaveevent
    public void setEventJoinLocation(UUID player, Location location) {
        eventJoinLocations.put(player, location.clone());
    }

    public Location getEventJoinLocation(UUID player) {
        return eventJoinLocations.get(player);
    }

    public void removeEventJoinLocation(UUID player) {
        eventJoinLocations.remove(player);
    }

    public boolean hasEventJoinLocation(UUID player) {
        return eventJoinLocations.containsKey(player);
    }

    private void registerCommands() {
        // Ability commands
        getCommand("ability1").setExecutor(new Ability1Command(this));
        getCommand("ability2").setExecutor(new Ability2Command(this));
        getCommand("ability3").setExecutor(new Ability3Command(this));
        getCommand("ultimate").setExecutor(new UltimateCommand(this));
        
        // Info commands
        InfoCommand infoCmd = new InfoCommand(this);
        getCommand("info").setExecutor(infoCmd);
        getCommand("info").setTabCompleter(infoCmd);
        
        getCommand("list").setExecutor(new ListCommand(this));
        
        RerollCommand rerollCmd = new RerollCommand(this);
        getCommand("reroll").setExecutor(rerollCmd);
        getCommand("reroll").setTabCompleter(rerollCmd);
        
        RepairCommand repairCmd = new RepairCommand(this);
        getCommand("repair").setExecutor(repairCmd);
        getCommand("repair").setTabCompleter(repairCmd);
        
        getCommand("reload").setExecutor(new ReloadCommand(this));
        
        GiveCommand giveCmd = new GiveCommand(this);
        getCommand("aspect-give").setExecutor(giveCmd);
        getCommand("aspect-give").setTabCompleter(giveCmd);
        
        GiveEventAspectCommand giveEventCmd = new GiveEventAspectCommand(this);
        getCommand("give-event-aspect").setExecutor(giveEventCmd);
        getCommand("give-event-aspect").setTabCompleter(giveEventCmd);
        
        // Event commands
        EclipseCommand eclipseCmd = new EclipseCommand(this);
        getCommand("eclipse").setExecutor(eclipseCmd);
        getCommand("event").setExecutor(eclipseCmd);
        
        getCommand("shadow").setExecutor(new ShadowCommand(this));
        getCommand("tempest").setExecutor(new TempestCommand(this));
        
        // Particle command
        ParticlesCommand particlesCmd = new ParticlesCommand(this);
        getCommand("particles").setExecutor(particlesCmd);
        getCommand("particles").setTabCompleter(particlesCmd);
        
        // Trust/Team/Withdraw commands
        TrustCommand trustCmd = new TrustCommand(this, trustManager);
        getCommand("trust").setExecutor(trustCmd);
        getCommand("trust").setTabCompleter(trustCmd);
        
        WithdrawCommand withdrawCmd = new WithdrawCommand(this);
        getCommand("withdraw").setExecutor(withdrawCmd);
        getCommand("withdraw").setTabCompleter(withdrawCmd);
        
        getCommand("leaveevent").setExecutor(new LeaveEventCommand(this));
        
        TeamCommand teamCmd = new TeamCommand(this, teamManager);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);
    }

    private void registerTrustCommand() {
        // Trust command registration handled by plugin.yml
    }

    private void registerTeamCommand() {
        // Team command registration handled by plugin.yml
    }

    private void registerWithdrawCommand() {
        // Withdraw command registration handled by plugin.yml
    }

    private void updateAllActionBars() {
        // Update action bars for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (scoreboardManager != null) {
                scoreboardManager.updatePlayer(player);
                scoreboardManager.updateActionBar(player);
            }
        }
    }

    private void enforceCloudheartRules() {
        // Enforce Cloudheart event rules
        if (eventManager != null) {
            eventManager.getActiveEvents().forEach(event -> {
                if (event instanceof com.aspectsmp.event.NinthSkyEvent ninthSky) {
                    ninthSky.enforceCloudheartRules();
                }
            });
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (resourcePackManager != null) {
            resourcePackManager.onPlayerJoin(player);
        }
        if (musicManager != null) {
            musicManager.onPlayerJoin(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (musicManager != null) {
            musicManager.onPlayerQuit(player);
        }
    }
    
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (resourcePackManager != null) {
            resourcePackManager.onResourcePackStatus(event.getPlayer(), event.getStatus().name().toLowerCase());
        }
    }
}