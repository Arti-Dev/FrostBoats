package com.articreep.frostboats;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Reload implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        FrostBoats.getPlugin().reload();
        sender.sendMessage(ChatColor.AQUA + "Recipes and config reloaded");
        return true;
    }
}
