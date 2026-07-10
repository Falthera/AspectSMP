package com.aspectsmp.items;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;

import java.util.List;

public class Cloudheart extends CustomItem {

    public Cloudheart() {
        super("cloudheart", "Cloudheart", "Token of the Ninth Sky");
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lCloudheart");
            meta.lore(List.of(
                Component.text("§7Token of the Ninth Sky."),
                Component.text("§7Hold this to wield the §bCloud Aspect§7."),
                Component.text("§cIf you lose it, the Aspect fades.")
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, "cloudheart");
            meta.addEnchant(Enchantment.WIND_BURST, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
