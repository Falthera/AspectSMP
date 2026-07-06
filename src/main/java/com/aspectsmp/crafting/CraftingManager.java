package com.aspectsmp.crafting;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.items.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class CraftingManager {

    private final AspectSMP plugin;
    private final List<NamespacedKey> registeredRecipes;

    public CraftingManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.registeredRecipes = new ArrayList<>();
    }

    public void initialize() {
        registerRecipes();
    }

    private void registerRecipes() {
        registerHeartCatalyst();
        registerHeartResonator();
        registerHeartRestorationKit();
        registerReforgingCore();
        registerStabilityBottle();
    }

    private void registerHeartCatalyst() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "heart_catalyst"), new HeartCatalyst().createItemStack());
        recipe.shape("DED", "E E", "DED");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }

    private void registerHeartResonator() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "heart_resonator"), new HeartResonator().createItemStack());
        recipe.shape("ECE", "CTC", "ECE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('C', Material.END_CRYSTAL);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }

    private void registerHeartRestorationKit() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "heart_restoration_kit"), new HeartRestorationKit().createItemStack());
        recipe.shape("TGT", "NGN", "GGG");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }

    private void registerReforgingCore() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "reforging_core"), new ReforgingCore().createItemStack());
        recipe.shape("DAD", " A ", "DAD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }

    private void registerStabilityBottle() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "stability_bottle"), new StabilityBottle().createItemStack());
        recipe.shape("DDD", "DGD", "DDD");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('G', Material.GOLDEN_APPLE);
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }
}