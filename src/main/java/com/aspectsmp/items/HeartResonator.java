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

public class HeartResonator extends CustomItem {

    public HeartResonator() {
        super("heart_resonator", "Heart Resonator", "Unlock Tier 3 abilities and Ultimate for your Heart");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d\u00a7lHeart Resonator");
        setCustomModelDataString(item, "resonator");
        meta.lore(List.of(Component.text("\u00a77Unlock Tier 3 abilities and Ultimate"), Component.text("\u00a77Right-click to upgrade your Heart")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "heart_resonator");
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
