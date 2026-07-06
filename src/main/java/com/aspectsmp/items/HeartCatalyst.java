package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class HeartCatalyst extends CustomItem {

    public HeartCatalyst() {
        super("heart_catalyst", "Heart Catalyst", "Unlock Tier 2 abilities for your Heart");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lHeart Catalyst");
        meta.lore(List.of(Component.text("§7Unlock Tier 2 abilities"), Component.text("§7Right-click to upgrade your Heart")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "heart_catalyst");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createRecipeResult() {
        return new HeartCatalyst().createItemStack();
    }
}