package com.aspectsmp.listeners;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.items.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ListenerManager implements Listener {

    private final AspectSMP plugin;

    public ListenerManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getHeartManager().getOrCreateHeart(player.getUniqueId());
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            HeartOfTheSea.ensureBound(player);
        }, 5L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            HeartOfTheSea.ensureBound(event.getPlayer());
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(event.getPlayer().getUniqueId()).orElse(null);
        if (heart != null) {
            plugin.getHeartManager().saveHeart(heart);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;

        if (HeartOfTheSea.isHeartOfTheSea(item)) {
            event.setCancelled(true);
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        if (CustomItem.isCustomItem(item, "heart_catalyst") || 
            CustomItem.isCustomItem(item, "heart_resonator") ||
            CustomItem.isCustomItem(item, "heart_restoration_kit") ||
            CustomItem.isCustomItem(item, "reforging_core")) {
            handleCustomItemUse(event, item);
            return;
        }

        Player player = event.getPlayer();
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;

        boolean sneaking = player.isSneaking();
        boolean rightClick = event.getAction().toString().contains("RIGHT_CLICK");

        if (rightClick) {
            if (!sneaking && heart.getTier() >= 1) {
                plugin.getAbilityManager().handleTier1Ability(player, heart);
            } else if (sneaking && heart.getTier() >= 2) {
                plugin.getAbilityManager().handleTier2Ability(player, heart);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (HeartOfTheSea.isHeartOfTheSea(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        if (current != null && HeartOfTheSea.isHeartOfTheSea(current)) {
            event.setCancelled(true);
        }
        
        if (cursor != null && HeartOfTheSea.isHeartOfTheSea(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        for (ItemStack item : event.getNewItems().values()) {
            if (HeartOfTheSea.isHeartOfTheSea(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleCustomItemUse(PlayerInteractEvent event, ItemStack item) {
        if (CustomItem.isCustomItem(item, "heart_catalyst")) {
            handleHeartCatalyst(event.getPlayer());
        } else if (CustomItem.isCustomItem(item, "heart_resonator")) {
            handleHeartResonator(event.getPlayer());
        } else if (CustomItem.isCustomItem(item, "heart_restoration_kit")) {
            handleHeartRestorationKit(event.getPlayer());
        } else if (CustomItem.isCustomItem(item, "reforging_core")) {
            handleReforgingCore(event.getPlayer());
        }
    }

    private void handleHeartCatalyst(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (heart.getTier() >= 2) {
            player.sendMessage("§cAlready at Tier 2 or higher!");
            return;
        }
        heart.setTier(2);
        player.sendMessage("§6Heart upgraded to Tier 2!");
        player.getInventory().getItemInMainHand().setAmount(
            player.getInventory().getItemInMainHand().getAmount() - 1);
    }

    private void handleHeartResonator(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (heart.getTier() >= 3) {
            player.sendMessage("§cAlready at Tier 3!");
            return;
        }
        heart.setTier(3);
        player.sendMessage("§dHeart upgraded to Tier 3!");
        player.getInventory().getItemInMainHand().setAmount(
            player.getInventory().getItemInMainHand().getAmount() - 1);
    }

    private void handleHeartRestorationKit(Player player) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (!heart.isDormant()) {
            player.sendMessage("§cHeart is not dormant!");
            return;
        }
        heart.setDormant(false);
        heart.setStability(50);
        player.sendMessage("§bHeart restored!");
        player.getInventory().getItemInMainHand().setAmount(
            player.getInventory().getItemInMainHand().getAmount() - 1);
    }

    private void handleReforgingCore(Player player) {
        player.sendMessage("§cReforging Core not implemented yet!");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player) {
            Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
            if (heart == null || heart.isDormant()) return;
            
            heart.addEssence((long) (event.getDamage() * 2));
        }
    }

    @EventHandler
    public void onPlayerKill(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player killer) {
            Heart heart = plugin.getHeartManager().getHeart(killer.getUniqueId()).orElse(null);
            if (heart == null || heart.isDormant()) return;
            
            heart.addStability(5);
            heart.addEssence(10);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
                if (heart != null && !heart.isDormant()) {
                    heart.addStability(-10);
                }
            }
        }
    }
}
