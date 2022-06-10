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
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrostBoats extends JavaPlugin {
    private static FrostBoats plugin;
    private static final List<NamespacedKey> recipeKeys = new ArrayList<>();
    public static final int maxDurability = 1000;

    @Override
    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(new Listeners(), this);

        // Recipe time!
        Material[] materials = {Material.ACACIA_BOAT, Material.BIRCH_BOAT, Material.DARK_OAK_BOAT, Material.JUNGLE_BOAT,
                Material.MANGROVE_BOAT, Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.ACACIA_CHEST_BOAT, Material.BIRCH_CHEST_BOAT,
                Material.DARK_OAK_CHEST_BOAT, Material.JUNGLE_CHEST_BOAT, Material.MANGROVE_CHEST_BOAT, Material.OAK_CHEST_BOAT, Material.SPRUCE_CHEST_BOAT};

        for (Material material : materials) {
            // Create the product of the recipe
            ItemStack product = new ItemStack(material);
            product.addUnsafeEnchantment(Enchantment.FROST_WALKER, 2);
            ItemMeta meta = product.getItemMeta();
            meta.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Durability: " + maxDurability));
            product.setItemMeta(meta);

            // Create the key and make the recipe
            NamespacedKey key = new NamespacedKey(this, "frosted_" + material.toString().toLowerCase());
            recipeKeys.add(key);
            ShapedRecipe recipe = new ShapedRecipe(key, product);
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

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Get frosting! Plugin loaded.");
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Unloaded recipes!");
    }

    public static FrostBoats getPlugin() {
        return plugin;
    }

    public static List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }
}
