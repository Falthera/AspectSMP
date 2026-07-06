package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class ReforgingCore extends CustomItem {

    public ReforgingCore() {
        super("reforging_core", "Reforging Core", "Reroll your Aspect");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lReforging Core");
        meta.lore(List.of(Component.text("§7Reroll your Aspect to a new class"), Component.text("§7Recipe: Nether Star + 4 Diamonds + 4 Amethyst Shards")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "reforging_core");
        item.setItemMeta(meta);
        return item;
    }
}