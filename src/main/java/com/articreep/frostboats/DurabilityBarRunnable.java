package com.articreep.frostboats;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class DurabilityBarRunnable extends BukkitRunnable {
    Player player;
    final NamespacedKey durabilityKey = new NamespacedKey(FrostBoats.getPlugin(), "durability");

    public DurabilityBarRunnable(Player player) {
        this.player = player;
    }

    @Override
    public void run() {

        if (!player.isInsideVehicle()) {
            this.cancel();
            return;
        }

        Entity boat = player.getVehicle();

        PersistentDataContainer container = boat.getPersistentDataContainer();

        if (!container.has(durabilityKey, PersistentDataType.INTEGER)) this.cancel();

        int durability = container.get(durabilityKey, PersistentDataType.INTEGER);

        // If durability is below 0 it is infinite, send a special message
        if (durability < 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new ComponentBuilder("Frost Walker Durability: ")
                            .append("Infinite!").color(ChatColor.GREEN).create());
            return;
        }

        // Choose the color of the message
        ChatColor color;
        if (durability > FrostBoats.getMaxDurability() * 0.5) color = ChatColor.GREEN;
        else if (durability > FrostBoats.getMaxDurability() * 0.25) color = ChatColor.YELLOW;
        else color = ChatColor.RED;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new ComponentBuilder("Frost Walker Durability: ")
                        .append(Integer.toString(durability)).color(color).create());

        if (durability == 0) this.cancel();

    }
}
