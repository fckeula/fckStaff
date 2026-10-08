package dev.fckeula.fckStaff.gui;

import dev.fckeula.fckStaff.config.ItemsConfig;
import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Builds the staff tools kit and the GUI menu.
 * Material, slot, amount, glow and custom-model-data come from config.yml
 * ("items"/"menu" sections); display name and lore come from langs/*.yml.
 * Both sides use the same key path, so every item is fully customizable
 * without touching any code.
 */
public class StaffToolsMenu {

    public static final NamespacedKey TOOL_KEY = new NamespacedKey("fckstaff", "tool");
    public static final NamespacedKey MENU_ACTION_KEY = new NamespacedKey("fckstaff", "menu_action");

    public static final String FREEZE_STICK = "freeze_stick";
    public static final String RANDOM_TP = "random_tp";
    public static final String INSPECTOR = "inspector";
    public static final String VANISH_TOGGLE = "vanish_toggle";
    public static final String FLY_TOGGLE = "fly_toggle";
    public static final String MENU_OPENER = "menu_opener";

    public static final String ACTION_STAFF_LIST = "staff_list";
    public static final String ACTION_RANDOM_TP = "random_tp";
    public static final String ACTION_LEAVE = "leave";

    private final LangManager lang;
    private final ItemsConfig items;

    public StaffToolsMenu(LangManager lang, ItemsConfig items) {
        this.lang = lang;
        this.items = items;
    }

    public Component menuTitle() {
        return lang.get("menu.title");
    }

    public void giveTools(Player player) {
        player.getInventory().setItem(items.getSlot("items.freeze-stick", 0),
                kitItem("items.freeze-stick", Material.BLAZE_ROD, FREEZE_STICK));
        player.getInventory().setItem(items.getSlot("items.random-tp", 1),
                kitItem("items.random-tp", Material.COMPASS, RANDOM_TP));
        player.getInventory().setItem(items.getSlot("items.inspector", 2),
                kitItem("items.inspector", Material.PLAYER_HEAD, INSPECTOR));
        player.getInventory().setItem(items.getSlot("items.vanish-toggle", 3),
                kitItem("items.vanish-toggle", Material.GLASS, VANISH_TOGGLE));
        player.getInventory().setItem(items.getSlot("items.menu", 4),
                kitItem("items.menu", Material.NETHER_STAR, MENU_OPENER));
        player.getInventory().setItem(items.getSlot("items.fly-toggle", 5),
                kitItem("items.fly-toggle", Material.FEATHER, FLY_TOGGLE));
    }

    private ItemStack kitItem(String path, Material fallbackMaterial, String toolTag) {
        return new ItemBuilder(items.getMaterial(path, fallbackMaterial))
                .name(lang.get(path + ".name"))
                .lore(lang.getList(path + ".lore"))
                .amount(items.getAmount(path, 1))
                .glow(items.isGlowing(path))
                .customModelData(items.getCustomModelData(path))
                .tag(TOOL_KEY, toolTag)
                .build();
    }

    public Inventory buildMenu() {
        Inventory inv = Bukkit.createInventory(null, items.getMenuSize(27), menuTitle());

        inv.setItem(items.getSlot("menu.staff-list", 11),
                menuItem("menu.staff-list", Material.ENDER_EYE, ACTION_STAFF_LIST));
        inv.setItem(items.getSlot("menu.random-tp", 13),
                menuItem("menu.random-tp", Material.COMPASS, ACTION_RANDOM_TP));
        inv.setItem(items.getSlot("menu.leave", 15),
                menuItem("menu.leave", Material.BARRIER, ACTION_LEAVE));

        return inv;
    }

    private ItemStack menuItem(String path, Material fallbackMaterial, String action) {
        return new ItemBuilder(items.getMaterial(path, fallbackMaterial))
                .name(lang.get(path + ".name"))
                .lore(lang.getList(path + ".lore"))
                .amount(items.getAmount(path, 1))
                .glow(items.isGlowing(path))
                .customModelData(items.getCustomModelData(path))
                .tag(MENU_ACTION_KEY, action)
                .build();
    }
}
