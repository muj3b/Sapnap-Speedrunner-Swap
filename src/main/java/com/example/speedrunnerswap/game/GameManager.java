package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.ActionBarUtil;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GameManager {
    
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private int activeRunnerIndex;
    private List<Player> runners;
    private List<Player> hunters;
    private BukkitTask swapTask;
    private BukkitTask actionBarTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    
    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.runners = new ArrayList<>();
        this.hunters = new ArrayList<>();
        this.playerStates = new HashMap<>();
    }
    
    /**
     * Start the game
     * @return True if the game was started successfully
     */
    public boolean startGame() {
        if (gameRunning) {
            return false;
        }
        
        // Load teams from config
        loadTeams();
        
        // Check if we have enough players
        if (runners.size() < 1) {
            return false;
        }
        
        // Initialize game state
        gameRunning = true;
        gamePaused = false;
        activeRunnerIndex = 0;
        activeRunner = runners.get(activeRunnerIndex);
        
        // Save initial player states
        saveAllPlayerStates();
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Start the swap timer
        scheduleNextSwap();
        
        // Start the action bar timer
        startActionBarUpdates();
        
        // Start the tracker updates
        if (plugin.getConfigManager().isTrackerEnabled()) {
            plugin.getTrackerManager().startTracking();
        }
        
        // Broadcast game start if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Game started! Active runner: " + activeRunner.getName());
        }
        
        return true;
    }
    
    /**
     * Stop the game
     */
    public void stopGame() {
        if (!gameRunning) {
            return;
        }
        
        // Cancel tasks
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        
        // Stop tracker
        plugin.getTrackerManager().stopTracking();
        
        // Restore player states
        restoreAllPlayerStates();
        
        // Reset game state
        gameRunning = false;
        gamePaused = false;
        activeRunner = null;
        
        // Broadcast game stop if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§c[SpeedrunnerSwap] Game stopped!");
        }
    }
    
    /**
     * Pause the game
     * @return True if the game was paused successfully
     */
    public boolean pauseGame() {
        if (!gameRunning || gamePaused) {
            return false;
        }
        
        // Cancel swap task
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }
        
        gamePaused = true;
        
        // Broadcast game pause if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§e[SpeedrunnerSwap] Game paused!");
        }
        
        return true;
    }
    
    /**
     * Resume the game
     * @return True if the game was resumed successfully
     */
    public boolean resumeGame() {
        if (!gameRunning || !gamePaused) {
            return false;
        }
        
        gamePaused = false;
        
        // Schedule next swap
        scheduleNextSwap();
        
        // Broadcast game resume if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Game resumed!");
        }
        
        return true;
    }
    
    /**
     * Perform a swap to the next runner
     */
    public void performSwap() {
        if (!gameRunning || gamePaused || runners.size() < 1) {
            return;
        }
        
        // Save current active runner state
        savePlayerState(activeRunner);
        
        // Find next active runner
        activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
        Player nextRunner = runners.get(activeRunnerIndex);
        
        // Check if the next runner is online
        if (!nextRunner.isOnline()) {
            // Skip to the next runner
            activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
            nextRunner = runners.get(activeRunnerIndex);
            
            // If we've gone through all runners and none are online, pause the game
            if (!nextRunner.isOnline()) {
                pauseGame();
                return;
            }
        }
        
        // Check for safe location if enabled
        if (plugin.getConfigManager().isSafeSwapEnabled()) {
            Location safeLocation = SafeLocationFinder.findSafeLocation(nextRunner.getLocation(), 
                    plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                    plugin.getConfigManager().getSafeSwapVerticalDistance(),
                    plugin.getConfigManager().getDangerousBlocks());
            
            if (safeLocation != null) {
                nextRunner.teleport(safeLocation);
            }
        }
        
        // Apply grace period
        int gracePeriodTicks = plugin.getConfigManager().getGracePeriodTicks();
        if (gracePeriodTicks > 0) {
            nextRunner.setInvulnerable(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (nextRunner.isOnline()) {
                    nextRunner.setInvulnerable(false);
                }
            }, gracePeriodTicks);
        }
        
        // Update active runner
        Player previousRunner = activeRunner;
        activeRunner = nextRunner;
        
        // Restore state to new active runner
        restorePlayerState(activeRunner);
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Schedule next swap
        scheduleNextSwap();
        
        // Broadcast swap if enabled
        if (plugin.getConfigManager().isBroadcastsEnabled()) {
            Bukkit.broadcastMessage("§6[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + activeRunner.getName() + "!");
        }
    }
    
    /**
     * Schedule the next swap
     */
    private void scheduleNextSwap() {
        if (swapTask != null) {
            swapTask.cancel();
        }
        
        // Calculate next swap time
        long intervalSeconds;
        if (plugin.getConfigManager().isRandomizeSwap()) {
            // Use randomized interval with Gaussian jitter
            double mean = plugin.getConfigManager().getSwapInterval();
            double stdDev = plugin.getConfigManager().getJitterStdDev();
            double jitteredInterval = ThreadLocalRandom.current().nextGaussian() * stdDev + mean;
            
            // Clamp within min/max if enabled
            if (plugin.getConfigManager().isClampJitter()) {
                int min = plugin.getConfigManager().getMinSwapInterval();
                int max = plugin.getConfigManager().getMaxSwapInterval();
                jitteredInterval = Math.max(min, Math.min(max, jitteredInterval));
            }
            
            intervalSeconds = Math.round(jitteredInterval);
        } else {
            // Use fixed interval
            intervalSeconds = plugin.getConfigManager().getSwapInterval();
        }
        
        // Convert to ticks (20 ticks = 1 second)
        long intervalTicks = intervalSeconds * 20;
        
        // Set next swap time
        nextSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000);
        
        // Schedule the swap
        swapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, intervalTicks);
    }
    
    /**
     * Start the action bar updates
     */
    private void startActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning) {
                return;
            }
            
            // Update action bar for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateActionBar(player);
            }
        }, 0L, 5L); // Update every 5 ticks (1/4 second)
    }
    
    /**
     * Update the action bar for a player
     * @param player The player to update
     */
    private void updateActionBar(Player player) {
        if (!gameRunning) {
            return;
        }
        
        // Calculate time until next swap
        long timeUntilSwap = nextSwapTime - System.currentTimeMillis();
        int secondsUntilSwap = Math.max(0, (int) (timeUntilSwap / 1000));
        
        String message;
        
        if (gamePaused) {
            message = "§e§lGAME PAUSED";
        } else if (player.equals(activeRunner)) {
            // Active runner sees time and status
            String status = "";
            if (player.isSneaking()) {
                status = " §7[Sneaking]";
            } else if (player.isSprinting()) {
                status = " §b[Sprinting]";
            }
            
            message = "§a§lACTIVE §f| §eNext swap: §f" + secondsUntilSwap + "s" + status;
        } else if (runners.contains(player)) {
            // Inactive runner sees time only
            message = "§c§lINACTIVE §f| §eNext swap: §f" + secondsUntilSwap + "s";
        } else if (hunters.contains(player) && plugin.getConfigManager().isShowCoordinates() && activeRunner != null) {
            // Hunter sees coordinates of active runner if enabled
            Location loc = activeRunner.getLocation();
            message = "§6§lHUNTER §f| §eTarget: §f" + activeRunner.getName() + 
                    " §7(§f" + loc.getBlockX() + "§7, §f" + loc.getBlockY() + "§7, §f" + loc.getBlockZ() + "§7)";
        } else {
            // Spectator sees basic info
            message = "§7§lSPECTATOR §f| §eActive: §f" + (activeRunner != null ? activeRunner.getName() : "None");
        }
        
        ActionBarUtil.sendActionBar(player, message);
    }
    
    /**
     * Apply effects to inactive runners
     */
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                // Clear effects for active runner
                runner.removePotionEffect(PotionEffectType.BLINDNESS);
                runner.removePotionEffect(PotionEffectType.DARKNESS);
                runner.removePotionEffect(PotionEffectType.SLOW);
                runner.removePotionEffect(PotionEffectType.JUMP);
                
                // Set game mode to survival
                runner.setGameMode(GameMode.SURVIVAL);
            } else {
                // Apply effects to inactive runner
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    // Apply blindness and immobilization
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    // Set game mode to spectator
                    runner.setGameMode(GameMode.SPECTATOR);
                }
                
                // Mute inactive runner if voice chat integration is enabled
                if (plugin.getConfigManager().isVoiceChatEnabled() && 
                        plugin.getConfigManager().isMuteInactiveRunners()) {
                    plugin.getVoiceChatIntegration().mutePlayer(runner);
                }
            }
        }
        
        // Unmute active runner if voice chat integration is enabled
        if (activeRunner != null && plugin.getConfigManager().isVoiceChatEnabled() && 
                plugin.getConfigManager().isMuteInactiveRunners()) {
            plugin.getVoiceChatIntegration().unmutePlayer(activeRunner);
        }
    }
    
    /**
     * Load teams from config
     */
    private void loadTeams() {
        runners.clear();
        hunters.clear();
        
        // Load runners
        for (String name : plugin.getConfigManager().getRunnerNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                runners.add(player);
            }
        }
        
        // Load hunters
        for (String name : plugin.getConfigManager().getHunterNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                hunters.add(player);
            }
        }
    }
    
    /**
     * Save the state of all players
     */
    private void saveAllPlayerStates() {
        for (Player runner : runners) {
            savePlayerState(runner);
        }
    }
    
    /**
     * Save the state of a player
     * @param player The player to save
     */
    private void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        playerStates.put(player.getUniqueId(), state);
    }
    
    /**
     * Restore the state of all players
     */
    private void restoreAllPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerStates.containsKey(player.getUniqueId())) {
                restorePlayerState(player);
            }
            
            // Clear effects
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.SLOW);
            player.removePotionEffect(PotionEffectType.JUMP);
            
            // Reset game mode
            if (player.getGameMode() == GameMode.SPECTATOR && runners.contains(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            
            // Unmute if voice chat integration is enabled
            if (plugin.getConfigManager().isVoiceChatEnabled() && 
                    plugin.getConfigManager().isMuteInactiveRunners()) {
                plugin.getVoiceChatIntegration().unmutePlayer(player);
            }
        }
    }
    
    /**
     * Restore the state of a player
     * @param player The player to restore
     */
    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            PlayerStateUtil.applyPlayerState(player, state);
        }
    }
    
    /**
     * Set the runners
     * @param players The players to set as runners
     */
    public void setRunners(List<Player> players) {
        // Update config
        for (Player player : players) {
            plugin.getConfigManager().addRunner(player);
        }
        
        // Update runners list
        runners.clear();
        runners.addAll(players);
        
        // Remove from hunters if present
        hunters.removeAll(players);
        
        // Save config
        plugin.getConfigManager().saveConfig();
        
        // Broadcast team changes if enabled
        if (plugin.getConfigManager().isBroadcastTeamChanges()) {
            String runnerNames = runners.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Runners set: " + runnerNames);
        }
    }
    
    /**
     * Set the hunters
     * @param players The players to set as hunters
     */
    public void setHunters(List<Player> players) {
        // Update config
        for (Player player : players) {
            plugin.getConfigManager().addHunter(player);
        }
        
        // Update hunters list
        hunters.clear();
        hunters.addAll(players);
        
        // Remove from runners if present
        runners.removeAll(players);
        
        // Save config
        plugin.getConfigManager().saveConfig();
        
        // Broadcast team changes if enabled
        if (plugin.getConfigManager().isBroadcastTeamChanges()) {
            String hunterNames = hunters.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            Bukkit.broadcastMessage("§c[SpeedrunnerSwap] Hunters set: " + hunterNames);
        }
    }
    
    /**
     * Check if a player is the active runner
     * @param player The player to check
     * @return True if the player is the active runner
     */
    public boolean isActiveRunner(Player player) {
        return gameRunning && activeRunner != null && activeRunner.equals(player);
    }
    
    /**
     * Check if a player is a runner
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runners.contains(player);
    }
    
    /**
     * Check if a player is a hunter
     * @param player The player to check
     * @return True if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunters.contains(player);
    }
    
    /**
     * Get the active runner
     * @return The active runner
     */
    public Player getActiveRunner() {
        return activeRunner;
    }
    
    /**
     * Get the list of runners
     * @return The list of runners
     */
    public List<Player> getRunners() {
        return new ArrayList<>(runners);
    }
    
    /**
     * Get the list of hunters
     * @return The list of hunters
     */
    public List<Player> getHunters() {
        return new ArrayList<>(hunters);
    }
    
    /**
     * Check if the game is running
     * @return True if the game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    /**
     * Check if the game is paused
     * @return True if the game is paused
     */
    public boolean isGamePaused() {
        return gamePaused;
    }
    
    /**
     * Get the time until the next swap in seconds
     * @return The time until the next swap
     */
    public int getTimeUntilNextSwap() {
        if (!gameRunning || gamePaused) {
            return 0;
        }
        
        long timeUntilSwap = nextSwapTime - System.currentTimeMillis();
        return Math.max(0, (int) (timeUntilSwap / 1000));
    }
}