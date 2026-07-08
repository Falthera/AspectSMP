package com.aspectsmp;

import com.aspectsmp.abilities.AbilityManager;
import com.aspectsmp.commands.Ability1Command;
import com.aspectsmp.commands.Ability2Command;
import com.aspectsmp.commands.Ability3Command;
import com.aspectsmp.commands.GiveCommand;
import com.aspectsmp.commands.GuiCommand;
import com.aspectsmp.commands.InfoCommand;
import com.aspectsmp.commands.ListCommand;
import com.aspectsmp.commands.ReloadCommand;
import com.aspectsmp.commands.RepairCommand;
import com.aspectsmp.commands.RerollCommand;
import com.aspectsmp.commands.TrustCommand;
import com.aspectsmp.commands.UltimateCommand;
import com.aspectsmp.crafting.CraftingManager;
import com.aspectsmp.core.RuleModifierManager;
import com.aspectsmp.event.EventManager;
import com.aspectsmp.event.SkyRealmManager;
import com.aspectsmp.gui.GuiManager;
import com.aspectsmp.listeners.ListenerManager;
import com.aspectsmp.storage.StorageManager;
import com.aspectsmp.trust.TrustManager;
import com.aspectsmp.util.ScoreboardManager;
import com.aspectsmp.combat.CombatManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AspectSMP extends JavaPlugin {

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
    private CombatManager combatManager;
    private EventManager eventManager;
    private SkyRealmManager skyRealmManager;

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
        this.combatManager = new CombatManager(this, trustManager);
        this.eventManager = new EventManager(this);
        this.skyRealmManager = new SkyRealmManager(this);
        
        storageManager.initialize();
        abilityManager.initialize();
        craftingManager.initialize();
        
        listenerManager.registerAll();
        scoreboardManager = new ScoreboardManager(this);
        getServer().getPluginManager().registerEvents(ruleModifierManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        registerCommands();
        registerTrustCommand();
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, heartManager::saveAll, 6000L, 6000L);
        getServer().getScheduler().runTaskTimer(this, scoreboardManager::updateAll, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::updateAllActionBars, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, trustManager::save, 6000L, 6000L);
        
        getLogger().info("Aspect SMP enabled!");
    }

    @Override
    public void onDisable() {
        if (heartManager != null) {
            heartManager.saveAll();
        }
        if (trustManager != null) {
            trustManager.save();
        }
        if (combatManager != null) {
            combatManager.cleanup();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
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

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public SkyRealmManager getSkyRealmManager() {
        return skyRealmManager;
    }

    private void registerCommands() {
        InfoCommand infoCmd = new InfoCommand(this);
        getCommand("aspect-info").setExecutor(infoCmd);
        getCommand("aspect-info").setTabCompleter(infoCmd);
        getCommand("aspect-list").setExecutor(new ListCommand(this));
        getCommand("aspect-gui").setExecutor(new GuiCommand(this));
        getCommand("aspect-ability1").setExecutor(new Ability1Command(this));
        getCommand("aspect-ability2").setExecutor(new Ability2Command(this));
        getCommand("aspect-ability3").setExecutor(new Ability3Command(this));
        getCommand("aspect-ultimate").setExecutor(new UltimateCommand(this));
        GiveCommand giveCmd = new GiveCommand(this);
        getCommand("aspect-give").setExecutor(giveCmd);
        getCommand("aspect-give").setTabCompleter(giveCmd);
        RerollCommand rerollCmd = new RerollCommand(this);
        getCommand("aspect-reroll").setExecutor(rerollCmd);
        getCommand("aspect-reroll").setTabCompleter(rerollCmd);
        RepairCommand repairCmd = new RepairCommand(this);
        getCommand("aspect-repair").setExecutor(repairCmd);
        getCommand("aspect-repair").setTabCompleter(repairCmd);
        getCommand("aspect-reload").setExecutor(new ReloadCommand(this));
        getCommand("event").setExecutor(eventManager);
    }

    private void registerTrustCommand() {
        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("trust").setTabCompleter(new TrustCommand(this, trustManager));
    }

    private void updateAllActionBars() {
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            scoreboardManager.updateActionBar(player);
        }
    }
}
