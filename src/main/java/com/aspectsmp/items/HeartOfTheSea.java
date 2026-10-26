package com.aspectsmp.items;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.DataComponentTypes;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class HeartOfTheSea {

    public static final NamespacedKey HEART_OF_THE_SEA_KEY = new NamespacedKey("aspectsmp", "heart_of_the_sea");
    public static final NamespacedKey BOUND_PLAYER_KEY = new NamespacedKey("aspectsmp", "bound_player");

    private static String getCustomModelDataString(AspectType aspect) {
        return switch (aspect) {
            case INFERNO -> "heart_inferno";
            case TIDE -> "heart_tide";
            case TEMPEST -> "heart_tempest";
            case RIFT -> "heart_rift";
            case VITALITY -> "heart_vitality";
            case WAR -> "heart_war";
            case COSMOS -> "heart_cosmos";
            case FORTUNE -> "heart_fortune";
            case CLOUD -> "heart_cloud";
            case WINTER -> "heart_winter";
            case SHADOW -> "heart_shadow";
            case STORM -> "heart_storm";
            default -> "heart_base"; // BASE
        };
    }

    private static void setCustomModelDataString(ItemStack item, String modelDataString) {
        CustomModelDataComponent cmd = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd == null) {
            cmd = CustomModelDataComponent.customModelData().addString(modelDataString).build();
        } else {
            cmd = cmd.toBuilder().addString(modelDataString).build();
        }
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, cmd);
    }

    public static ItemStack create(Player player, AspectType aspect) {
        ItemStack item = new ItemStack(org.bukkit.Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dHeart of the Sea §7(" + aspect.getDisplayName() + " Heart)");
            setCustomModelDataString(item, getCustomModelDataString(aspect));
            Heart heart = AspectSMP.getInstance().getHeartManager().getHeart(player.getUniqueId()).orElse(null);
            int stability = heart != null ? heart.getStability() : 100;
            meta.lore(java.util.List.of(
                Component.text("§7This heart pulses with the power of the §b" + aspect.getDisplayName() + " Aspect§7."),
                Component.text("§8Bound to " + player.getName()),
                Component.text("§cStability: " + stability)
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
        setCustomModelDataString(item, getCustomModelDataString(aspect));
        String ownerName = pdc.get(BOUND_PLAYER_KEY, PersistentDataType.STRING);
        Heart heart = null;
        if (ownerName != null) {
            try {
                heart = AspectSMP.getInstance().getHeartManager().getHeart(java.util.UUID.fromString(ownerName)).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }
        int stability = heart != null ? heart.getStability() : 100;
        meta.lore(java.util.List.of(
            Component.text("§7This heart pulses with the power of the §b" + aspect.getDisplayName() + " Aspect§7."),
            Component.text("§8Bound to your soul"),
            Component.text("§cStability: " + stability)
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
        Heart heart = AspectSMP.getInstance().getHeartManager().getOrCreateHeart(player.getUniqueId());
        if (heart == null) return;

        boolean foundAny = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isHeartOfTheSea(item)) {
                foundAny = true;
                break;
            }
        }
        if (!foundAny) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (isHeartOfTheSea(offHand)) {
                foundAny = true;
            }
        }

        if (foundAny) {
            for (int i = 0; i < player.getInventory().getContents().length; i++) {
                ItemStack item = player.getInventory().getContents()[i];
                if (isHeartOfTheSea(item)) {
                    player.getInventory().setItem(i, null);
                }
            }
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (isHeartOfTheSea(offHand)) {
                player.getInventory().setItemInOffHand(null);
            }
        }

        ItemStack heartItem = create(player, heart.getAspect());
        player.getInventory().addItem(heartItem);
        player.updateInventory();
    }
}
