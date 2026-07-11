package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class StabilityBottle extends CustomItem {

    public static final NamespacedKey STABILITY_KEY = new NamespacedKey("aspectsmp", "stability_amount");

    public StabilityBottle() {
        super("stability_bottle", "Stability Bottle", "Restores 50 Stability to your Heart, capped at 100");
    }

    @Override
    public ItemStack createItemStack() {
        return createItemStack(50);
    }

    public ItemStack createItemStack(int stabilityAmount) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lStability Bottle");
            meta.lore(List.of(
                Component.text("§7Restores §a" + stabilityAmount + " Stability §7to your Heart"),
                Component.text("§7Cannot exceed §b100 Stability§7."),
                Component.text("§8Right-click to use")
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "stability_bottle");
            pdc.set(STABILITY_KEY, PersistentDataType.INTEGER, stabilityAmount);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static int getStoredStability(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(STABILITY_KEY, PersistentDataType.INTEGER)) return 0;
        return pdc.get(STABILITY_KEY, PersistentDataType.INTEGER);
    }
}
