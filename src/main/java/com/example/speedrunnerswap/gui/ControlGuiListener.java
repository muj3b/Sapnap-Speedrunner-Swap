package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ControlGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;

    // Temporary selections per player for runner selector GUI
    private final Map<java.util.UUID, Set<String>> pendingRunnerSelections = new HashMap<>();

    public ControlGuiListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    private boolean isMain(String title) {
        return title != null && title.equals(plugin.getConfigManager().getGuiMainMenuTitle());
    }

    private boolean isRunnerSelector(String title) {
        return title != null && title.equals(plugin.getConfigManager().getGuiTeamSelectorTitle());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory view = event.getView().getTopInventory();
        if (view == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!isMain(title) && !isRunnerSelector(title)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (isMain(title)) {
            handleMainClick(player, clicked);
        } else if (isRunnerSelector(title)) {
            handleRunnerSelectorClick(player, clicked, event.getRawSlot(), view.getSize());
        }
    }

    private void handleMainClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        boolean running = plugin.getGameManager().isGameRunning();

        if (type == Material.LIME_WOOL) {
            if (!running) {
                // If no runners are configured, select all online players by default
                if (plugin.getConfigManager().getRunnerNames().isEmpty()) {
                    List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    plugin.getConfigManager().setRunnerNames(names);
                    plugin.getGameManager().setRunners(new ArrayList<>(Bukkit.getOnlinePlayers()));
                }
                plugin.getGameManager().startGame();
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.RED_WOOL) {
            if (running) plugin.getGameManager().stopGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.YELLOW_WOOL) {
            if (running && !plugin.getGameManager().isGamePaused()) plugin.getGameManager().pauseGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.ORANGE_WOOL) {
            if (running && plugin.getGameManager().isGamePaused()) plugin.getGameManager().resumeGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getGameManager().shuffleQueue();
            player.sendMessage("§aShuffled runner queue.");
            return;
        }

        if (type == Material.BOOK) {
            // Initialize pending selection with current config
            Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
            pendingRunnerSelections.put(player.getUniqueId(), initial);
            new ControlGui(plugin).openRunnerSelector(player);
            return;
        }

        if (type == Material.CLOCK) {
            // Distinguish which clock was clicked by its display name
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            if (name.startsWith("Runner Timer:")) {
                String current = plugin.getConfigManager().getRunnerTimerVisibility();
                String next = switch (current.toLowerCase()) {
                    case "always" -> "last_10";
                    case "last_10" -> "never";
                    default -> "always";
                };
                plugin.getConfigManager().setRunnerTimerVisibility(next);
                player.sendMessage("§eRunner timer visibility: §a" + next);
                plugin.getGameManager().refreshActionBar();
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (name.startsWith("Waiting Timer:")) {
                String current = plugin.getConfigManager().getWaitingTimerVisibility();
                String next = switch (current.toLowerCase()) {
                    case "always" -> "last_10";
                    case "last_10" -> "never";
                    default -> "always";
                };
                plugin.getConfigManager().setWaitingTimerVisibility(next);
                player.sendMessage("§eWaiting timer visibility: §a" + next);
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.ARMOR_STAND) {
            // Cycle freeze mode EFFECTS -> SPECTATOR -> LIMBO -> CAGE -> EFFECTS
            String mode = plugin.getConfigManager().getFreezeMode();
            String next = switch (mode.toUpperCase()) {
                case "EFFECTS" -> "SPECTATOR";
                case "SPECTATOR" -> "LIMBO";
                case "LIMBO" -> "CAGE";
                default -> "EFFECTS";
            };
            plugin.getConfigManager().setFreezeMode(next);
            player.sendMessage("§eInactive runner state: §a" + next);
            // Re-apply to all
            plugin.getGameManager().refreshActionBar();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.SLIME_BLOCK || type == Material.MAGMA_BLOCK) {
            boolean enabled = plugin.getConfigManager().isSafeSwapEnabled();
            plugin.getConfigManager().setSafeSwapEnabled(!enabled);
            player.sendMessage("§eSafe Swap: §a" + (!enabled));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.ARROW) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            int interval = plugin.getConfigManager().getSwapInterval();
            if (name.contains("-5")) interval -= 5; else if (name.contains("+5")) interval += 5;
            plugin.getConfigManager().setSwapInterval(interval);
            player.sendMessage("§eInterval set to: §a" + plugin.getConfigManager().getSwapInterval() + "s");
            // Refresh scheduling if running
            plugin.getGameManager().refreshSwapSchedule();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.COMPARATOR) {
            boolean randomize = plugin.getConfigManager().isSwapRandomized();
            plugin.getConfigManager().setSwapRandomized(!randomize);
            player.sendMessage("§eRandomize swaps: §a" + (!randomize));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.PAPER) {
            // Print status
            player.sendMessage("§6=== ControlSwap Status ===");
            player.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
            player.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
            if (plugin.getGameManager().isGameRunning()) {
                Player active = plugin.getGameManager().getActiveRunner();
                player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                player.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                String runners = plugin.getGameManager().getRunners().stream().map(Player::getName).collect(Collectors.joining(", "));
                player.sendMessage("§eRunners: §f" + runners);
            }
        }
    }

    private void handleRunnerSelectorClick(Player player, ItemStack clicked, int rawSlot, int size) {
        Material type = clicked.getType();
        // Bottom row buttons
        if (rawSlot >= size - 9) {
            if (type == Material.BARRIER) {
                // Discard
                pendingRunnerSelections.remove(player.getUniqueId());
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (type == Material.EMERALD_BLOCK) {
                // Apply
                Set<String> sel = pendingRunnerSelections.remove(player.getUniqueId());
                if (sel == null) sel = new HashSet<>();
                plugin.getConfigManager().setRunnerNames(new ArrayList<>(sel));
                List<Player> players = new ArrayList<>();
                for (String name : sel) {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null && p.isOnline()) players.add(p);
                }
                plugin.getGameManager().setRunners(players);
                player.sendMessage("§aRunners set: §f" + String.join(", ", sel));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Toggle player selection based on the head name
        if (type == Material.PLAYER_HEAD) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(clicked.getItemMeta().displayName());
            if (name == null || name.isBlank()) return;
            Set<String> sel = pendingRunnerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>(plugin.getConfigManager().getRunnerNames()));
            if (sel.contains(name)) sel.remove(name); else sel.add(name);
            // Refresh the selector GUI
            new ControlGui(plugin).openRunnerSelector(player);
        }
    }
}
