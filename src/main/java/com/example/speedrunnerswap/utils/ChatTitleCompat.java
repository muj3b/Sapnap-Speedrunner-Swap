package com.example.speedrunnerswap.utils;

import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Title and chat compatibility helpers for Paper/Spigot with fallbacks.
 */
public final class ChatTitleCompat {
    private ChatTitleCompat() {}

    /** Show a title using Adventure API if present, else Player#sendTitle. Durations in milliseconds. */
    public static void showTitle(Player player, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        if (player == null) return;
        // Try Player#showTitle(Title) with Adventure types
        try {
            Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
            Class<?> titleCls = Class.forName("net.kyori.adventure.title.Title");
            Class<?> timesCls = Class.forName("net.kyori.adventure.title.Title$Times");
            java.lang.reflect.Method text = comp.getMethod("text", String.class);
            Object titleComp = text.invoke(null, title != null ? title : "");
            Object subComp = text.invoke(null, subtitle != null ? subtitle : "");
            java.lang.reflect.Method times = timesCls.getMethod("times", Duration.class, Duration.class, Duration.class);
            Object timesObj = times.invoke(null, Duration.ofMillis(fadeInMs), Duration.ofMillis(stayMs), Duration.ofMillis(fadeOutMs));
            java.lang.reflect.Method makeTitle = titleCls.getMethod("title", comp, comp, timesCls);
            Object t = makeTitle.invoke(null, titleComp, subComp, timesObj);
            java.lang.reflect.Method show = player.getClass().getMethod("showTitle", titleCls);
            show.invoke(player, t);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback: legacy sendTitle(String, String, int, int, int) (ticks)
        try {
            int fi = (int) Math.max(0, fadeInMs / 50);
            int st = (int) Math.max(0, stayMs / 50);
            int fo = (int) Math.max(0, fadeOutMs / 50);
            player.sendTitle(title != null ? title : "", subtitle != null ? subtitle : "", fi, st, fo);
        } catch (Throwable ignored) {
        }
    }

    /** Send a chat line; tries Component path first then falls back to plain String. */
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        // Try Player#sendMessage(Component)
        try {
            Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
            java.lang.reflect.Method text = comp.getMethod("text", String.class);
            Object component = text.invoke(null, message);
            java.lang.reflect.Method send = player.getClass().getMethod("sendMessage", comp);
            send.invoke(player, component);
            return;
        } catch (Throwable ignored) {
        }
        player.sendMessage(message);
    }
}

