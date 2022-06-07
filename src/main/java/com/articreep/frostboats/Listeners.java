package com.articreep.frostboats;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class Listeners implements Listener {

    public final static HashMap<Integer, Integer> frostBoats = new HashMap<>();

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (event.getEntityType() == EntityType.BOAT) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

            if (item.getEnchantments().isEmpty()) return;

            // Check enchantments for frost walker
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                if (enchantment.equals(Enchantment.FROST_WALKER)) {

                    // Cool, it's a frost walker boat.
                    Boat boat = (Boat) event.getEntity();
                    // Map boat entity ID to Frost Walker Level
                    frostBoats.put(boat.getEntityId(), item.getEnchantments().get(enchantment));

                    // Since boats sink a little, spawn the boat just a little higher if spawned in water
                    if (event.getBlock().getType() == Material.WATER) {
                        boat.teleport(boat.getLocation().add(0, 1, 0));
                    }
                    return;

                }
            }
        }
    }

    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {

        // Is it a frostboat?
        if (frostBoats.containsKey(event.getVehicle().getEntityId())) {

            Boat boat = (Boat) event.getVehicle();

            // Get the blocks underneath the boat - nested for loops
            int radius = 2 + frostBoats.get(boat.getEntityId());
            // Cap the radius
            if (radius > 15) radius = 15;


            Location center = boat.getLocation().clone().subtract(0, 1, 0);

            // Should iterate over 25 blocks
            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {

                    Block block = new Location(center.getWorld(), x, center.getY(), z).getBlock();

                    // If it's a water block change it to ice.
                    if (block.getType() == Material.WATER) {
                        block.setType(Material.FROSTED_ICE);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBoatDestroy(VehicleDestroyEvent event) {
        if (frostBoats.containsKey(event.getVehicle().getEntityId())) {
            Boat boat = (Boat) event.getVehicle();

            // If the player is in creative mode don't worry about this, let the boat just disappear
            if (event.getAttacker() instanceof Player) {
                Player p = (Player) event.getAttacker();
                if (p.getGameMode() == GameMode.CREATIVE) {
                    frostBoats.remove(boat.getEntityId());
                    return;
                }
            }

            // Don't drop the vanilla boat otherwise
            event.setCancelled(true);

            World w = boat.getWorld();
            ItemStack item = null;

            // Make sure we're giving the right boat type back
            // TODO Update for 1.19
            switch (boat.getWoodType()) {
                case ACACIA:
                    item = new ItemStack(Material.ACACIA_BOAT);
                    break;
                case BIRCH:
                    item = new ItemStack(Material.BIRCH_BOAT);
                    break;
                case JUNGLE:
                    item = new ItemStack(Material.JUNGLE_BOAT);
                    break;
                case GENERIC:
                    item = new ItemStack(Material.OAK_BOAT);
                    break;
                case REDWOOD:
                    item = new ItemStack(Material.SPRUCE_BOAT);
                    break;
                case DARK_OAK:
                    item = new ItemStack(Material.DARK_OAK_BOAT);
                    break;
            }

            // Ensure their frost walker is retained
            item.addUnsafeEnchantment(Enchantment.FROST_WALKER, frostBoats.get(boat.getEntityId()));

            // Remove from the Map
            frostBoats.remove(boat.getEntityId());

            // Kill the existing boat
            boat.remove();

            // Give them their item back!
            w.dropItem(boat.getLocation(), item);

        }
    }
}
