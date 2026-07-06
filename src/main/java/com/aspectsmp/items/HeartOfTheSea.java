package com.aspectsmp.items;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class HeartOfTheSea {

    public static final NamespacedKey HEART_OF_THE_SEA_KEY = new NamespacedKey("aspectsmp", "heart_of_the_sea");
    public static final NamespacedKey BOUND_PLAYER_KEY = new NamespacedKey("aspectsmp", "bound_player");

    public static ItemStack create(Player player, AspectType aspect) {
        ItemStack item = new ItemStack(org.bukkit.Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dHeart of the Sea §7(" + aspect.getDisplayName() + " Heart)");
            meta.lore(java.util.List.of(
                Component.text("§7This heart pulses with the power of the §b" + aspect.getDisplayName() + " Aspect§7."),
                Component.text("§8Bound to " + player.getName())
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(HEART_OF_THE_SEA_KEY, PersistentDataType.STRING, "heart_of_the_sea");
            pdc.set(BOUND_PLAYER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void updateAspectName(ItemStack item, AspectType aspect) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(HEART_OF_THE_SEA_KEY, PersistentDataType.STRING)) return;
        
        meta.setDisplayName("§dHeart of the Sea §7(" + aspect.getDisplayName() + " Heart)");
        meta.lore(java.util.List.of(
            Component.text("§7This heart pulses with the power of the §b" + aspect.getDisplayName() + " Aspect§7."),
            Component.text("§8Bound to your soul")
        ));
        item.setItemMeta(meta);
    }

    public static boolean isHeartOfTheSea(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(HEART_OF_THE_SEA_KEY, PersistentDataType.STRING);
    }

    public static boolean isBoundToPlayer(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(BOUND_PLAYER_KEY, PersistentDataType.STRING)) return false;
        String ownerStr = pdc.get(BOUND_PLAYER_KEY, PersistentDataType.STRING);
        return ownerStr != null && ownerStr.equals(player.getUniqueId().toString());
    }

    public static void ensureBound(Player player) {
        Heart heart = AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;

        boolean hasHeart = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isHeartOfTheSea(item) && isBoundToPlayer(item, player)) {
                hasHeart = true;
                updateAspectName(item, heart.getAspect());
                break;
            }
        }

        if (!hasHeart) {
            ItemStack heartItem = create(player, heart.getAspect());
            player.getInventory().addItem(heartItem);
            player.updateInventory();
        }
    }
}
