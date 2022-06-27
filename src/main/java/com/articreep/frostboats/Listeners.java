package com.articreep.frostboats;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class Listeners implements Listener {
    final NamespacedKey frostWalkerKey = new NamespacedKey(FrostBoats.getPlugin(), "frost-walker-level");
    final NamespacedKey durabilityKey = new NamespacedKey(FrostBoats.getPlugin(), "durability");
    final NamespacedKey materialKey = new NamespacedKey(FrostBoats.getPlugin(), "material");

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

                    // Add Frost Walker level to the Boat's PersistentDataContainer
                    entityContainer.set(frostWalkerKey, PersistentDataType.INTEGER, item.getEnchantments().get(enchantment));

                    // Does this boat have a durability value yet?
                    if (!itemContainer.has(durabilityKey, PersistentDataType.INTEGER)) {
                        entityContainer.set(durabilityKey, PersistentDataType.INTEGER, FrostBoats.getMaxDurability());
                    } else {
                        // grab the value
                        entityContainer.set(durabilityKey, PersistentDataType.INTEGER,
                                itemContainer.get(durabilityKey, PersistentDataType.INTEGER));
                    }

                    // Add the material type to the container.
                    // I would not have to do this if the TreeSpecies enum had mangrove
                    entityContainer.set(materialKey, PersistentDataType.STRING, item.getType().toString());


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
            double radius = 4.9 + container.get(frostWalkerKey, PersistentDataType.INTEGER);
            // Cap the radius
            if (radius > 15 && !FrostBoats.isRadiusUncapped()) radius = 15;
            double radiusSquared = radius * radius;

            Location center = boat.getLocation().clone().getBlock().getLocation().subtract(0, 1, 0);

            for (double x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (double z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
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


            // If the durability is negative don't decrement durability
            if (durability < 0) return;

            // Durability ran out?
            if (durability - 1 == 0) {
                for (Entity entity : boat.getPassengers()) {
                    entity.sendMessage(ChatColor.AQUA + "Your Frost Walker boat's durability has run out!");
                }
                // Remove Frost Walker
                container.set(frostWalkerKey, PersistentDataType.INTEGER, 0);
                boat.getWorld().playSound(boat.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
            // A warning when durability is low
            } else if (durability == Math.floor(FrostBoats.getMaxDurability() * 0.25)) {
                for (Entity entity : boat.getPassengers()) {
                    entity.sendMessage(ChatColor.AQUA + "Your Frost Walker boat has 25% durability remaining!");
                }
            }
            container.set(durabilityKey, PersistentDataType.INTEGER, durability - 1);
        }
    }

    @EventHandler
    public void onBoatEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player) {

            Entity boat = event.getVehicle();
            PersistentDataContainer container = boat.getPersistentDataContainer();

            if (!container.has(frostWalkerKey, PersistentDataType.INTEGER)) return;

            // Is it a FrostBoat?
            if (container.get(frostWalkerKey, PersistentDataType.INTEGER) > 0) {

                // Start a BukkitRunnable for the player
                new DurabilityBarRunnable(((Player) event.getEntered()).getPlayer()).runTaskTimer(FrostBoats.getPlugin(), 40, 1);
            }
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
            ItemStack item;
            Material material;
            PersistentDataContainer container = boat.getPersistentDataContainer();

            // Construct an ItemStack with the proper material stored in the data container
            // Backwards "compatibility": If the key doesn't exist, drop an oak boat.
            if (container.get(materialKey, PersistentDataType.STRING) == null) {
                material = Material.OAK_BOAT;
            } else {
                material = Material.valueOf(container.get(materialKey, PersistentDataType.STRING));
            }
            item = new ItemStack(material);

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
        // A quick check to ensure recipes are loaded
        if (FrostBoats.getRecipeKeys().isEmpty()) {
            FrostBoats.getPlugin().loadRecipes();
            return;
        }

        for (NamespacedKey key : FrostBoats.getRecipeKeys()) {
            e.getPlayer().discoverRecipe(key);
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
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
