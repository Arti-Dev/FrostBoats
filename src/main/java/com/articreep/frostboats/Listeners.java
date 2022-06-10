package com.articreep.frostboats;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class Listeners implements Listener {
    NamespacedKey frostWalkerKey = new NamespacedKey(FrostBoats.getPlugin(), "frost-walker-level");
    NamespacedKey durabilityKey = new NamespacedKey(FrostBoats.getPlugin(), "durability");

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (event.getEntityType() == EntityType.BOAT || event.getEntityType() == EntityType.CHEST_BOAT) {

            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            Entity boat = event.getEntity();

            if (item.getEnchantments().isEmpty()) return;

            PersistentDataContainer itemContainer = item.getItemMeta().getPersistentDataContainer();
            PersistentDataContainer entityContainer = boat.getPersistentDataContainer();

            // Check enchantments for frost walker
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                if (enchantment.equals(Enchantment.FROST_WALKER)) {

                    // Cool, it's a frost walker boat.

                    // Add Frost Walker level to the Boat's PersistentDataContainer
                    entityContainer.set(frostWalkerKey, PersistentDataType.INTEGER, item.getEnchantments().get(enchantment));

                    // Does this boat have a durability value yet?
                    if (!itemContainer.has(durabilityKey, PersistentDataType.INTEGER)) {
                        entityContainer.set(durabilityKey, PersistentDataType.INTEGER, FrostBoats.maxDurability);
                    } else {
                        // grab the value
                        entityContainer.set(durabilityKey, PersistentDataType.INTEGER,
                                itemContainer.get(durabilityKey, PersistentDataType.INTEGER));
                    }


                    // Since boats sink a little, spawn the boat just a little higher if spawned in water
                    if (event.getBlock().getType() == Material.WATER) {
                        boat.teleport(boat.getLocation().add(0, 0.25, 0));
                    }
                    return;

                }
            }
        }
    }

    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {

        Entity boat = event.getVehicle();
        PersistentDataContainer container = boat.getPersistentDataContainer();
        boolean hasGeneratedIce = false;

        if (!container.has(frostWalkerKey, PersistentDataType.INTEGER)) return;

        // Is it a frostboat?
        if (container.get(frostWalkerKey, PersistentDataType.INTEGER) > 0) {

            // Spawn a cute particle
            boat.getLocation().getWorld().spawnParticle(Particle.SNOWFLAKE, boat.getLocation(), 1, 0, 0, 0, 0);

            // Get the blocks underneath the boat - nested for loops
            int radius = 5 + container.get(frostWalkerKey, PersistentDataType.INTEGER);
            // Cap the radius
            if (radius > 15) radius = 15;
            int radiusSquared = radius * radius;

            Location center = boat.getLocation().clone().getBlock().getLocation().subtract(0, 1, 0);

            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                    Location l = new Location(center.getWorld(), x, center.getY(), z);
                    if (l.distanceSquared(center) > radiusSquared) continue;
                    Block block = l.getBlock();

                    // If it's a water block AND there is air above change it to ice.
                    if (block.getType() == Material.WATER) {
                        if (l.add(0, 1, 0).getBlock().getType() == Material.AIR) {
                            block.setType(Material.FROSTED_ICE);
                            // Indicate that the boat has frosted ice
                            if (!hasGeneratedIce) hasGeneratedIce = true;
                        }
                    }
                }
            }

            // After all this is done, decrement the durability of the boat down by 1.
            // unless it didn't frost anything, in that case just return as there's nothing else to do
            if (!hasGeneratedIce) return;

            int durability = container.get(durabilityKey, PersistentDataType.INTEGER);
            // Durability ran out?
            if (durability - 1 <= 0) {
                for (Entity entity : boat.getPassengers()) {
                    entity.sendMessage(ChatColor.AQUA + "Your boat's Frost Durability has run out!");
                }
                // Bye bye, Frost Walker.
                container.set(frostWalkerKey, PersistentDataType.INTEGER, 0);
                boat.getWorld().playSound(boat.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);

            } else if (durability == 250) {
                for (Entity entity : boat.getPassengers()) {
                    entity.sendMessage(ChatColor.AQUA + "Your Frost Walker boat has 250 durability remaining!");
                }
            }
            container.set(durabilityKey, PersistentDataType.INTEGER, durability - 1);
        }
    }

    @EventHandler
    public void onBoatDestroy(VehicleDestroyEvent event) {
        Entity entity = event.getVehicle();
        if (!entity.getPersistentDataContainer().has(frostWalkerKey, PersistentDataType.INTEGER)) return;

        if (entity.getPersistentDataContainer().get(frostWalkerKey, PersistentDataType.INTEGER) > 0) {
            Boat boat = (Boat) entity;

            // If the player is in creative mode don't worry about this, let the boat just disappear
            if (event.getAttacker() instanceof Player p) {
                if (p.getGameMode() == GameMode.CREATIVE) {
                    return;
                }
            }

            // Don't drop the vanilla boat otherwise
            event.setCancelled(true);

            World w = boat.getWorld();
            ItemStack item = null;
            PersistentDataContainer container = boat.getPersistentDataContainer();

            // Make sure we're giving the right boat type back
            // TODO Apparently mangrove boats are of TreeSpecies.GENERIC. Weird.
            if (boat instanceof ChestBoat) {
                item = switch (boat.getWoodType()) {
                    case ACACIA -> new ItemStack(Material.ACACIA_CHEST_BOAT);
                    case BIRCH -> new ItemStack(Material.BIRCH_CHEST_BOAT);
                    case JUNGLE -> new ItemStack(Material.JUNGLE_CHEST_BOAT);
                    case GENERIC -> new ItemStack(Material.OAK_CHEST_BOAT);
                    case REDWOOD -> new ItemStack(Material.SPRUCE_CHEST_BOAT);
                    case DARK_OAK -> new ItemStack(Material.DARK_OAK_CHEST_BOAT);
                };
            } else {
                item = switch (boat.getWoodType()) {
                    case ACACIA -> new ItemStack(Material.ACACIA_BOAT);
                    case BIRCH -> new ItemStack(Material.BIRCH_BOAT);
                    case JUNGLE -> new ItemStack(Material.JUNGLE_BOAT);
                    case GENERIC -> new ItemStack(Material.OAK_BOAT);
                    case REDWOOD -> new ItemStack(Material.SPRUCE_BOAT);
                    case DARK_OAK -> new ItemStack(Material.DARK_OAK_BOAT);
                };
            }

            // Ensure their frost walker is retained
            item.addUnsafeEnchantment(Enchantment.FROST_WALKER, container.get(frostWalkerKey, PersistentDataType.INTEGER));

            // Transfer the remaining durability to the item
            int durability = container.get(durabilityKey, PersistentDataType.INTEGER);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER,
                    durability);

            // Add a line in the lore that says how much durability is left
            meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + durability));

            // Apply the ItemMeta
            item.setItemMeta(meta);

            // Kill the existing boat
            boat.remove();

            // Give them their item back!
            w.dropItem(boat.getLocation(), item);

        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        for (NamespacedKey key : FrostBoats.getRecipeKeys()) {
            e.getPlayer().discoverRecipe(key);
        }
    }

    @EventHandler
    public void onBoatCraft(CraftItemEvent e) {
        if (e.getRecipe() instanceof ShapedRecipe recipe) {
            for (NamespacedKey key : FrostBoats.getRecipeKeys()) {
                if (recipe.getKey().equals(key)) {
                    e.getWhoClicked().getInventory().addItem(new ItemStack(Material.BUCKET, 2));
                }
            }
        }
    }
}
