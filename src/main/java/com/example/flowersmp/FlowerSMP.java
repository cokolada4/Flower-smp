package com.example.flowersmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FlowerSMP extends JavaPlugin {

    private File dataFile;
    private FileConfiguration dataConfig;
    private Map<UUID, Location> placedFlowers = new HashMap<>();
    private Set<UUID> deadPlayers = new HashSet<>();
    private Set<UUID> receivedFlowers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();

        getServer().getPluginManager().registerEvents(new FlowerListener(this), this);
        getLogger().info("FlowerSMP enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("FlowerSMP disabled!");
    }

    public void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        placedFlowers.clear();
        if (dataConfig.getConfigurationSection("placed") != null) {
            for (String uuidStr : dataConfig.getConfigurationSection("placed").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String locStr = dataConfig.getString("placed." + uuidStr);
                Location loc = stringToLocation(locStr);
                if (loc != null) {
                    placedFlowers.put(uuid, loc);
                }
            }
        }

        deadPlayers.clear();
        if (dataConfig.contains("dead")) {
            for (String uuidStr : dataConfig.getStringList("dead")) {
                deadPlayers.add(UUID.fromString(uuidStr));
            }
        }

        receivedFlowers.clear();
        if (dataConfig.contains("received")) {
            for (String uuidStr : dataConfig.getStringList("received")) {
                receivedFlowers.add(UUID.fromString(uuidStr));
            }
        }
    }

    public void saveData() {
        dataConfig.set("placed", null);
        for (Map.Entry<UUID, Location> entry : placedFlowers.entrySet()) {
            dataConfig.set("placed." + entry.getKey().toString(), locationToString(entry.getValue()));
        }
        dataConfig.set("dead", deadPlayers.stream().map(UUID::toString).toList());
        dataConfig.set("received", receivedFlowers.stream().map(UUID::toString).toList());
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location stringToLocation(String str) {
        if (str == null) return null;
        String[] parts = str.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public boolean hasPlacedFlower(UUID uuid) {
        return placedFlowers.containsKey(uuid);
    }

    public void setFlowerPlaced(UUID uuid, Location loc) {
        if (loc != null) {
            placedFlowers.put(uuid, loc);
        } else {
            placedFlowers.remove(uuid);
        }
        saveData();
    }

    public UUID getOwnerOfLocation(Location loc) {
        for (Map.Entry<UUID, Location> entry : placedFlowers.entrySet()) {
            if (entry.getValue().equals(loc)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isDead(UUID uuid) {
        return deadPlayers.contains(uuid);
    }

    public void setDead(UUID uuid, boolean dead) {
        if (dead) {
            deadPlayers.add(uuid);
        } else {
            deadPlayers.remove(uuid);
        }
        saveData();
    }

    public boolean hasReceivedFlower(UUID uuid) {
        return receivedFlowers.contains(uuid);
    }

    public void setReceivedFlower(UUID uuid, boolean received) {
        if (received) {
            receivedFlowers.add(uuid);
        } else {
            receivedFlowers.remove(uuid);
        }
        saveData();
    }
}
