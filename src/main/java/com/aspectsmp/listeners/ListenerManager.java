package com.aspectsmp.listeners;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.AspectType;
import com.aspectsmp.core.Heart;
import com.aspectsmp.items.*;
import com.aspectsmp.core.RuleModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ListenerManager implements Listener {

    private final AspectSMP plugin;
    private final Map<UUID, Map<UUID, Set<PotionEffectType>>> activeTrustEffectMap = new ConcurrentHashMap<>();

    public ListenerManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Map<UUID, Map<UUID, Set<PotionEffectType>>> newTrustEffectMap = new ConcurrentHashMap<>();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    plugin.getAbilityManager().processPassives(player);
                    Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
                    if (heart == null || heart.isDormant()) continue;
                    RuleModifier modifier = plugin.getRuleModifierManager().getModifier(heart.getAspect());
                    if (modifier != null) {
                        modifier.applyPassive(player);
                    }
                    
                    for (UUID trusted : plugin.getTrustManager().getTrusted(player.getUniqueId())) {
                        Player ally = plugin.getServer().getPlayer(trusted);
                        if (ally == null || !ally.isOnline()) continue;
                        if (ally.getLocation().distanceSquared(player.getLocation()) <= 100) {
                            applyTrustBuffs(ally, heart.getAspect());
                            newTrustEffectMap.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                                .put(trusted, getEffectsForAspect(heart.getAspect()));
                        }
                    }
                }
                
                for (Map.Entry<UUID, Map<UUID, Set<PotionEffectType>>> outerEntry : activeTrustEffectMap.entrySet()) {
                    UUID thrusterUuid = outerEntry.getKey();
                    Map<UUID, Set<PotionEffectType>> oldAllyMap = outerEntry.getValue();
                    Map<UUID, Set<PotionEffectType>> newAllyMap = newTrustEffectMap.get(thrusterUuid);
                    
                    if (newAllyMap == null) {
                        for (Map.Entry<UUID, Set<PotionEffectType>> entry : oldAllyMap.entrySet()) {
                            Player ally = plugin.getServer().getPlayer(entry.getKey());
                            if (ally != null && ally.isOnline()) {
                                for (PotionEffectType type : entry.getValue()) {
                                    ally.removePotionEffect(type);
                                }
                            }
                        }
                        continue;
                    }
                    
                    for (Map.Entry<UUID, Set<PotionEffectType>> entry : oldAllyMap.entrySet()) {
                        UUID allyUuid = entry.getKey();
                        if (!newAllyMap.containsKey(allyUuid)) {
                            Player ally = plugin.getServer().getPlayer(allyUuid);
                            if (ally != null && ally.isOnline()) {
                                for (PotionEffectType type : entry.getValue()) {
                                    ally.removePotionEffect(type);
                                }
                            }
                        }
                    }
                }
                
                activeTrustEffectMap.clear();
                for (Map.Entry<UUID, Map<UUID, Set<PotionEffectType>>> outer : newTrustEffectMap.entrySet()) {
                    activeTrustEffectMap.put(outer.getKey(), new ConcurrentHashMap<>(outer.getValue()));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private java.util.Set<PotionEffectType> getEffectsForAspect(AspectType aspect) {
        return switch (aspect) {
            case INFERNO -> java.util.Set.of(PotionEffectType.FIRE_RESISTANCE, PotionEffectType.STRENGTH);
            case TIDE -> java.util.Set.of(PotionEffectType.WATER_BREATHING, PotionEffectType.DOLPHINS_GRACE);
            case TEMPEST -> java.util.Set.of(PotionEffectType.SPEED);
            case RIFT -> java.util.Set.of(PotionEffectType.NIGHT_VISION);
            case VITALITY -> java.util.Set.of(PotionEffectType.REGENERATION);
            case WAR -> java.util.Set.of(PotionEffectType.STRENGTH);
            case COSMOS -> java.util.Set.of(PotionEffectType.RESISTANCE);
            case FORTUNE -> java.util.Set.of(PotionEffectType.LUCK, PotionEffectType.HERO_OF_THE_VILLAGE);
            case CLOUD -> java.util.Set.of(PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST);
            case WINTER -> java.util.Set.of(PotionEffectType.RESISTANCE, PotionEffectType.SLOWNESS);
        };
    }
    
    private void applyTrustBuffs(Player ally, AspectType aspect) {
        switch (aspect) {
            case INFERNO -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 80, 0), true);
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 80, 0), true);
            }
            case TIDE -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 80, 0), true);
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 80, 0), true);
            }
            case TEMPEST -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 80, 0), true);
            }
            case RIFT -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 80, 0), true);
            }
            case VITALITY -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 80, 0), true);
            }
            case WAR -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 80, 0), true);
            }
            case COSMOS -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 80, 0), true);
            }
            case FORTUNE -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 80, 0), true);
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE, 80, 0), true);
            }
            case CLOUD -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 80, 0), true);
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 80, 0), true);
            }
            case WINTER -> {
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 80, 0), true);
                ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 80, 0), true);
            }
        }
    }
    
    private void removeTrustBuffs(Player player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.LUCK);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
    }

    public void clearAspectEffects(Player player) {
        removeTrustBuffs(player);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
        player.removeMetadata("berserker", com.aspectsmp.AspectSMP.getInstance());
        player.removeMetadata("rift_blink_cd", com.aspectsmp.AspectSMP.getInstance());
        player.removeMetadata("vitality_bloom", com.aspectsmp.AspectSMP.getInstance());
        player.removeMetadata("golden_touch", com.aspectsmp.AspectSMP.getInstance());
        player.removeMetadata("revival_shield", com.aspectsmp.AspectSMP.getInstance());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Heart heart = plugin.getHeartManager().getOrCreateHeart(player.getUniqueId());
        
        if (heart.getAspect() == AspectType.CLOUD && !heart.isCloudUnlocked()) {
            AspectType[] nonCloud = java.util.Arrays.stream(AspectType.values())
                    .filter(a -> a != AspectType.CLOUD)
                    .toArray(AspectType[]::new);
            AspectType random = nonCloud[new java.util.Random().nextInt(nonCloud.length)];
            heart.setAspect(random);
            heart.setCloudUnlocked(false);
            player.sendMessage("§cThe Aspect of the Cloud has been removed. Win the Ninth Sky event to unlock it.");
        }
        
        if (heart.getAspect() == AspectType.WINTER && !heart.isWinterUnlocked()) {
            AspectType[] nonWinter = java.util.Arrays.stream(AspectType.values())
                    .filter(a -> a != AspectType.WINTER)
                    .toArray(AspectType[]::new);
            AspectType random = nonWinter[new java.util.Random().nextInt(nonWinter.length)];
            heart.setAspect(random);
            heart.setWinterUnlocked(false);
            player.sendMessage("§cThe Aspect of Winter has been removed. Win the Frozen Eclipse event to unlock it.");
        }
        
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
        Player player = event.getPlayer();
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart != null) {
            plugin.getHeartManager().saveHeart(heart);
        }
        plugin.getScoreboardManager().onPlayerQuit(player);
        plugin.getCombatManager().handleLogout(player);
        removeTrustBuffs(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        boolean hasHeart = (mainHandItem != null && HeartOfTheSea.isHeartOfTheSea(mainHandItem)) ||
                           (offHandItem != null && HeartOfTheSea.isHeartOfTheSea(offHandItem));

        if ((mainHandItem != null && mainHandItem.getType().isBlock()) ||
            (offHandItem != null && offHandItem.getType().isBlock())) {
            return;
        }

        ItemStack usedItem = event.getItem();
        EquipmentSlot usedHand = event.getHand();

        ItemStack customItem = null;
        EquipmentSlot customHand = null;

        if (usedItem != null && (
            CustomItem.isCustomItem(usedItem, "heart_catalyst") || 
            CustomItem.isCustomItem(usedItem, "heart_resonator") ||
            CustomItem.isCustomItem(usedItem, "heart_restoration_kit") ||
            CustomItem.isCustomItem(usedItem, "reforging_core") ||
            CustomItem.isCustomItem(usedItem, "stability_bottle")
        )) {
            customItem = usedItem;
            customHand = usedHand;
        } else if (offHandItem != null && (
            CustomItem.isCustomItem(offHandItem, "heart_catalyst") || 
            CustomItem.isCustomItem(offHandItem, "heart_resonator") ||
            CustomItem.isCustomItem(offHandItem, "heart_restoration_kit") ||
            CustomItem.isCustomItem(offHandItem, "reforging_core") ||
            CustomItem.isCustomItem(offHandItem, "stability_bottle")
        )) {
            customItem = offHandItem;
            customHand = EquipmentSlot.OFF_HAND;
        }
        
        if (customItem != null) {
            handleCustomItemUse(event, customItem, customHand);
            return;
        }

        if (!hasHeart) {
            return;
        }

        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) {
            player.sendMessage("§cYour Heart is not available or is dormant!");
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
        
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
            event.setCancelled(true);
            return;
        }
        
        org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
        if (topInv != null && !(topInv.getHolder() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (HeartOfTheSea.isHeartOfTheSea(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (HeartOfTheSea.isHeartOfTheSea(event.getItem())) {
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

    private void handleCustomItemUse(PlayerInteractEvent event, ItemStack item, EquipmentSlot hand) {
        if (CustomItem.isCustomItem(item, "heart_catalyst")) {
            handleHeartCatalyst(event.getPlayer(), event, hand, item);
        } else if (CustomItem.isCustomItem(item, "heart_resonator")) {
            handleHeartResonator(event.getPlayer(), event, hand, item);
        } else if (CustomItem.isCustomItem(item, "heart_restoration_kit")) {
            handleHeartRestorationKit(event.getPlayer(), event, hand, item);
        } else if (CustomItem.isCustomItem(item, "reforging_core")) {
            handleReforgingCore(event.getPlayer(), event, hand, item);
        } else if (CustomItem.isCustomItem(item, "stability_bottle")) {
            handleStabilityBottle(event.getPlayer(), event, hand, item);
        }
    }

    private void consumeOneItem(Player player, EquipmentSlot hand, ItemStack item) {
        if (item == null) return;
        int newAmount = item.getAmount() - 1;
        if (newAmount <= 0) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            item.setAmount(newAmount);
        }
    }

    private void handleHeartCatalyst(Player player, PlayerInteractEvent event, EquipmentSlot hand, ItemStack item) {
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
        consumeOneItem(player, hand, item);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleHeartResonator(Player player, PlayerInteractEvent event, EquipmentSlot hand, ItemStack item) {
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
        consumeOneItem(player, hand, item);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleHeartRestorationKit(Player player, PlayerInteractEvent event, EquipmentSlot hand, ItemStack item) {
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
        consumeOneItem(player, hand, item);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleReforgingCore(Player player, PlayerInteractEvent event, EquipmentSlot hand, ItemStack item) {
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null) return;
        
        AspectType current = heart.getAspect();
        java.util.List<AspectType> choices = new java.util.ArrayList<>(java.util.List.of(AspectType.values()));
        choices.remove(current);
        choices.remove(com.aspectsmp.core.AspectType.CLOUD);
        AspectType newAspect = choices.get(new java.util.Random().nextInt(choices.size()));
        
        heart.setAspect(newAspect);
        heart.setTier(1);
        heart.getCooldowns().clear();
        clearAspectEffects(player);
        
        player.sendMessage("§b§lYour Heart has been reforged!");
        player.sendMessage("§7Old Aspect: §f" + current.getDisplayName());
        player.sendMessage("§7New Aspect: §f" + newAspect.getDisplayName());
        player.sendMessage("§7Tier reset to 1.");
        
        consumeOneItem(player, hand, item);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    private void handleStabilityBottle(Player player, PlayerInteractEvent event, EquipmentSlot hand, ItemStack item) {
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
        consumeOneItem(player, hand, item);
        plugin.getScoreboardManager().updatePlayer(player);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof org.bukkit.entity.Player player)) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        if (event.getEntity() instanceof Player target) {
            if (plugin.getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            plugin.getCombatManager().tagPlayer(player.getUniqueId());
            plugin.getCombatManager().tagPlayer(target.getUniqueId());
        }
        
        heart.addEssence((long) (event.getDamage() * 2));
        
        if (heart.getAspect() == AspectType.INFERNO && event.getEntity() instanceof org.bukkit.entity.LivingEntity target && target.getFireTicks() > 0) {
            event.setDamage(event.getDamage() * 1.2);
        }
        
        if (heart.getAspect() == AspectType.WAR) {
            RuleModifier modifier = plugin.getRuleModifierManager().getModifier(AspectType.WAR);
            if (modifier != null) {
                modifier.recordCombat(player.getUniqueId());
            }
        }
        
        if (player.hasMetadata("berserker") && event.getEntity() instanceof org.bukkit.entity.LivingEntity living) {
            double healAmount = event.getFinalDamage() * 0.2;
            player.heal(healAmount);
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

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && event instanceof EntityDamageByEntityEvent damageByEntity && damageByEntity.getDamager() instanceof Player attacker) {
            if (!plugin.getTrustManager().isTrusted(player.getUniqueId(), attacker.getUniqueId())) {
                plugin.getCombatManager().tagPlayer(player.getUniqueId());
                plugin.getCombatManager().tagPlayer(attacker.getUniqueId());
            }
        }
        
        if (heart.getAspect() == AspectType.RIFT && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if (new java.util.Random().nextDouble() < 0.2 && !player.hasMetadata("rift_blink_cd")) {
                org.bukkit.Location loc = player.getLocation().getDirection().multiply(5).toLocation(player.getWorld());
                if (loc.getBlock().getType().isAir()) {
                    player.teleport(loc);
                    player.setMetadata("rift_blink_cd", new org.bukkit.metadata.FixedMetadataValue(com.aspectsmp.AspectSMP.getInstance(), true));
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.removeMetadata("rift_blink_cd", com.aspectsmp.AspectSMP.getInstance()), 60L);
                }
            }
        }
        
        if (player.hasMetadata("vitality_bloom")) {
            org.bukkit.metadata.MetadataValue meta = player.getMetadata("vitality_bloom").get(0);
            java.util.Optional<Double> opt = java.util.Optional.ofNullable(meta.asDouble());
            if (opt.isPresent()) {
                double bloomHp = opt.get();
                if (bloomHp >= event.getDamage()) {
                    player.setMetadata("vitality_bloom", new org.bukkit.metadata.FixedMetadataValue(com.aspectsmp.AspectSMP.getInstance(), bloomHp - event.getDamage()));
                    event.setDamage(0);
                    player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                } else {
                    player.removeMetadata("vitality_bloom", com.aspectsmp.AspectSMP.getInstance());
                    event.setDamage(event.getDamage() - bloomHp);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("golden_touch")) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        long now = System.currentTimeMillis();
        long expire = player.getMetadata("golden_touch").get(0).asLong();
        if (now > expire) {
            player.removeMetadata("golden_touch", com.aspectsmp.AspectSMP.getInstance());
            return;
        }
        
        if (new java.util.Random().nextDouble() < 0.5) {
            event.setDropItems(false);
            player.getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 1));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        if (victim.hasMetadata("revival_shield")) {
            org.bukkit.metadata.MetadataValue meta = victim.getMetadata("revival_shield").get(0);
            long expireTime = meta.asLong();
            if (System.currentTimeMillis() <= expireTime) {
                event.setCancelled(true);
                victim.setHealth(1.0);
                victim.removeMetadata("revival_shield", com.aspectsmp.AspectSMP.getInstance());
                victim.sendMessage("§a§lREVIVAL SHIELD ACTIVATED!");
                victim.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, victim.getLocation().add(0, 1, 0), 50, 1, 1, 1);
                victim.playSound(victim.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                return;
            } else {
                victim.removeMetadata("revival_shield", com.aspectsmp.AspectSMP.getInstance());
            }
        }

        removeTrustBuffs(victim);
        
        if (!plugin.getConfig().getBoolean("features.obfuscated-death-messages", true)) return;
        
        Player killer = victim.getKiller();
        
        if (killer == null) return;
        if (!killer.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;
        
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) return;
        
        String killerName = killer.getName();
        if (!deathMessage.contains(killerName)) return;
        
        String obfuscatedName = "§k" + killerName;
        event.setDeathMessage(deathMessage.replace(killerName, obfuscatedName));
    }

    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("features.explosion-proof-items", true)) return;
        if (!(event.getEntity() instanceof Item)) return;
        
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || 
            cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
        }
    }
}
