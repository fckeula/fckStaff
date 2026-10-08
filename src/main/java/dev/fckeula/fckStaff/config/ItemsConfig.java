package dev.fckeula.fckStaff.config;

import dev.fckeula.fckStaff.FckStaff;
import org.bukkit.Material;

/**
 * Reads per-item visual/behavioral settings (material, slot, amount, glow,
 * custom-model-data) from config.yml. Display name and lore live in
 * langs/*.yml instead, using the same key path, so translation stays
 * separate from appearance.
 *
 * Every getter always reads live from plugin.getConfig(), so /fckstaff reload
 * picks up changes automatically without any extra caching logic here.
 */
public class ItemsConfig {

    private final FckStaff plugin;

    public ItemsConfig(FckStaff plugin) {
        this.plugin = plugin;
    }

    public Material getMaterial(String path, Material fallback) {
        String matName = plugin.getConfig().getString(path + ".material");
        if (matName == null) return fallback;

        Material material = Material.matchMaterial(matName);
        if (material == null) {
            plugin.getLogger().warning("Invalid material '" + matName + "' for '" + path + "', using default.");
            return fallback;
        }
        return material;
    }

    public int getSlot(String path, int fallback) {
        return plugin.getConfig().getInt(path + ".slot", fallback);
    }

    public int getAmount(String path, int fallback) {
        int amount = plugin.getConfig().getInt(path + ".amount", fallback);
        return Math.max(1, Math.min(64, amount));
    }

    public boolean isGlowing(String path) {
        return plugin.getConfig().getBoolean(path + ".glow", false);
    }

    public Integer getCustomModelData(String path) {
        String key = path + ".custom-model-data";
        if (!plugin.getConfig().contains(key)) return null;
        return plugin.getConfig().getInt(key);
    }

    public int getMenuSize(int fallback) {
        int size = plugin.getConfig().getInt("menu.size", fallback);
        size = Math.max(9, Math.min(54, size));
        return (size / 9) * 9;
    }
}
