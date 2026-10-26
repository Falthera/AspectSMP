package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.DataComponentTypes;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class HeartRestorationKit extends CustomItem {

    public HeartRestorationKit() {
        super("heart_restoration_kit", "Heart Restoration Kit", "Restore a Dormant Heart");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lHeart Restoration Kit");
        setCustomModelDataString(item, "restoration");
        meta.lore(List.of(Component.text("\u00a77Restore a Dormant Heart to Stable"), Component.text("\u00a77Right-click to repair your Heart")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "heart_restoration_kit");
        item.setItemMeta(meta);
        return item;
    }

    private void setCustomModelDataString(ItemStack item, String modelDataString) {
        CustomModelDataComponent cmd = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd == null) {
            cmd = CustomModelDataComponent.customModelData().addString(modelDataString).build();
        } else {
            cmd = cmd.toBuilder().addString(modelDataString).build();
        }
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, cmd);
    }
}
