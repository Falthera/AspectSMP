package com.aspectsmp;

import com.aspectsmp.abilities.AbilityManager;
import com.aspectsmp.commands.CommandManager;
import com.aspectsmp.commands.TrustCommand;
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
    private CommandManager commandManager;
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
        this.commandManager = new CommandManager(this);
        this.trustManager = new TrustManager(this);
        
        storageManager.initialize();
        abilityManager.initialize();
        craftingManager.initialize();
        
        listenerManager.registerAll();
        scoreboardManager = new ScoreboardManager(this);
        getServer().getPluginManager().registerEvents(ruleModifierManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        commandManager.registerCommands();
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

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TrustManager getTrustManager() {
        return trustManager;
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