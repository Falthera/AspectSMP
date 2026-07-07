package com.aspectsmp;

import com.aspectsmp.abilities.AbilityManager;
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
import com.aspectsmp.gui.GuiManager;
import com.aspectsmp.listeners.ListenerManager;
import com.aspectsmp.storage.StorageManager;
import com.aspectsmp.trust.TrustManager;
import com.aspectsmp.util.ScoreboardManager;
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

    private void registerCommands() {
        InfoCommand infoCmd = new InfoCommand(this);
        getCommand("info").setExecutor(infoCmd);
        getCommand("info").setTabCompleter(infoCmd);
        getCommand("list").setExecutor(new ListCommand(this));
        getCommand("gui").setExecutor(new GuiCommand(this));
        getCommand("ability3").setExecutor(new Ability3Command(this));
        getCommand("ultimate").setExecutor(new UltimateCommand(this));
        GiveCommand giveCmd = new GiveCommand(this);
        getCommand("give").setExecutor(giveCmd);
        getCommand("give").setTabCompleter(giveCmd);
        RerollCommand rerollCmd = new RerollCommand(this);
        getCommand("reroll").setExecutor(rerollCmd);
        getCommand("reroll").setTabCompleter(rerollCmd);
        RepairCommand repairCmd = new RepairCommand(this);
        getCommand("repair").setExecutor(repairCmd);
        getCommand("repair").setTabCompleter(repairCmd);
        getCommand("reload").setExecutor(new ReloadCommand(this));
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
