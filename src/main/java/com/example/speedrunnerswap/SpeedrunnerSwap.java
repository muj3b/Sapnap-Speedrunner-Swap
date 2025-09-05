package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.listeners.DragonDefeatListener;
import com.example.speedrunnerswap.listeners.EventListeners;
// Runner-only plugin: trackers, kits, stats and other systems removed
// Removed unused Bukkit import
import org.bukkit.plugin.java.JavaPlugin;

public final class SpeedrunnerSwap extends JavaPlugin {
    
    private static SpeedrunnerSwap instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    // GUI removed for ControlSwap
    // Removed: hunters/tracking/powerups/kits/stats/worldborder/bounty/suddendeath
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.gameManager = new GameManager(this);
        // No GUI manager in ControlSwap
        // Runner-only: no additional managers

        // Register commands
        SwapCommand swapCommand = new SwapCommand(this);
        getCommand("swap").setExecutor(swapCommand);
        getCommand("swap").setTabCompleter(swapCommand);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        getServer().getPluginManager().registerEvents(new DragonDefeatListener(this), this);
        // Register control GUI interactions
        getServer().getPluginManager().registerEvents(new com.example.speedrunnerswap.gui.ControlGuiListener(this), this);
        
        // Log startup with version
        // Prefer Paper's getPluginMeta() via reflection; fallback to deprecated getDescription() via reflection
        String ver = "unknown";
        try {
            java.lang.reflect.Method m = this.getClass().getMethod("getPluginMeta");
            Object meta = m.invoke(this);
            if (meta != null) {
                java.lang.reflect.Method gv = meta.getClass().getMethod("getVersion");
                Object v = gv.invoke(meta);
                if (v != null) ver = v.toString();
            }
        } catch (Throwable ignored) {
            try {
                java.lang.reflect.Method m2 = org.bukkit.plugin.java.JavaPlugin.class.getMethod("getDescription");
                Object desc = m2.invoke(this);
                if (desc != null) {
                    java.lang.reflect.Method gv2 = desc.getClass().getMethod("getVersion");
                    Object v2 = gv2.invoke(desc);
                    if (v2 != null) ver = v2.toString();
                }
            } catch (Throwable ignored2) {
            }
        }
        getLogger().info("ControlSwap v" + ver + " enabled");
    }
    
    @Override
    public void onDisable() {
        // Stop the game if it's running
        if (gameManager.isGameRunning()) {
            gameManager.stopGame();
        }
        
        // Save config
        configManager.saveConfig();
        
        // Log shutdown
        getLogger().info("ControlSwap disabled");
    }
    
    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static SpeedrunnerSwap getInstance() {
        return instance;
    }
    
    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the game manager
     * @return The game manager
     */
    public GameManager getGameManager() {
        return gameManager;
    }
    
    // GUI removed
    
    // Removed: getters for removed managers and power-up validation
}
