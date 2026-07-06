package com.aspectsmp.gui;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiManager implements Listener {

    private final AspectSMP plugin;
    private final Map<Player, InventoryPage> playerPages;

    public GuiManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.playerPages = new java.util.HashMap<>();
    }

    public void openHeartGUI(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player);
        Inventory inv = Bukkit.createInventory(new HeartHolder(), 54, "§dYour Heart");
        
        ItemStack aspectDisplay = createDisplayItem(heart);
        inv.setItem(13, aspectDisplay);
        
        ItemStack tierDisplay = createTierDisplay(heart);
        inv.setItem(22, tierDisplay);
        
        ItemStack stabilityDisplay = createStabilityDisplay(heart);
        inv.setItem(31, stabilityDisplay);
        
        ItemStack essenceDisplay = createEssenceDisplay(heart);
        inv.setItem(40, essenceDisplay);
        
        playerPages.put(player, InventoryPage.HEART_OVERVIEW);
        player.openInventory(inv);
    }

    public void openAspectListGUI(Player player) {
        Inventory inv = Bukkit.createInventory(new AspectListHolder(), 54, "§bAspect Selection");
        
        int slot = 10;
        for (AspectType aspect : AspectType.values()) {
            ItemStack item = createAspectItem(aspect);
            inv.setItem(slot, item);
            slot += 2;
        }
        
        player.openInventory(inv);
    }

    private ItemStack createDisplayItem(Heart heart) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f" + heart.getAspect().getDisplayName() + " Heart");
        meta.lore(java.util.List.of(Component.text("§7Tier: " + heart.getTier()), Component.text("§7Stability: " + heart.getStability())));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTierDisplay(Heart heart) {
        Material mat = switch (heart.getTier()) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.DIAMOND;
            case 3 -> Material.NETHERITE_INGOT;
            default -> Material.STONE;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Tier " + heart.getTier());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStabilityDisplay(Heart heart) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cStability: " + heart.getStability());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7State: " + heart.getStabilityState().getDisplayName()));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEssenceDisplay(Heart heart) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bEssence: " + heart.getEssence());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAspectItem(AspectType aspect) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(aspect.getEmoji() + " " + aspect.getDisplayName());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        
        event.setCancelled(true);
    }

    public enum InventoryPage {
        HEART_OVERVIEW, ASPECT_LIST, ABILITY_VIEW, UPGRADE, REForge
    }

    public static class HeartHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class AspectListHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}