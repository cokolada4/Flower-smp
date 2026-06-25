package com.example.flowersmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FlowerCommand implements CommandExecutor {

    private final FlowerSMP plugin;
    private final FlowerListener listener;

    public FlowerCommand(FlowerSMP plugin, FlowerListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be an operator to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lifeflower new <player> or /lifeflower pardon <player>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        if (subCommand.equals("new")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = target.getUniqueId();

            // 1. Remove existing placed flower
            Location loc = plugin.getPlacedFlowerLocation(uuid);
            if (loc != null) {
                loc.getBlock().setType(Material.AIR);
                plugin.setFlowerPlaced(uuid, null);
            }

            // 2. Remove from all online players' inventories
            NamespacedKey ownerKey = listener.getOwnerKey();
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] contents = p.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item == null || item.getType() == Material.AIR) continue;
                    if (!item.hasItemMeta()) continue;

                    ItemMeta meta = item.getItemMeta();
                    String ownerUuidStr = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                    if (uuid.toString().equals(ownerUuidStr)) {
                        p.getInventory().setItem(i, null);
                    }
                }
            }

            // 3. Give new flower to the operator or target
            ItemStack newFlower = listener.getSpecialFlower(uuid);
            if (sender instanceof Player op) {
                op.getInventory().addItem(newFlower);
                sender.sendMessage("§aNew Life Flower for " + targetName + " has been given to you.");
            } else if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().getInventory().addItem(newFlower);
                sender.sendMessage("§aNew Life Flower for " + targetName + " has been given to them (online).");
            } else {
                sender.sendMessage("§cCannot give flower: target is offline and command was run from console.");
            }
            return true;

        } else if (subCommand.equals("pardon")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = target.getUniqueId();

            if (plugin.isDead(uuid)) {
                plugin.setDead(uuid, false);
                sender.sendMessage("§aPlayer " + targetName + " has been pardoned and can now rejoin/respawn.");
            } else {
                sender.sendMessage("§ePlayer " + targetName + " was not marked as dead.");
            }
            return true;
        }

        return false;
    }
}
