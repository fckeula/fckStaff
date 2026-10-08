package dev.fckeula.fckStaff.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(Component name) {
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(List<Component> lines) {
        meta.lore(lines.stream()
                .map(c -> c.decoration(TextDecoration.ITALIC, false))
                .toList());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        return this;
    }

    public ItemBuilder customModelData(Integer data) {
        if (data != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder tag(NamespacedKey key, String value) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.STRING, value);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
