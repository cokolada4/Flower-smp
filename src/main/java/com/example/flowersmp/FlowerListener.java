package com.example.flowersmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class FlowerListener implements Listener {

    private final FlowerSMP plugin;
    private final NamespacedKey flowerKey;
    private final NamespacedKey ownerKey;
    private Material flowerMaterial;

    public FlowerListener(FlowerSMP plugin) {
        this.plugin = plugin;
        this.flowerKey = new NamespacedKey(plugin, "special_flower");
        this.ownerKey = new NamespacedKey(plugin, "flower_owner");
        updateFlowerMaterial();
    }

    public void updateFlowerMaterial() {
        String materialName = plugin.getConfig().getString("flower-material", "POPPY");
        flowerMaterial = Material.matchMaterial(materialName);
        if (flowerMaterial == null) flowerMaterial = Material.POPPY;
    }

    public NamespacedKey getFlowerKey() { return flowerKey; }
    public NamespacedKey getOwnerKey() { return ownerKey; }

    public ItemStack getSpecialFlower(UUID ownerUuid) {
        ItemStack item = new ItemStack(flowerMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String ownerName = "Unknown";
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            if (owner.getName() != null) {
                ownerName = owner.getName();
            }

            String nameTemplate = plugin.getConfig().getString("flower-name-template", "&c%player%'s Life Flower");
            String displayName = nameTemplate.replace("%player%", ownerName);

            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
            meta.getPersistentDataContainer().set(flowerKey, PersistentDataType.STRING, "true");
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, ownerUuid.toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSpecialFlower(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(flowerKey, PersistentDataType.STRING);
    }

    private UUID getFlowerOwner(ItemStack item) {
        if (!isSpecialFlower(item)) return null;
        String uuidStr = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        return UUID.fromString(uuidStr);
    }

    private void notifyOwnerOfDestruction(UUID ownerUuid) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            String message = plugin.getConfig().getString("flower-destroyed-message", "&cYour Life Flower has been destroyed! You are no longer safe.");
            owner.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (plugin.isDead(event.getPlayer().getUniqueId())) {
            String message = plugin.getConfig().getString("login-deny-message", "You are currently dead because your flower was not placed when you died.");
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isDead(player.getUniqueId())) return;

        if (!plugin.hasReceivedFlower(player.getUniqueId())) {
            player.getInventory().addItem(getSpecialFlower(player.getUniqueId()));
            plugin.setReceivedFlower(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isSpecialFlower(item)) {
            UUID ownerUuid = getFlowerOwner(item);
            if (ownerUuid != null) {
                plugin.setFlowerPlaced(ownerUuid, event.getBlock().getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (handleFlowerRemoval(event.getBlock(), true)) {
            event.setDropItems(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        handleFlowerRemoval(event.getBlock(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleFlowerRemoval(block, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleFlowerRemoval(block, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent event) {
        handleFlowerRemoval(event.getToBlock(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            handleFlowerRemoval(block, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            handleFlowerRemoval(block, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() == flowerMaterial) {
            if (!block.getBlockData().isSupported(block)) {
                handleFlowerRemoval(block, true);
            }
        }
    }

    private boolean handleFlowerRemoval(Block block, boolean drop) {
        UUID ownerUuid = plugin.getOwnerOfLocation(block.getLocation());
        if (ownerUuid != null) {
            plugin.setFlowerPlaced(ownerUuid, null);
            if (drop) {
                block.getWorld().dropItemNaturally(block.getLocation(), getSpecialFlower(ownerUuid));
            } else {
                notifyOwnerOfDestruction(ownerUuid);
            }
            block.setType(Material.AIR);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (isSpecialFlower(item.getItemStack())) {
                // If item is removed by damage (fire, lava, cactus, etc)
                // We don't have EntityRemoveEvent in 1.20.1 easily, so we check damage.
                // For cactus/lava it's usually instant or rapid.
                UUID ownerUuid = getFlowerOwner(item.getItemStack());
                if (ownerUuid != null) {
                    // Check if it's dead next tick or now
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!item.isValid()) {
                             notifyOwnerOfDestruction(ownerUuid);
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isSpecialFlower(event.getEntity().getItemStack())) {
            UUID ownerUuid = getFlowerOwner(event.getEntity().getItemStack());
            if (ownerUuid != null) {
                notifyOwnerOfDestruction(ownerUuid);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.hasPlacedFlower(player.getUniqueId())) {
            plugin.setDead(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.isDead(uuid)) {
            String message = plugin.getConfig().getString("death-kick-message", "You died without your flower being placed! You cannot respawn.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            }, 1L);
        } else {
            Location flowerLoc = plugin.getPlacedFlowerLocation(uuid);
            if (flowerLoc != null) {
                event.setRespawnLocation(flowerLoc.clone().add(0.5, 0.1, 0.5));
            }
        }
    }
}
