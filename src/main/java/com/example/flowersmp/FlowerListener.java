package com.example.flowersmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import org.bukkit.event.entity.EntityExplodeEvent;
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

    public FlowerListener(FlowerSMP plugin) {
        this.plugin = plugin;
        this.flowerKey = new NamespacedKey(plugin, "special_flower");
    }

    private ItemStack getSpecialFlower() {
        String materialName = plugin.getConfig().getString("flower-material", "POPPY");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.POPPY;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString("flower-name", "&cYour Life Flower");
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            meta.getPersistentDataContainer().set(flowerKey, PersistentDataType.STRING, "true");
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSpecialFlower(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(flowerKey, PersistentDataType.STRING);
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

        if (!plugin.hasPlacedFlower(player.getUniqueId()) && !hasFlowerInInventory(player)) {
            player.getInventory().addItem(getSpecialFlower());
        }
    }

    private boolean hasFlowerInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSpecialFlower(item)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isSpecialFlower(event.getItemInHand())) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (plugin.hasPlacedFlower(uuid)) {
                event.setCancelled(true);
                return;
            }
            plugin.setFlowerPlaced(uuid, event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        handleFlowerRemoval(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        handleFlowerRemoval(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleFlowerRemoval(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleFlowerRemoval(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent event) {
        handleFlowerRemoval(event.getToBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            handleFlowerRemoval(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            handleFlowerRemoval(block);
        }
    }

    private void handleFlowerRemoval(Block block) {
        UUID ownerUuid = plugin.getOwnerOfLocation(block.getLocation());
        if (ownerUuid != null) {
            plugin.setFlowerPlaced(ownerUuid, null);
            block.getWorld().dropItemNaturally(block.getLocation(), getSpecialFlower());
            block.setType(Material.AIR);
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
        if (plugin.isDead(player.getUniqueId())) {
            String message = plugin.getConfig().getString("death-kick-message", "You died without your flower being placed! You cannot respawn.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            }, 1L);
        }
    }
}
