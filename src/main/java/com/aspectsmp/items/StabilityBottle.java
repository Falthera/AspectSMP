package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class StabilityBottle extends CustomItem {

    public StabilityBottle() {
        super("stability_bottle", "Stability Bottle", "Restores 50 Stability to your Heart, capped at 100");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lStability Bottle");
        meta.lore(List.of(
            Component.text("§7Restores §a50 Stability §7to your Heart"),
            Component.text("§7Cannot exceed §b100 Stability§7."),
            Component.text("§8Right-click to use")
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "stability_bottle");
        item.setItemMeta(meta);
        return item;
    }
}
