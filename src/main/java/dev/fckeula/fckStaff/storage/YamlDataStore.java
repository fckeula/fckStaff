package dev.fckeula.fckStaff.storage;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.staff.StaffPlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * YAML-backed implementation (staffdata.yml), selected via storage.type: yaml.
 * The simplest option, zero external setup - just a plain file, at the cost of
 * rewriting the whole file on every save. Fine for small/medium staff teams;
 * SQLite or MySQL/MariaDB scale better for very large ones.
 */
public class YamlDataStore implements StaffDataStore {

    private final FckStaff plugin;
    private final Object lock = new Object();
    private final File file;
    private YamlConfiguration data;

    public YamlDataStore(FckStaff plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "staffdata.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create staffdata.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    // Writes go through an async task like the SQL-backed stores, mainly for
    // consistency - a YAML save is a full file rewrite, so it's not free either.
    private void save() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lock) {
                try {
                    data.save(file);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not save staffdata.yml: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void close() {
        // Nothing to release for a plain file.
    }

    @Override
    public boolean hasEntry(UUID uuid) {
        synchronized (lock) {
            return data.contains(uuid.toString());
        }
    }

    @Override
    public void saveStaffState(UUID uuid, StaffPlayerData staffData, boolean manualVanish) {
        synchronized (lock) {
            String path = uuid.toString();
            data.set(path + ".manual-vanish", manualVanish);
            data.set(path + ".saved-inventory", toStorableList(staffData.savedInventory()));
            data.set(path + ".saved-armor", toStorableList(staffData.savedArmor()));
            data.set(path + ".saved-offhand", staffData.savedOffhand());
            data.set(path + ".saved-location", staffData.savedLocation());
            data.set(path + ".saved-gamemode", staffData.savedGameMode().name());
            data.set(path + ".saved-health", staffData.savedHealth());
            data.set(path + ".saved-food", staffData.savedFoodLevel());
            data.set(path + ".saved-exp", staffData.savedExp());
            data.set(path + ".saved-level", staffData.savedLevel());
            data.set(path + ".saved-allow-flight", staffData.savedAllowFlight());
            data.set(path + ".saved-flying", staffData.savedFlying());
        }
        save();
    }

    @Override
    public void updateManualVanish(UUID uuid, boolean manualVanish) {
        synchronized (lock) {
            String path = uuid.toString();
            if (!data.contains(path)) return;
            data.set(path + ".manual-vanish", manualVanish);
        }
        save();
    }

    @Override
    public void removeEntry(UUID uuid) {
        synchronized (lock) {
            data.set(uuid.toString(), null);
        }
        save();
    }

    @Override
    public StoredStaffState loadStaffState(UUID uuid) {
        synchronized (lock) {
            String path = uuid.toString();
            if (!data.contains(path)) return null;

            ItemStack[] inventory = fromStorableList(data.getList(path + ".saved-inventory"));
            ItemStack[] armor = fromStorableList(data.getList(path + ".saved-armor"));
            Object offhandRaw = data.get(path + ".saved-offhand");
            ItemStack offhand = offhandRaw instanceof ItemStack item ? item : new ItemStack(Material.AIR);

            Object locationRaw = data.get(path + ".saved-location");
            Location location = locationRaw instanceof Location loc ? loc : null;
            if (location == null) return null;

            GameMode gameMode = GameMode.valueOf(data.getString(path + ".saved-gamemode", "SURVIVAL"));
            double health = data.getDouble(path + ".saved-health", 20.0);
            int food = data.getInt(path + ".saved-food", 20);
            float exp = (float) data.getDouble(path + ".saved-exp", 0.0);
            int level = data.getInt(path + ".saved-level", 0);
            boolean allowFlight = data.getBoolean(path + ".saved-allow-flight", false);
            boolean flying = data.getBoolean(path + ".saved-flying", false);
            boolean manualVanish = data.getBoolean(path + ".manual-vanish", false);

            StaffPlayerData staffData = new StaffPlayerData(
                    inventory, armor, offhand, location, gameMode,
                    health, food, exp, level, allowFlight, flying
            );

            return new StoredStaffState(staffData, manualVanish);
        }
    }

    private List<ItemStack> toStorableList(ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack item : items) {
            list.add(item != null ? item : new ItemStack(Material.AIR));
        }
        return list;
    }

    private ItemStack[] fromStorableList(List<?> rawList) {
        if (rawList == null) return new ItemStack[0];

        ItemStack[] array = new ItemStack[rawList.size()];
        for (int i = 0; i < rawList.size(); i++) {
            Object obj = rawList.get(i);
            if (obj instanceof ItemStack item && item.getType() != Material.AIR) {
                array[i] = item;
            }
        }
        return array;
    }
}
