package com.articreep.frostboats.frostboats;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class Listeners implements Listener {

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (event.getEntityType() == EntityType.BOAT) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

            if (item.getEnchantments().isEmpty()) return;
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                if (enchantment.equals(Enchantment.FROST_WALKER)) {
                    // Cool, it's a frost walker boat.
                }
            }
        }
    }
}
