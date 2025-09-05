package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.listeners.DragonDefeatListener;
import com.example.speedrunnerswap.gui.AboutGuiListener;
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
        // Minimal About GUI listener for /swap maker
        getServer().getPluginManager().registerEvents(new AboutGuiListener(this), this);
        
        // Log startup with version
        String ver = getPluginMeta() != null ? getPluginMeta().getVersion() : "unknown";
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
