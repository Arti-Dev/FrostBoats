package com.articreep.frostboats;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostBoats extends JavaPlugin {
    private static FrostBoats plugin;

    @Override
    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(new Listeners(), this);
        getLogger().info("Get frosting! Plugin loaded.");

    }

    @Override
    public void onDisable() {
        getLogger().info("Goodbye!");
    }

    public static FrostBoats getPlugin() {
        return plugin;
    }
}
