package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import java.time.Duration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
// Use compat resolver for cross-version effect lookups
// no compat helpers used directly in this class

public class GameManager {
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private List<Player> runners;
    private BukkitTask swapTask;
    private BukkitTask actionBarTask;
    private BukkitTask titleTask;
    private BukkitTask cageTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    // Cage management for CAGE freeze mode
    private final Map<java.util.UUID, java.util.List<org.bukkit.block.BlockState>> builtCages = new java.util.HashMap<>();
    private final Map<java.util.UUID, org.bukkit.Location> cageCenters = new java.util.HashMap<>();
    
    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.runners = new ArrayList<>();
        this.playerStates = new HashMap<>();
    }
    
    public boolean startGame() {
        if (gameRunning) {
            return false;
        }
        
        if (!canStartGame()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§cGame cannot start: Add at least one runner.");
            }
            return false;
        }
        
        // Countdown
        new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (count > 0) {
                    Title title = Title.title(
                        Component.text("Starting in " + count).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Made by muj3b").color(NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(500))
                    );
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.showTitle(title);
                    }
                    count--;
                } else {
                    this.cancel();
                    gameRunning = true;
                    gamePaused = false;
                    // Randomize starting runner if 3+ players; keep order for 2
                    if (runners.size() >= 3) {
                        int start = java.util.concurrent.ThreadLocalRandom.current().nextInt(runners.size());
                        java.util.Collections.rotate(runners, -start);
                    }
                    activeRunner = runners.get(0);
                    saveAllPlayerStates();

                applyInactiveEffects();
                scheduleNextSwap();
                startActionBarUpdates();
                startTitleUpdates();
                startCageEnforcement();
                
            }
        }
    }.runTaskTimer(plugin, 0L, 20L);
    
    return true;
}    public void endGame(Team winner) {
        if (!gameRunning) {
            return;
        }

        Component titleText;
        String runnerSubtitle = "";

        if (winner == Team.RUNNER) {
            titleText = Component.text("RUNNERS WIN!", NamedTextColor.GREEN, TextDecoration.BOLD);
            runnerSubtitle = "Great run!";
        } else {
            titleText = Component.text("GAME OVER", NamedTextColor.RED, TextDecoration.BOLD);
            runnerSubtitle = "No winner declared.";
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Component subtitleText = Component.text(runnerSubtitle, NamedTextColor.YELLOW);

            Title endTitle = Title.title(
                titleText,
                subtitleText,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(500))
            );
            player.showTitle(endTitle);
        }

        if (swapTask != null) swapTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
        if (titleTask != null) titleTask.cancel();
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Optionally preserve final runner progress for all runners (configurable)
                if (plugin.getConfig().getBoolean("swap.preserve_runner_progress_on_end", false)) {
                    try {
                        if (activeRunner != null && activeRunner.isOnline() && !runners.isEmpty()) {
                            com.example.speedrunnerswap.models.PlayerState finalState = PlayerStateUtil.capturePlayerState(activeRunner);
                            for (Player r : runners) {
                                playerStates.put(r.getUniqueId(), finalState);
                            }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to capture/apply final runner state: " + ex.getMessage());
                    }
                }

                cleanupAllCages();
                restoreAllPlayerStates();
                
                gameRunning = false;
                gamePaused = false;
                activeRunner = null;
                
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    String winnerMessage = (winner == Team.RUNNER) ? "Runners win!" : "Game ended!";
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage("§a[ControlSwap] Game ended! " + winnerMessage);
                    }
                }

                broadcastDonationMessage();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void broadcastDonationMessage() {
        final String donateUrl = plugin.getConfig().getString(
            "donation.url",
            "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01"
        );

        Component spacer = Component.text("");
        Component header = Component.text("=== Support the Creator ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);
        Component desc = Component.text("Enjoyed the game? Help keep updates coming!")
            .color(NamedTextColor.YELLOW);
        Component donate = Component.text("❤ Click to Donate")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Open donation page", NamedTextColor.GOLD)))
            .clickEvent(ClickEvent.openUrl(donateUrl));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(spacer);
            player.sendMessage(header);
            player.sendMessage(desc);
            player.sendMessage(donate);
            player.sendMessage(spacer);
        }
    }
    /** Stop the game without declaring a winner */
    public void stopGame() {
        endGame(null);
    }

    /**
     * Get whether the player is a runner
     * @param player The player to check
     * @return true if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runners.contains(player);
    }

    /**
     * Get whether the game is running
     * @return true if the game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Get the current active runner
     * @return The currently active runner
     */
    public Player getActiveRunner() {
        return activeRunner;
    }

    /**
     * Get all runners
     * @return List of all runners
     */
    public List<Player> getRunners() {
        return runners;
    }

    
    /**
     * Refresh the swap schedule timer
     */
    public void refreshSwapSchedule() {
        if (gameRunning && !gamePaused) {
            scheduleNextSwap();
        }
    }

    /**
     * Refresh the action bar display for all players
     */
    public void refreshActionBar() {
        if (gameRunning && !gamePaused) {
            updateActionBar();
        }
    }

    /**
     * Get the game state for a specific player
     * @param player The player to get state for
     * @return The player's game state or null if not found
     */
    public PlayerState getPlayerState(Player player) {
        if (player == null) return null;
        return playerStates.computeIfAbsent(player.getUniqueId(), id -> PlayerStateUtil.capturePlayerState(player));
    }

    /**
     * Check if the game can be started
     * @return true if the game can be started, false otherwise
     */
    public boolean canStartGame() {
        if (gameRunning) {
            return false;
        }
        loadTeams();
        return !runners.isEmpty();
    }

    /**
     * Update teams after player join/leave
     */
    public void updateTeams() {
        List<Player> newRunners = new ArrayList<>();

        for (Player runner : runners) {
            if (runner.isOnline()) {
                newRunners.add(runner);
            }
        }
        
        runners = newRunners;

        // If all runners disconnect, pause instead of ending the game
        if (gameRunning && runners.isEmpty()) {
                if (plugin.getConfigManager().isPauseOnDisconnect()) {
                    pauseGame();
                    if (plugin.getConfigManager().isBroadcastGameEvents()) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage("§e[ControlSwap] Game paused: waiting for players to return.");
                        }
                    }
                } else {
                // Keep running but log a warning for admins
                plugin.getLogger().warning("No runners online; game continues (pause_on_disconnect=false)");
            }
        }
    }

    /**
     * Handle a player quitting
     * @param player The player who quit
     */
    public void handlePlayerQuit(Player player) {
        if (!gameRunning) {
            return;
        }

        if (player.equals(activeRunner)) {
            if (plugin.getConfigManager().isPauseOnDisconnect()) {
                pauseGame();
            } else {
                performSwap();
            }
        }
        
        savePlayerState(player);
    }

    /**
     * Get time until next swap in seconds
     */
    public int getTimeUntilNextSwap() {
        return (int) ((nextSwapTime - System.currentTimeMillis()) / 1000);
    }
    
    private void saveAllPlayerStates() {
        for (Player runner : runners) {
            savePlayerState(runner);
        }
    }
    
    private void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        playerStates.put(player.getUniqueId(), state);
    }
    
    private void restoreAllPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerStates.containsKey(player.getUniqueId())) {
                restorePlayerState(player);
            }
            
            // Remove effects using compat lookups for cross-version support
            PotionEffectType eff;
            if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("weakness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("jump_boost")) != null) player.removePotionEffect(eff);
            
            if (player.getGameMode() == GameMode.SPECTATOR && runners.contains(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }
    
    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            try {
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                // Reset to server-defined max health using version-safe attribute access
                player.setHealth(BukkitCompat.getMaxHealthValue(player));
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.setFallDistance(0);
                player.setInvulnerable(false);
                
                PlayerStateUtil.applyPlayerState(player, state);
                
                Location loc = state.getLocation();
                if (loc != null && !loc.getBlock().getType().isSolid()) {
                    player.teleport(loc);
                } else {
                    player.teleport(plugin.getConfigManager().getSpawnLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore state for player " + player.getName() + ": " + e.getMessage());
                player.teleport(plugin.getConfigManager().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
    }
    
    private void loadTeams() {
        runners.clear();
        for (String name : plugin.getConfigManager().getRunnerNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                runners.add(player);
            }
        }
    }
    
    private void scheduleNextSwap() {
        if (swapTask != null) {
            swapTask.cancel();
        }
        
        long intervalSeconds;
        if (plugin.getConfigManager().isSwapRandomized()) {
            double mean = plugin.getConfigManager().getSwapInterval();
            double stdDev = plugin.getConfigManager().getJitterStdDev();
            double jitteredInterval = ThreadLocalRandom.current().nextGaussian() * stdDev + mean;
            
            if (plugin.getConfigManager().isClampJitter()) {
                int min = plugin.getConfigManager().getMinSwapInterval();
                int max = plugin.getConfigManager().getMaxSwapInterval();
                jitteredInterval = Math.max(min, Math.min(max, jitteredInterval));
            }
            
            intervalSeconds = Math.round(jitteredInterval);
        } else {
            intervalSeconds = plugin.getConfigManager().getSwapInterval();
        }
        
        long intervalTicks = intervalSeconds * 20;
        nextSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000);
        swapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, intervalTicks);
    }
    
    // Removed: hunter swap scheduling
    
    private void startActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
    
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning) {
                return;
            }
            updateActionBar();
        }, 0L, 20L);
    }

    private void startTitleUpdates() {
        if (titleTask != null) titleTask.cancel();
        // Update titles a bit faster for near-immediate status feedback
        titleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            updateTitles();
        }, 0L, 5L); // 0.25s for snappier updates
    }
    
    private void updateActionBar() {
        if (!gameRunning || gamePaused) {
            return;
        }

        int timeLeft = getTimeUntilNextSwap();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!runners.contains(p)) {
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(p, "");
                continue;
            }
            if (p.equals(activeRunner)) {
                String msg = String.format("§eSwap in: §c%ds", Math.max(0, timeLeft));
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(p, msg);
            } else {
                int idx = runners.indexOf(p);
                int pos = Math.max(1, idx); // 1..n-1
                String queued = (pos == 1) ? "§6Queued §7(§e1§7) — You’re up next" : "§7Queued §7(§f" + pos + "§7)";
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(p, queued);
            }
        }
    }
    
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                // Remove cage if previously created
                removeCageFor(runner);
                PotionEffectType eff;
                if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null) runner.removePotionEffect(eff);
                runner.setGameMode(GameMode.SURVIVAL);
                // Ensure flight is disabled for the active runner to avoid anti-cheat confusion
                try { runner.setAllowFlight(false); } catch (Exception ignored) {}
                try { runner.setFlying(false); } catch (Exception ignored) {}
                
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, runner);
                }
            } else {
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null) runner.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType darkness = BukkitCompat.resolvePotionEffect("darkness");
                    if (darkness != null) runner.addPotionEffect(new PotionEffect(darkness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType slowness = BukkitCompat.resolvePotionEffect("slowness");
                    if (slowness != null) runner.addPotionEffect(new PotionEffect(slowness, Integer.MAX_VALUE, 255, false, false));
                    PotionEffectType slowFalling = BukkitCompat.resolvePotionEffect("slow_falling");
                    if (slowFalling != null) runner.addPotionEffect(new PotionEffect(slowFalling, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    runner.setGameMode(GameMode.SPECTATOR);
                } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                    Location limboLocation = plugin.getConfigManager().getLimboLocation();
                    // Try to find a safe nearby spot instead of blindly teleporting
                    Location safe = SafeLocationFinder.findSafeLocation(
                            limboLocation,
                            plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                            plugin.getConfigManager().getSafeSwapVerticalDistance(),
                            plugin.getConfigManager().getDangerousBlocks());
                    runner.teleport(safe != null ? safe : limboLocation);
                    runner.setGameMode(GameMode.ADVENTURE);
                    PotionEffectType blindness2 = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness2 != null) runner.addPotionEffect(new PotionEffect(blindness2, Integer.MAX_VALUE, 1, false, false));
                } else if (freezeMode.equalsIgnoreCase("CAGE")) {
                    // Teleport to a high-altitude bedrock cage and blind
                    createCageFor(runner);
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null) runner.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    runner.setGameMode(GameMode.ADVENTURE);
                    // Allow flight while caged to prevent server kicking for "flying"
                    try { runner.setAllowFlight(true); } catch (Exception ignored) {}
                    try { runner.setFlying(false); } catch (Exception ignored) {}
                }
                
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(runner)) {
                        viewer.hidePlayer(plugin, runner);
                    }
                }
            }
        }
    }
    
    // Removed: hunter freeze mechanic
    
    private void performSwap() {
        if (!gameRunning || gamePaused || runners.isEmpty()) {
            return;
        }

        // Persist the current active runner's state before swapping
        if (activeRunner != null && activeRunner.isOnline()) {
            savePlayerState(activeRunner);
        }

        // Rotate the queue: head -> tail until the next online runner is at index 0
        if (runners.isEmpty()) { pauseGame(); return; }
        int guard = 0;
        do {
            Player head = runners.remove(0);
            runners.add(head);
            guard++;
            if (guard > 64) {
                plugin.getLogger().warning("No online runners found during swap - pausing game");
                pauseGame();
                return;
            }
        } while (!runners.get(0).isOnline());

        Player nextRunner = runners.get(0);
        Player previousRunner = activeRunner;

        // Handle single-runner scenario gracefully: just refresh timers
        if (previousRunner != null && previousRunner.equals(nextRunner)) {
            // Keep the same active runner; just reschedule next swap and apply optional power-up
            scheduleNextSwap();
            return;
        }

        activeRunner = nextRunner;

        // Grace period for the new active runner
        int gracePeriodTicks = plugin.getConfigManager().getGracePeriodTicks();
        if (gracePeriodTicks > 0) {
            nextRunner.setInvulnerable(true);
            final Player finalNextRunner = nextRunner;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finalNextRunner.isOnline()) {
                    finalNextRunner.setInvulnerable(false);
                }
            }, gracePeriodTicks);
        }

        if (previousRunner != null && previousRunner.isOnline()) {
            // Capture full state (includes potion effects, XP, etc.) from the previous runner
            com.example.speedrunnerswap.models.PlayerState prevState = PlayerStateUtil.capturePlayerState(previousRunner);

            // Apply to the next runner
            PlayerStateUtil.applyPlayerState(nextRunner, prevState);

            // Teleport adjustment for safe swap near the previous runner's location
            if (plugin.getConfigManager().isSafeSwapEnabled()) {
                Location swapLocation = previousRunner.getLocation();
                Location safeLocation = SafeLocationFinder.findSafeLocation(
                        swapLocation,
                        plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                        plugin.getConfigManager().getSafeSwapVerticalDistance(),
                        plugin.getConfigManager().getDangerousBlocks());
                if (safeLocation != null) {
                    nextRunner.teleport(safeLocation);
                }
            }

            // Remove all active potion effects from the previous runner
            for (PotionEffect effect : previousRunner.getActivePotionEffects()) {
                previousRunner.removePotionEffect(effect.getType());
            }

            // Clear previous runner's inventory to prevent duplication exploits
            previousRunner.getInventory().clear();
            previousRunner.getInventory().setArmorContents(new ItemStack[]{});
            previousRunner.getInventory().setItemInOffHand(null);
            previousRunner.updateInventory();
        }

        applyInactiveEffects();
        scheduleNextSwap();

        // Suppress public chat broadcast on swap per request
    }

    /** Trigger an immediate runner swap (admin action) */
    public void triggerImmediateSwap() {
        if (!gameRunning || gamePaused) return;
        Bukkit.getScheduler().runTask(plugin, this::performSwap);
    }

    // Removed: hunter shuffle and power-up mechanics

    /**
     * Pause the game
     */
    public boolean pauseGame() {
        if (!gameRunning || gamePaused) {
            return false;
        }
        gamePaused = true;
        if (swapTask != null) {
            swapTask.cancel();
        }
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (titleTask != null) {
            titleTask.cancel();
        }
        return true;
    }

    /**
     * Resume the game
     */
    public boolean resumeGame() {
        if (!gameRunning || !gamePaused) {
            return false;
        }
        gamePaused = false;
        scheduleNextSwap();
        startActionBarUpdates();
        startTitleUpdates();
        startCageEnforcement();
        return true;
    }

    /** Returns whether the game is currently paused */
    public boolean isGamePaused() {
        return gamePaused;
    }

    /** Shuffle the runner queue while keeping the current active runner in front */
    public boolean shuffleQueue() {
        if (runners == null || runners.size() < 2) return false;
        Player current = activeRunner;
        java.util.List<Player> rest = new java.util.ArrayList<>();
        for (Player p : runners) {
            if (!p.equals(current)) rest.add(p);
        }
        java.util.Collections.shuffle(rest, new java.util.Random());
        java.util.List<Player> newOrder = new java.util.ArrayList<>();
        newOrder.add(current);
        newOrder.addAll(rest);
        runners = newOrder;
        applyInactiveEffects();
        refreshActionBar();
        return true;
    }

    /** Replace runners list and update config team names */
    public void setRunners(java.util.List<Player> players) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : players) names.add(p.getName());
        // Clear and set in config atomically
        plugin.getConfigManager().setRunnerNames(names);
        // Update runtime list
        this.runners = new java.util.ArrayList<>(players);
    }

    private void updateTitles() {
        int timeLeft = getTimeUntilNextSwap();
        Player current = activeRunner;
        boolean isSneak = current != null && current.isSneaking();
        boolean isSprint = current != null && current.isSprinting();

        net.kyori.adventure.text.Component sub = net.kyori.adventure.text.Component.text(
                String.format("Sneaking: %s  |  Running: %s", isSneak ? "Yes" : "No", isSprint ? "Yes" : "No"))
                .color(NamedTextColor.YELLOW);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!runners.contains(p)) continue; // Only runners get titles

            boolean isActive = p.equals(current);
            boolean shouldShow = !isActive; // waiting: always; active: never (use actionbar for last 10s)
            if (!shouldShow) continue;

            net.kyori.adventure.text.Component titleText = net.kyori.adventure.text.Component.text(
                    String.format("Swap in: %ds", Math.max(0, timeLeft)))
                    .color(isActive ? NamedTextColor.RED : NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);

            Title title = Title.title(
                    titleText,
                    sub,
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ZERO)
            );
            p.showTitle(title);
        }
    }

    private void createCageFor(Player runner) {
        if (runner == null || !runner.isOnline()) return;
        if (builtCages.containsKey(runner.getUniqueId())) return;

        // Build cage in the runner's current world to avoid cross-dimension edge cases
        Location base = plugin.getConfigManager().getLimboLocation();
        World world = runner.getWorld();
        int y = findSafeCageY(world, base.getBlockY());
        int index = Math.max(0, runners.indexOf(runner));
        int spacing = 10;
        int cx = (int) Math.round(base.getX()) + index * spacing;
        int cz = (int) Math.round(base.getZ());
        // Place center at the cage floor level (top of the bedrock floor),
        // so standing on the floor does not get treated as "outside" and yo-yo teleport.
        Location center = new Location(world, cx + 0.5, y, cz + 0.5);
        // Ensure surrounding chunks are loaded (3x3 around center)
        try {
            int ccx = center.getBlockX() >> 4;
            int ccz = center.getBlockZ() >> 4;
            for (int dcx = -1; dcx <= 1; dcx++) {
                for (int dcz = -1; dcz <= 1; dcz++) {
                    world.getChunkAt(ccx + dcx, ccz + dcz).load(true);
                }
            }
        } catch (Throwable ignored) {}

        java.util.List<org.bukkit.block.BlockState> changed = new java.util.ArrayList<>();
        // Build 5x5x5 cube of bedrock with 3x3x3 air cavity, plus extended floor to catch glitches
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    org.bukkit.block.Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    boolean isShell = (dx == -2 || dx == 2 || dz == -2 || dz == 2 || dy == -1 || dy == 3);
                    if (isShell) {
                        changed.add(block.getState());
                        block.setType(Material.BEDROCK, false);
                    } else {
                        changed.add(block.getState());
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }

        // Extended floor: 7x7 bedrock platform at y-1 to prevent falling off due to lag/glitch
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                org.bukkit.block.Block floor = world.getBlockAt(cx + dx, y - 1, cz + dz);
                changed.add(floor.getState());
                floor.setType(Material.BEDROCK, false);
            }
        }

        builtCages.put(runner.getUniqueId(), changed);
        cageCenters.put(runner.getUniqueId(), center.clone());
        // Teleport inside cage
        runner.teleport(center);
        // Guard against anti-fly kicks while caged
        try { runner.setAllowFlight(true); } catch (Exception ignored) {}
        try { runner.setFlying(false); } catch (Exception ignored) {}
    }

    private void removeCageFor(Player runner) {
        if (runner == null) return;
        java.util.List<org.bukkit.block.BlockState> states = builtCages.remove(runner.getUniqueId());
        cageCenters.remove(runner.getUniqueId());
        if (states != null) {
            for (org.bukkit.block.BlockState s : states) {
                try { s.update(true, false); } catch (Exception ignored) {}
            }
        }
    }

    // Pick a safe Y to build the 5x5x5 cage that avoids world min/max and nether ceiling issues
    private int findSafeCageY(World world, int preferredY) {
        int min = world.getMinHeight() + 6;  // allow shell thickness
        int max = world.getMaxHeight() - 6;
        int target = preferredY;
        if (target < min || target > max) {
            // Choose defaults by environment
            switch (world.getEnvironment()) {
                case NETHER -> target = Math.min(max, Math.max(min, 96));
                case THE_END -> target = Math.min(max, Math.max(min, 80));
                default -> target = Math.min(max, Math.max(min, max - 10));
            }
        }
        return target;
    }

    private void cleanupAllCages() {
        java.util.Set<java.util.UUID> ids = new java.util.HashSet<>(builtCages.keySet());
        for (java.util.UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) removeCageFor(p);
        }
        builtCages.clear();
        cageCenters.clear();
    }

    private void startCageEnforcement() {
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }
        cageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) return;

            for (Player r : runners) {
                if (r.equals(activeRunner)) continue;
                if (!r.isOnline()) continue;
                // If player's world changed, rebuild their cage in the new world
                org.bukkit.Location existing = cageCenters.get(r.getUniqueId());
                if (existing != null && existing.getWorld() != r.getWorld()) {
                    removeCageFor(r);
                }
                // Ensure cage exists
                createCageFor(r);
                org.bukkit.Location center = cageCenters.get(r.getUniqueId());
                if (center == null) continue;

                org.bukkit.Location loc = r.getLocation();
                double dx = Math.abs(loc.getX() - center.getX());
                double dy = loc.getY() - center.getY();
                double dz = Math.abs(loc.getZ() - center.getZ());
                // With center.y at floor top, allow a small tolerance above/below
                boolean outside = dx > 1.2 || dz > 1.2 || dy < -0.2 || dy > 2.8;
                if (outside) {
                    r.teleport(center);
                    r.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    r.setFallDistance(0f);
                    r.setNoDamageTicks(Math.max(10, r.getNoDamageTicks()));
                } else {
                    // Keep anti-fly protection while in cage
                    try { r.setAllowFlight(true); } catch (Exception ignored) {}
                    try { r.setFlying(false); } catch (Exception ignored) {}
                }
            }
        }, 0L, 5L); // enforce every 0.25s
    }
}
