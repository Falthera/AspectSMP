package com.aspectsmp.items;

import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

public class CustomItem {

    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("aspectsmp", "item_id");
    public static final NamespacedKey OWNER_KEY = new NamespacedKey("aspectsmp", "owner");
    public static final NamespacedKey ESSENCE_STORED_KEY = new NamespacedKey("aspectsmp", "essence_stored");

    private final String id;
    private final String displayName;
    private final String description;

    public CustomItem(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + displayName);
            meta.lore(java.util.List.of(Component.text("§7" + description)));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isCustomItem(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(ITEM_ID_KEY, PersistentDataType.STRING) && 
               id.equals(pdc.get(ITEM_ID_KEY, PersistentDataType.STRING));
    }

    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public static void setOwner(ItemStack item, UUID owner) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(OWNER_KEY, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
    }

    public static UUID getOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String ownerStr = pdc.get(OWNER_KEY, PersistentDataType.STRING);
        return ownerStr != null ? UUID.fromString(ownerStr) : null;
    }
}