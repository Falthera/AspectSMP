package com.aspectsmp;

import com.aspectsmp.abilities.AbilityManager;
import com.aspectsmp.commands.CommandManager;
import com.aspectsmp.crafting.CraftingManager;
import com.aspectsmp.core.RuleModifierManager;
import com.aspectsmp.gui.GuiManager;
import com.aspectsmp.listeners.ListenerManager;
import com.aspectsmp.storage.StorageManager;
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
        
        storageManager.initialize();
        abilityManager.initialize();
        craftingManager.initialize();
        
        listenerManager.registerAll();
        getServer().getPluginManager().registerEvents(ruleModifierManager, this);
        commandManager.registerCommands();
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, heartManager::saveAll, 6000L, 6000L);
        
        getLogger().info("Aspect SMP enabled!");
    }

    @Override
    public void onDisable() {
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
}