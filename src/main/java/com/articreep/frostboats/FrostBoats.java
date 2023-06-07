package com.articreep.frostboats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class FrostBoats extends JavaPlugin {
    private static FrostBoats plugin;
    private static final List<NamespacedKey> recipeKeys = new ArrayList<>();

    // Config fields
    private static int maxDurability = 1000;
    private static boolean radiusUncapped = false;
    private static int frostWalkerRecipeLevel = 2;
    private static int baseAnvilCost = 12;
    private static boolean loadCraftingRecipes = true;
    private static boolean loadAnvilRecipes = true;
    private static boolean hideInfiniteDurability = false;

    public static final Set<Material> materials = Set.of(Material.ACACIA_BOAT, Material.BIRCH_BOAT, Material.DARK_OAK_BOAT, Material.JUNGLE_BOAT,
            Material.MANGROVE_BOAT, Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.ACACIA_CHEST_BOAT, Material.BIRCH_CHEST_BOAT,
            Material.DARK_OAK_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT, Material.MANGROVE_CHEST_BOAT, Material.OAK_CHEST_BOAT,
            Material.SPRUCE_CHEST_BOAT, Material.CHERRY_BOAT, Material.CHERRY_CHEST_BOAT, Material.BAMBOO_CHEST_RAFT, Material.BAMBOO_RAFT);
    private static NamespacedKey durabilityKey;


    @Override
    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(new Listeners(), this);
        getCommand("reloadfrostboats").setExecutor(new Reload());
        saveDefaultConfig();

        // Load NamespacedKey
        durabilityKey = new NamespacedKey(FrostBoats.getPlugin(), "durability");

        // Read from config file
        loadConfig();

        loadRecipes();

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "FrostBoats loaded");
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "FrostBoats recipes and plugin unloaded");
    }

    void loadRecipes() {

        if (!loadCraftingRecipes) return;

        for (Material material : materials) {

            // Create the key and make the recipe
            NamespacedKey key = new NamespacedKey(this, "frosted_" + material.toString().toLowerCase());
            recipeKeys.add(key);
            ShapedRecipe recipe = new ShapedRecipe(key, createFrostBoat(material, maxDurability, frostWalkerRecipeLevel));
            recipe.shape("   ", "PBP", "SSS");
            recipe.setIngredient('P', Material.POWDER_SNOW_BUCKET);
            recipe.setIngredient('B', material);
            recipe.setIngredient('S', Material.SNOW_BLOCK);

            // Register the recipe
            Bukkit.addRecipe(recipe);

            // Give everyone online the recipe when loaded if they don't already have it
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.discoverRecipe(key);
            }
        }
    }

    /**
     * Reloads config values and recipes.
     * Only to be used by the /reloadfrostboats command.
     */
    void reload() {
        // Unload all recipes
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }

        // Reload config values
        saveDefaultConfig();
        reloadConfig();
        loadConfig();

        // Finally load recipes
        loadRecipes();

    }

    private void loadConfig() {
        maxDurability = getConfig().getInt("durability");
        radiusUncapped = getConfig().getBoolean("uncapradius");
        frostWalkerRecipeLevel = getConfig().getInt("frostwalkerrecipelevel");
        if (frostWalkerRecipeLevel <= 0 || frostWalkerRecipeLevel > 255) frostWalkerRecipeLevel = 2;
        baseAnvilCost = getConfig().getInt("baseanvilcost");
        loadCraftingRecipes = getConfig().getBoolean("loadcraftingrecipes");
        loadAnvilRecipes = getConfig().getBoolean("loadanvilrecipes");
        hideInfiniteDurability = getConfig().getBoolean("hideinfinitelore");
    }

    public static FrostBoats getPlugin() {
        return plugin;
    }

    /**
     * Gets a list of all the FrostBoat recipe keys currently loaded.
     * If the recipes are not loaded due to the config the list will be empty!
     * @return A list of all FrostBoat recipe keys currently loaded
     */
    public static List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public static boolean isRadiusUncapped() {
        return radiusUncapped;
    }

    public static int getMaxDurability() {
        return maxDurability;
    }

    public static boolean canLoadAnvilRecipes() {
        return loadAnvilRecipes;
    }

    public static int getBaseAnvilCost() {
        return baseAnvilCost;
    }

    public static boolean shouldHideInfiniteDurability() {
        return hideInfiniteDurability;
    }

    /**
     * Constructs a FrostBoat ItemStack.
     * @param material The item material - must be a boat
     * @param durability The durability of the FrostBoat
     * @param level The level of Frost Walker to apply to the boat
     * @throws IllegalArgumentException if the item material is not a boat or a chest boat
     * @return the newly created Frost Boat ItemStack
     */
    public static ItemStack createFrostBoat(Material material, int durability, int level) throws IllegalArgumentException {

        if (!materials.contains(material)) throw new IllegalArgumentException("Material supplied is not a Boat or a Boat with Chest.");

        ItemStack product = new ItemStack(material);

        product.addUnsafeEnchantment(Enchantment.FROST_WALKER, level);

        ItemMeta meta = product.getItemMeta();

        if (durability < 0) {
            if (!hideInfiniteDurability) {
                meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + "∞"));
            }
        } else meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + durability));

        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER,
                durability);
        product.setItemMeta(meta);

        return product;
    }

    /**
     * Constructs a FrostBoat ItemStack with a name. This version is for the Anvil GUI.
     * @param material The item material - must be a boat
     * @param durability The durability of the FrostBoat
     * @param level The level of Frost Walker to apply to the boat
     * @param name The display name of the item
     * @throws IllegalArgumentException if the item material is not a boat or a chest boat
     * @return the newly created Frost Boat ItemStack
     */
    public static ItemStack createFrostBoat(Material material, int durability, int level, String name) {

        ItemStack product = new ItemStack(material);

        product.addUnsafeEnchantment(Enchantment.FROST_WALKER, level);

        ItemMeta meta = product.getItemMeta();
        meta.setDisplayName(name);

        if (durability < 0) {
            if (!hideInfiniteDurability) {
                meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + "∞"));
            }
        } else meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + durability));

        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER,
                durability);
        product.setItemMeta(meta);

        return product;
    }
}
