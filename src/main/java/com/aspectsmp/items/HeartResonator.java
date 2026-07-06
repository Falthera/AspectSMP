package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class HeartResonator extends CustomItem {

    public HeartResonator() {
        super("heart_resonator", "Heart Resonator", "Unlock Tier 3 abilities and Ultimate for your Heart");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lHeart Resonator");
        meta.lore(List.of("§7Unlock Tier 3 abilities and Ultimate", "§7Right-click to upgrade your Heart"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "heart_resonator");
        item.setItemMeta(meta);
        return item;
    }
}