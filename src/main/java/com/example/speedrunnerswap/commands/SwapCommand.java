package com.example.speedrunnerswap.commands;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SwapCommand implements CommandExecutor, TabCompleter {
    
    private final SpeedrunnerSwap plugin;
    
    public SwapCommand(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                return handleMainCommand(sender);
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "start":
                    return handleStart(sender);
                case "stop":
                    return handleStop(sender);
                case "pause":
                    return handlePause(sender);
                case "resume":
                    return handleResume(sender);
                case "status":
                    return handleStatus(sender);
                case "shuffle":
                    return handleShuffle(sender);
                case "maker":
                    return handleMaker(sender);
                case "setrunners":
                    return handleSetRunners(sender, Arrays.copyOfRange(args, 1, args.length));
                case "reload":
                    return handleReload(sender);
                case "gui":
                    return handleMainCommand(sender);
                case "clearteams":
                    return handleClearTeams(sender);
                default:
                    sender.sendMessage("§cUnknown subcommand. Use /swap for help.");
                    return false;
            }
        } catch (Exception e) {
            // Catch any unexpected errors so Bukkit doesn't show the generic message without a stacktrace
            sender.sendMessage("§cAn internal error occurred while executing that command. Check server logs for details.");
            plugin.getLogger().log(Level.SEVERE, "Unhandled exception while executing /swap by " + (sender == null ? "UNKNOWN" : sender.getName()), e);
            return false;
        }
    }

    private boolean handleMainCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return false;
        }

        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }

        // Print quick help since GUI is removed in ControlSwap
        sender.sendMessage("§6ControlSwap Commands:");
        sender.sendMessage("§e/swap setrunners <names>§7 — set runners");
        sender.sendMessage("§e/swap start|stop|pause|resume§7 — control game");
        sender.sendMessage("§e/swap status§7 — show status");
        sender.sendMessage("§e/swap reload§7 — reload config");
        return true;
    }
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is already running.");
            return false;
        }
        
        boolean success = plugin.getGameManager().startGame();
        if (success) {
            sender.sendMessage("§aGame started successfully.");
        } else {
            sender.sendMessage("§cFailed to start the game. Make sure there are runners set.");
        }
        
        return success;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        plugin.getGameManager().stopGame();
        sender.sendMessage("§aGame stopped.");
        
        return true;
    }
    
    private boolean handlePause(CommandSender sender) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        if (plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("§cThe game is already paused.");
            return false;
        }
        
        boolean success = plugin.getGameManager().pauseGame();
        if (success) {
            sender.sendMessage("§aGame paused.");
        } else {
            sender.sendMessage("§cFailed to pause the game.");
        }
        
        return success;
    }
    
    private boolean handleResume(CommandSender sender) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        if (!plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("§cThe game is not paused.");
            return false;
        }
        
        boolean success = plugin.getGameManager().resumeGame();
        if (success) {
            sender.sendMessage("§aGame resumed.");
        } else {
            sender.sendMessage("§cFailed to resume the game.");
        }
        
        return success;
    }
    
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        sender.sendMessage("§6=== ControlSwap Status ===");
        sender.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
        sender.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
        
        if (plugin.getGameManager().isGameRunning()) {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            sender.sendMessage("§eActive Runner: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
            sender.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            List<Player> runners = plugin.getGameManager().getRunners();
            
            sender.sendMessage("§eRunners: §f" + runners.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", ")));
            
            // No hunters in ControlSwap
        }
        
        return true;
    }

    private boolean handleShuffle(CommandSender sender) {
        if (!sender.hasPermission("controlswap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }

        boolean ok = plugin.getGameManager().shuffleQueue();
        if (ok) {
            sender.sendMessage("§aShuffled runner queue (active stays active).");
        } else {
            sender.sendMessage("§cNot enough runners to shuffle.");
        }
        return ok;
    }

    private boolean handleMaker(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Created by muj3b — donate: " + plugin.getConfig().getString("donation.url", "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01"));
            return true;
        }

        Player player = (Player) sender;

        // Chat message with clickable donate link
        String donateUrl = plugin.getConfig().getString("donation.url", "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01");
        net.kyori.adventure.text.Component header = net.kyori.adventure.text.Component.text("ControlSwap created by muj3b")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
        net.kyori.adventure.text.Component donate = net.kyori.adventure.text.Component.text("❤ Click to Donate")
                .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("Open donation page")))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(donateUrl));
        player.sendMessage(header);
        player.sendMessage(donate);

        // Open a small About GUI with a creator head in the top-right corner
        try {
            new com.example.speedrunnerswap.gui.AboutGui(plugin).openFor(player);
        } catch (Throwable t) {
            // ignore if GUI fails for any reason
        }
        return true;
    }
    
    private boolean handleSetRunners(CommandSender sender, String[] playerNames) {
        if (!sender.hasPermission("controlswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (playerNames.length == 0) {
            sender.sendMessage("§cUsage: /swap setrunners <player1> [player2] [player3] ...");
            return false;
        }
        
        List<Player> players = new ArrayList<>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null) {
                players.add(player);
            } else {
                sender.sendMessage("§cPlayer not found: " + name);
            }
        }
        
        if (players.isEmpty()) {
            sender.sendMessage("§cNo valid players specified.");
            return false;
        }
        
        plugin.getGameManager().setRunners(players);
        sender.sendMessage("§aRunners set: " + players.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")));
        
        return true;
    }
    
    // No hunters in ControlSwap
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("controlswap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        // Stop the game if it's running
        if (plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }
        
        // Reload the config
        plugin.getConfigManager().loadConfig();
        sender.sendMessage("§aConfiguration reloaded.");
        
        return true;
    }

    private boolean handleClearTeams(CommandSender sender) {
        if (!sender.hasPermission("controlswap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }

        // Stop the game if it's running
        if (plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }

        plugin.getGameManager().setRunners(new ArrayList<>());
        sender.sendMessage("§aCleared all runners.");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subCommands = Arrays.asList("start", "stop", "pause", "resume", "status", "shuffle", "maker", "setrunners", "reload", "gui", "clearteams");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            // Player names for setrunners
            if (args[0].equalsIgnoreCase("setrunners")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String name = player.getName();
                    if (name.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        }
        
        return completions;
    }
}
