package com.kurb.shepherd.managers;

import com.kurb.shepherd.items.ChargeItem;
import com.kurb.shepherd.items.StaffItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class RecipeManager {

    private final JavaPlugin plugin;

    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        registerStaffRecipe();
        registerChargeRecipe();
    }

    private void registerStaffRecipe() {
        // copper ingot | amethyst shard | gold ingot
        //      -       | redstone torch |     -
        //      -       |    diamond     |     -
        NamespacedKey key = new NamespacedKey(plugin, "staff_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, StaffItem.create());
        recipe.shape("CAG", " T ", " D ");
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('T', Material.REDSTONE_TORCH);
        recipe.setIngredient('D', Material.DIAMOND);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerChargeRecipe() {
        //      -       | amethyst shard |     -
        //    carrot    |  redstone dust |   carrot
        //      -       |       -        |     -
        NamespacedKey key = new NamespacedKey(plugin, "charge_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, ChargeItem.create());
        recipe.shape(" A ", "CRC", "   ");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('C', Material.CARROT);
        recipe.setIngredient('R', Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }
}
