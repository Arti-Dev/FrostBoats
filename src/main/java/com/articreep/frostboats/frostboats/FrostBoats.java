package com.articreep.frostboats.frostboats;

import org.bukkit.plugin.java.JavaPlugin;

public final class FrostBoats extends JavaPlugin {
    private FrostBoats plugin;

    @Override
    public void onEnable() {
        plugin = this;
        // A listener to listen for boats being placed
        // A listener to listen for boats being broken

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public FrostBoats getPlugin() {
        return plugin;
    }
}
