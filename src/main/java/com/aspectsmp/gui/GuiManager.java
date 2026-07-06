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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiManager implements Listener {

    private final AspectSMP plugin;
    private final Map<Player, InventoryPage> playerPages;

    public GuiManager(AspectSMP plugin) {
        this.plugin = plugin;
        this.playerPages = new HashMap<>();
    }

    public void openHeartGUI(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player);
        if (heart == null) return;
        
        Inventory inv = Bukkit.createInventory(new HeartHolder(), 54, "§dYour Heart");
        
        ItemStack aspectItem = createAspectItem(heart.getAspect());
        inv.setItem(13, aspectItem);
        
        ItemStack tierItem = createTierItem(heart);
        inv.setItem(22, tierItem);
        
        ItemStack stabilityItem = createStabilityItem(heart);
        inv.setItem(31, stabilityItem);
        
        ItemStack essenceItem = createEssenceItem(heart);
        inv.setItem(40, essenceItem);
        
        ItemStack filler = createFiller();
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53}) {
            inv.setItem(i, filler);
        }
        
        playerPages.put(player, InventoryPage.HEART_OVERVIEW);
        player.openInventory(inv);
    }

    public void openAspectListGUI(Player player) {
        Inventory inv = Bukkit.createInventory(new AspectListHolder(), 54, "§bAspect Selection");
        
        ItemStack filler = createFiller();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        int slot = 10;
        for (AspectType aspect : AspectType.values()) {
            ItemStack item = createAspectItem(aspect);
            inv.setItem(slot, item);
            slot += 2;
            if (slot > 44) break;
        }
        
        playerPages.put(player, InventoryPage.ASPECT_LIST);
        player.openInventory(inv);
    }

    public void openAspectDetailGUI(Player player, AspectType aspect) {
        Heart heart = plugin.getHeartManager().getHeart(player);
        if (heart == null) return;
        
        Inventory inv = Bukkit.createInventory(new AspectDetailHolder(), 54, "§b" + aspect.getDisplayName() + " Aspect");
        
        ItemStack filler = createFiller();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        ItemStack aspectInfo = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta aspectMeta = aspectInfo.getItemMeta();
        aspectMeta.setDisplayName("§f" + aspect.getEmoji() + " §d" + aspect.getDisplayName() + " Aspect");
        List<Component> aspectLore = new ArrayList<>();
        aspectLore.add(Component.text("§7This aspect represents the power of " + aspect.getDisplayName() + "§7."));
        aspectLore.add(Component.text("§7Current: " + (heart.getAspect() == aspect ? "§a✓ Active" : "§c✗ Inactive")));
        aspectMeta.lore(aspectLore);
        aspectInfo.setItemMeta(aspectMeta);
        inv.setItem(13, aspectInfo);
        
        ItemStack passiveItem = createPassiveItem(aspect);
        inv.setItem(29, passiveItem);
        
        ItemStack tier1Item = createAbilityItem(aspect, 1);
        inv.setItem(31, tier1Item);
        
        ItemStack tier2Item = createAbilityItem(aspect, 2);
        inv.setItem(33, tier2Item);
        
        ItemStack tier3Item = createAbilityItem(aspect, 3);
        inv.setItem(35, tier3Item);
        
        ItemStack ultimateItem = createUltimateItem(aspect);
        inv.setItem(48, ultimateItem);
        
        ItemStack backButton = createBackButton();
        inv.setItem(49, backButton);
        
        playerPages.put(player, InventoryPage.ASPECT_DETAIL);
        player.openInventory(inv);
    }

    private ItemStack createAspectItem(AspectType aspect) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f" + aspect.getEmoji() + " §d" + aspect.getDisplayName() + " Aspect");
        meta.lore(java.util.List.of(Component.text("§7Click to view details")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTierItem(Heart heart) {
        Material mat = switch (heart.getTier()) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.DIAMOND;
            case 3 -> Material.NETHERITE_INGOT;
            default -> Material.STONE;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Tier " + heart.getTier());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Upgrade your Heart to unlock more abilities."));
        lore.add(Component.text("§7Right click for Tier 1 ability"));
        if (heart.getTier() >= 2) {
            lore.add(Component.text("§7Shift+Right click for Tier 2 ability"));
        }
        if (heart.getTier() >= 3) {
            lore.add(Component.text("§7Use /ability3 for Tier 3 ability"));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStabilityItem(Heart heart) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cStability: " + heart.getStability());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7State: " + heart.getStabilityState().getDisplayName()));
        lore.add(Component.text("§7Affects passive bonuses and cooldowns."));
        lore.add(Component.text("§7Gained from kills, lost from deaths."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEssenceItem(Heart heart) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bEssence: " + heart.getEssence());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Earned by damaging and killing players."));
        lore.add(Component.text("§7Used for future upgrades."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cBack to Overview");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPassiveItem(AspectType aspect) {
        String passiveName = getPassiveAbilityName(aspect);
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aPassive: " + passiveName);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Always active regardless of tier."));
        lore.add(Component.text("§7Effect varies based on stability."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAbilityItem(AspectType aspect, int tier) {
        String abilityName = getAbilityName(aspect, tier);
        ItemStack item = new ItemStack(tier == 1 ? Material.WOODEN_SWORD : tier == 2 ? Material.IRON_SWORD : Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Tier " + tier + " Ability: " + abilityName);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Tier " + tier + " ability for " + aspect.getDisplayName()));
        if (tier == 1) {
            lore.add(Component.text("§7Activation: Right Click"));
        } else if (tier == 2) {
            lore.add(Component.text("§7Activation: Shift + Right Click"));
        } else if (tier == 3) {
            lore.add(Component.text("§7Activation: /ability3 command"));
        }
        lore.add(Component.text("§7Cooldown varies by ability."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUltimateItem(AspectType aspect) {
        String ultimateName = getUltimateName(aspect);
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dUltimate: " + ultimateName);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Ultimate ability for " + aspect.getDisplayName()));
        lore.add(Component.text("§7Activation: /ultimate command"));
        lore.add(Component.text("§7Long cooldown - use wisely!"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getPassiveAbilityName(AspectType aspect) {
        return switch (aspect) {
            case INFERNO -> "Infernal Aura";
            case TIDE -> "Ocean's Embrace";
            case TEMPEST -> "Storm Surge";
            case RIFT -> "Dimensional Echo";
            case VITALITY -> "Life Force";
            case WAR -> "Battle Frenzy";
            case COSMOS -> "Stellar Alignment";
            case FORTUNE -> "Lucky Charm";
        };
    }

    private String getAbilityName(AspectType aspect, int tier) {
        return switch (aspect) {
            case INFERNO -> switch (tier) {
                case 1 -> "Flame Step";
                case 2 -> "Infernal Chains";
                case 3 -> "Lava Burst";
                default -> "Unknown";
            };
            case TIDE -> switch (tier) {
                case 1 -> "Hydro Push";
                case 2 -> "Tidal Prison";
                case 3 -> "Maelstrom";
                default -> "Unknown";
            };
            case TEMPEST -> switch (tier) {
                case 1 -> "Static Bolt";
                case 2 -> "Dash Chain";
                case 3 -> "Storm Rush";
                default -> "Unknown";
            };
            case RIFT -> switch (tier) {
                case 1 -> "Blink";
                case 2 -> "Void Swap";
                case 3 -> "Dimensional Tear";
                default -> "Unknown";
            };
            case VITALITY -> switch (tier) {
                case 1 -> "Heal Pulse";
                case 2 -> "Life Link";
                case 3 -> "Revival Shield";
                default -> "Unknown";
            };
            case WAR -> switch (tier) {
                case 1 -> "Berserk Slash";
                case 2 -> "Execution Chain";
                case 3 -> "Rage Mode";
                default -> "Unknown";
            };
            case COSMOS -> switch (tier) {
                case 1 -> "Gravity Pull";
                case 2 -> "Orbital Shield";
                case 3 -> "Meteor Call";
                default -> "Unknown";
            };
            case FORTUNE -> switch (tier) {
                case 1 -> "Ore Sense";
                case 2 -> "Mining Rush";
                case 3 -> "Lucky Strike";
                default -> "Unknown";
            };
        };
    }

    private String getUltimateName(AspectType aspect) {
        return switch (aspect) {
            case INFERNO -> "Sun Core Collapse";
            case TIDE -> "Maelstrom";
            case TEMPEST -> "Thunder Execution";
            case RIFT -> "Reality Collapse";
            case VITALITY -> "Genesis Field";
            case WAR -> "War God State";
            case COSMOS -> "Black Star Collapse";
            case FORTUNE -> "King's Jackpot";
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        
        event.setCancelled(true);
        
        InventoryPage currentPage = playerPages.get(player);
        if (currentPage == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        
        if (currentPage == InventoryPage.HEART_OVERVIEW) {
            if (clicked.getType() == Material.ENCHANTED_BOOK && 
                clicked.getItemMeta().getDisplayName().contains("Aspect")) {
                openAspectListGUI(player);
            }
        } else if (currentPage == InventoryPage.ASPECT_LIST) {
            if (clicked.getType() == Material.ENCHANTED_BOOK && 
                clicked.getItemMeta().getDisplayName().contains("Aspect")) {
                for (AspectType aspect : AspectType.values()) {
                    if (clicked.getItemMeta().getDisplayName().contains(aspect.getDisplayName())) {
                        openAspectDetailGUI(player, aspect);
                        return;
                    }
                }
            }
        } else if (currentPage == InventoryPage.ASPECT_DETAIL) {
            if (clicked.getType() == Material.ARROW && 
                clicked.getItemMeta().getDisplayName().equals("§cBack to Overview")) {
                openHeartGUI(player);
            }
        }
    }

    public enum InventoryPage {
        HEART_OVERVIEW, ASPECT_LIST, ASPECT_DETAIL, ABILITY_VIEW, UPGRADE, REForge
    }

    public static class HeartHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class AspectListHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class AspectDetailHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}