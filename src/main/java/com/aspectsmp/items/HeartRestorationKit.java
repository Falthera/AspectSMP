package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class HeartRestorationKit extends CustomItem {

    public HeartRestorationKit() {
        super("heart_restoration_kit", "Heart Restoration Kit", "Restore a Dormant Heart");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lHeart Restoration Kit");
        meta.lore(List.of("§7Restore a Dormant Heart to Stable", "§7Right-click to repair your Heart"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "heart_restoration_kit");
        item.setItemMeta(meta);
        return item;
    }
}