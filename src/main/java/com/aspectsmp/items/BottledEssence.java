package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class BottledEssence extends CustomItem {

    public BottledEssence() {
        super("bottled_essence", "Bottled Essence", "Portable essence currency");
    }

    public ItemStack createItemStack(long amount) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f§lBottled Essence");
        meta.lore(List.of(Component.text("§7Essence: §e" + amount), Component.text("§7Right-click to extract/inject")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "bottled_essence");
        pdc.set(ESSENCE_STORED_KEY, PersistentDataType.LONG, amount);
        item.setItemMeta(meta);
        return item;
    }

    public static long getStoredEssence(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0L;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(ESSENCE_STORED_KEY, PersistentDataType.LONG, 0L);
    }
}