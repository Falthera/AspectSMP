package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Color;
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
        CustomModelData cmd = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        CustomModelData.Builder builder = CustomModelData.customModelData();
        if (cmd != null) {
            for (String s : cmd.strings()) builder.addString(s);
            for (Float f : cmd.floats()) builder.addFloat(f);
            for (Boolean b : cmd.flags()) builder.addFlag(b);
            for (org.bukkit.Color c : cmd.colors()) builder.addColor(c);
        }
        builder.addString(modelDataString);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, builder.build());
    }
}
