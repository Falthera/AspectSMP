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
import java.util.UUID;

public class ListenerManager implements Listener {

    private final AspectSMP plugin;

    public ListenerManager(AspectSMP plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        new BukkitRunnable() {
            @Override
            public void run() {
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
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void applyTrustBuffs(Player ally, AspectType aspect) {
        ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 80, 0), true);
        ally.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE, 80, 0), true);
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
        if (!(event.getDamager() instanceof org.bukkit.entity.Player player)) return;
        
        Heart heart = plugin.getHeartManager().getHeart(player.getUniqueId()).orElse(null);
        if (heart == null || heart.isDormant()) return;
        
        if (event.getEntity() instanceof Player target) {
            if (plugin.getTrustManager().isTrusted(player.getUniqueId(), target.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
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
        if (!plugin.getConfig().getBoolean("features.obfuscated-death-messages", true)) return;
        
        Player victim = event.getEntity();
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
