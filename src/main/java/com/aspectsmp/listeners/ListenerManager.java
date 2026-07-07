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
import org.bukkit.event.block.Action;
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
        Heart heart = plugin.getHeartManager().getOrCreateHeart(player.getUniqueId());
        
        if (player.isOnline()) {
            player.sendMessage("§d§lWelcome to Aspect SMP!");
            player.sendMessage("§7You are bound to the §b" + heart.getAspect().getDisplayName() + " §7Heart.");
            player.sendMessage("§8Your Heart of the Sea will appear shortly...");
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                HeartOfTheSea.ensureBound(player);
                plugin.getScoreboardManager().updatePlayer(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                HeartOfTheSea.ensureBound(event.getPlayer());
                plugin.getScoreboardManager().updatePlayer(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(event.getPlayer().getUniqueId()).orElse(null);
        if (heart != null) {
            plugin.getHeartManager().saveHeart(heart);
        }
        plugin.getScoreboardManager().onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) return;
        
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        boolean holdingHeart = item != null && HeartOfTheSea.isHeartOfTheSea(item);

        if (holdingHeart) {
            event.setCancelled(true);
        }

        boolean isCustomItem = item != null && (
            CustomItem.isCustomItem(item, "heart_catalyst") || 
            CustomItem.isCustomItem(item, "heart_resonator") ||
            CustomItem.isCustomItem(item, "heart_restoration_kit") ||
            CustomItem.isCustomItem(item, "reforging_core") ||
            CustomItem.isCustomItem(item, "stability_bottle")
        );
        
        if (isCustomItem) {
            handleCustomItemUse(event, item);
            return;
        }

        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) {
            player.sendMessage("§cYour Heart is not available or is dormant!");
            return;
        }
        
        if (!holdingHeart) {
            player.sendMessage("§cHold your Heart of the Sea to use abilities!");
            return;
        }

        boolean sneaking = player.isSneaking();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
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
        boolean heartCurrent = current != null && HeartOfTheSea.isHeartOfTheSea(current);
        boolean heartCursor = cursor != null && HeartOfTheSea.isHeartOfTheSea(cursor);
        
        if (!heartCurrent && !heartCursor) return;
        
        if (event.getClickedInventory() != null && !(event.getClickedInventory().getHolder() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        boolean hasHeart = event.getNewItems().values().stream()
            .anyMatch(HeartOfTheSea::isHeartOfTheSea);
        if (!hasHeart) return;
        
        if (event.getInventory().getHolder() instanceof Player) {
            return;
        }
        
        event.setCancelled(true);
    }

    private void handleCustomItemUse(PlayerInteractEvent event, ItemStack item) {
        if (CustomItem.isCustomItem(item, "heart_catalyst")) {
            handleHeartCatalyst(event.getPlayer(), event);
        } else if (CustomItem.isCustomItem(item, "heart_resonator")) {
            handleHeartResonator(event.getPlayer(), event);
        } else if (CustomItem.isCustomItem(item, "heart_restoration_kit")) {
            handleHeartRestorationKit(event.getPlayer(), event);
        } else if (CustomItem.isCustomItem(item, "reforging_core")) {
            handleReforgingCore(event.getPlayer(), event);
        } else if (CustomItem.isCustomItem(item, "stability_bottle")) {
            handleStabilityBottle(event.getPlayer(), event);
        }
    }

    private void consumeOneItem(Player player, PlayerInteractEvent event) {
        ItemStack used = event.getItem();
        if (used == null) return;
        int newAmount = used.getAmount() - 1;
        if (newAmount <= 0) {
            if (event.getHand() == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            used.setAmount(newAmount);
        }
    }

    private void handleHeartCatalyst(Player player, PlayerInteractEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (heart.getTier() >= 2) {
            player.sendMessage("§cAlready at Tier 2 or higher!");
            return;
        }
        if (heart.getEssence() < 20) {
            player.sendMessage("§cYou need 20 Essence to upgrade!");
            return;
        }
        heart.setTier(2);
        heart.addEssence(-20);
        player.sendMessage("§6Heart upgraded to Tier 2!");
        consumeOneItem(player, event);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleHeartResonator(Player player, PlayerInteractEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (heart.getTier() >= 3) {
            player.sendMessage("§cAlready at Tier 3!");
            return;
        }
        if (heart.getTier() < 2) {
            player.sendMessage("§cYou need Tier 2 first!");
            return;
        }
        if (heart.getEssence() < 50) {
            player.sendMessage("§cYou need 50 Essence to upgrade!");
            return;
        }
        heart.setTier(3);
        heart.addEssence(-50);
        player.sendMessage("§dHeart upgraded to Tier 3!");
        consumeOneItem(player, event);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleHeartRestorationKit(Player player, PlayerInteractEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (!heart.isDormant()) {
            player.sendMessage("§cHeart is not dormant!");
            return;
        }
        if (heart.getEssence() < 30) {
            player.sendMessage("§cYou need 30 Essence to restore your Heart!");
            return;
        }
        heart.setDormant(false);
        heart.setStability(50);
        heart.addEssence(-30);
        player.sendMessage("§bHeart restored!");
        consumeOneItem(player, event);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleReforgingCore(Player player, PlayerInteractEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        
        AspectType current = heart.getAspect();
        java.util.List<AspectType> choices = new java.util.ArrayList<>(java.util.List.of(AspectType.values()));
        choices.remove(current);
        AspectType newAspect = choices.get(new java.util.Random().nextInt(choices.size()));
        
        heart.setAspect(newAspect);
        heart.setTier(1);
        heart.getCooldowns().clear();
        
        player.sendMessage("§b§lYour Heart has been reforged!");
        player.sendMessage("§7Old Aspect: §f" + current.getDisplayName());
        player.sendMessage("§7New Aspect: §f" + newAspect.getDisplayName());
        player.sendMessage("§7Tier reset to 1.");
        
        consumeOneItem(player, event);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleStabilityBottle(Player player, PlayerInteractEvent event) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        if (heart.getStability() >= 100) {
            player.sendMessage("§cYour Stability is already full!");
            return;
        }
        if (heart.getEssence() < 10) {
            player.sendMessage("§cYou need 10 Essence to use this!");
            return;
        }
        heart.addStability(50);
        heart.addEssence(-10);
        player.sendMessage("§aStability restored by 50!");
        consumeOneItem(player, event);
        plugin.getScoreboardManager().updatePlayer(player);
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
