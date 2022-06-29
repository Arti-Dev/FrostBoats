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

import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrostBoats extends JavaPlugin {
    private static FrostBoats plugin;
    private static final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private static int maxDurability = 1000;
    private static boolean radiusUncapped = false;
    private static int frostWalkerRecipeLevel = 2;
    public static final Material[] materials = {Material.ACACIA_BOAT, Material.BIRCH_BOAT, Material.DARK_OAK_BOAT, Material.JUNGLE_BOAT,
            Material.MANGROVE_BOAT, Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.ACACIA_CHEST_BOAT, Material.BIRCH_CHEST_BOAT,
            Material.DARK_OAK_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT, Material.MANGROVE_CHEST_BOAT, Material.OAK_CHEST_BOAT,
            Material.SPRUCE_CHEST_BOAT};
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
        maxDurability = getConfig().getInt("durability");
        radiusUncapped = getConfig().getBoolean("uncapradius");
        frostWalkerRecipeLevel = getConfig().getInt("frostwalkerrecipelevel");
        if (frostWalkerRecipeLevel <= 0 || frostWalkerRecipeLevel > 255) frostWalkerRecipeLevel = 2;


        loadRecipes();

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Get frosting! Plugin loaded.");
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Unloaded recipes!");
    }

    void loadRecipes() {

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

    void reload() {
        // Reload all recipes
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }

        // Reload config values
        reloadConfig();
        maxDurability = getConfig().getInt("durability");
        radiusUncapped = getConfig().getBoolean("uncapradius");
        frostWalkerRecipeLevel = getConfig().getInt("frostwalkerrecipelevel");
        if (frostWalkerRecipeLevel <= 0 || frostWalkerRecipeLevel > 255) frostWalkerRecipeLevel = 2;

        loadRecipes();

    }

    public static FrostBoats getPlugin() {
        return plugin;
    }

    public static List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public static boolean isRadiusUncapped() {
        return radiusUncapped;
    }

    public static int getMaxDurability() {
        return maxDurability;
    }

    public static ItemStack createFrostBoat(Material material, int durability, int level) {

        ItemStack product = new ItemStack(material);

        product.addUnsafeEnchantment(Enchantment.FROST_WALKER, level);

        ItemMeta meta = product.getItemMeta();
        meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + durability));
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER,
                durability);
        product.setItemMeta(meta);

        return product;
    }

    public static ItemStack createFrostBoat(Material material, int durability, int level, String name) {

        ItemStack product = new ItemStack(material);

        product.addUnsafeEnchantment(Enchantment.FROST_WALKER, level);

        ItemMeta meta = product.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + durability));
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER,
                durability);
        product.setItemMeta(meta);

        return product;
    }
}
