package com.articreep.frostboats;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class Listeners implements Listener {
    final NamespacedKey frostWalkerKey = new NamespacedKey(FrostBoats.getPlugin(), "frost-walker-level");
    final NamespacedKey durabilityKey = new NamespacedKey(FrostBoats.getPlugin(), "durability");
    final NamespacedKey materialKey = new NamespacedKey(FrostBoats.getPlugin(), "material");
    final NamespacedKey displayNameKey = new NamespacedKey(FrostBoats.getPlugin(), "display-name");

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (event.getEntityType() == EntityType.BOAT || event.getEntityType() == EntityType.CHEST_BOAT) {

            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            Entity boat = event.getEntity();

            if (item.getEnchantments().isEmpty()) return;

            PersistentDataContainer itemContainer = item.getItemMeta().getPersistentDataContainer();
            PersistentDataContainer entityContainer = boat.getPersistentDataContainer();

            // Check enchantments for frost walker
            if (item.getEnchantments().containsKey(Enchantment.FROST_WALKER)) {

                // Add Frost Walker level to the Boat's PersistentDataContainer
                entityContainer.set(frostWalkerKey, PersistentDataType.INTEGER, item.getEnchantments().get(Enchantment.FROST_WALKER));

                // Does this boat have a durability value yet?
                if (itemContainer.has(durabilityKey, PersistentDataType.INTEGER)) {
                    // grab the value
                    entityContainer.set(durabilityKey, PersistentDataType.INTEGER,
                            itemContainer.get(durabilityKey, PersistentDataType.INTEGER));
                } else {
                    entityContainer.set(durabilityKey, PersistentDataType.INTEGER, FrostBoats.getMaxDurability());
                }

                // Does this boat have a custom display name?
                if (item.getItemMeta().hasDisplayName()) {
                    entityContainer.set(displayNameKey, PersistentDataType.STRING, item.getItemMeta().getDisplayName());
                }

                // Add the material type to the container.
                // The TreeSpecies enum is deprecated. If there's a better way, let me know!
                entityContainer.set(materialKey, PersistentDataType.STRING, item.getType().toString());


                // Since boats sink a little, spawn the boat just a little higher if spawned in water
                if (event.getBlock().getType() == Material.WATER) {
                    boat.teleport(boat.getLocation().add(0, 0.25, 0));
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

            // Check if the durability is already somehow zero. This can be caused by the config.
            // If so remove frost walker and do not send a message.
            int durability = container.get(durabilityKey, PersistentDataType.INTEGER);

            if (durability == 0) {
                container.set(frostWalkerKey, PersistentDataType.INTEGER, 0);
                boat.getWorld().playSound(boat.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
                return;
            }

            // Otherwise..
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

        // If the player is in creative mode don't worry about this. If there's a boat let it disappear
        if (event.getAttacker() instanceof Player p) {
            if (p.getGameMode() == GameMode.CREATIVE) {
                return;
            }
        }

        // Don't drop the vanilla boat otherwise
        event.setCancelled(true);

        Boat boat = (Boat) entity;
        World w = boat.getWorld();
        ItemStack item;
        Material material;
        PersistentDataContainer container = boat.getPersistentDataContainer();
        String displayName;

        // Construct an ItemStack with the proper material stored in the data container
        // Backwards "compatibility": If the key doesn't exist, drop an oak boat.
        if (container.get(materialKey, PersistentDataType.STRING) == null) {
            material = Material.OAK_BOAT;
        } else {
            material = Material.valueOf(container.get(materialKey, PersistentDataType.STRING));
        }

        if (entity.getPersistentDataContainer().get(frostWalkerKey, PersistentDataType.INTEGER) > 0) {

            // Ensure their frost walker is retained
            int frostLevel = container.get(frostWalkerKey, PersistentDataType.INTEGER);

            // Transfer the remaining durability to the item
            int durability = container.get(durabilityKey, PersistentDataType.INTEGER);

            // Transfer their boat's display name (if applicable)
            displayName = container.get(displayNameKey, PersistentDataType.STRING);

            item = FrostBoats.createFrostBoat(material, durability, frostLevel, displayName);

        } else {
            displayName = container.get(displayNameKey, PersistentDataType.STRING);
            item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }

        // Kill the existing boat
        boat.remove();

        // Give them their item back!
        w.dropItem(boat.getLocation(), item);
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
            // If no recipes are loaded the list will be empty and nothing will happen
            for (NamespacedKey key : FrostBoats.getRecipeKeys()) {
                if (recipe.getKey().equals(key)) {
                    e.getWhoClicked().getInventory().addItem(new ItemStack(Material.BUCKET, 2));
                    return;
                }
            }
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAnvilUse(PrepareAnvilEvent event) {

        if (!FrostBoats.canLoadAnvilRecipes()) return;

        AnvilInventory inventory = event.getInventory();

        // If there are not two materials in the anvil quit out
        ItemStack itemboat = inventory.getItem(0);
        ItemStack itembook = inventory.getItem(1);

        if (itemboat == null || itembook == null) return;

        // Check both items
        // Loop through a static list of boat Material types
        for (Material material : FrostBoats.materials) {
            if (itemboat.getType() == material && itembook.getType() == Material.ENCHANTED_BOOK) {

                // Book enchantments are stored in meta so have to go there.
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itembook.getItemMeta();

                if (meta.hasStoredEnchant(Enchantment.FROST_WALKER)) {
                    // What level?
                    int frostLevel = meta.getStoredEnchantLevel(Enchantment.FROST_WALKER);
                    ItemStack product = FrostBoats.createFrostBoat(itemboat.getType(), -1, frostLevel, inventory.getRenameText());
                    // Formula is base-cost * level, caps at 40
                    Bukkit.getScheduler().runTask(FrostBoats.getPlugin(), () -> {
                        int cost = FrostBoats.getBaseAnvilCost() * frostLevel;
                        if (cost > 39) cost = 39;
                        inventory.setRepairCost(cost);
                    });
                    event.setResult(product);

                }
                return;

            }
        }
    }
}
