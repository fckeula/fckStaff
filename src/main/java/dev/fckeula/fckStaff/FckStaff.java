package dev.fckeula.fckStaff;

import dev.fckeula.fckStaff.addon.AddonManager;
import dev.fckeula.fckStaff.api.fckStaffAPI;
import dev.fckeula.fckStaff.api.fckStaffAPIImpl;
import dev.fckeula.fckStaff.commands.FreezeChatCommand;
import dev.fckeula.fckStaff.commands.FreezeCommand;
import dev.fckeula.fckStaff.commands.StaffChatCommand;
import dev.fckeula.fckStaff.commands.StaffModeCommand;
import dev.fckeula.fckStaff.commands.VanishCommand;
import dev.fckeula.fckStaff.commands.fckStaffCommand;
import dev.fckeula.fckStaff.config.ItemsConfig;
import dev.fckeula.fckStaff.gui.StaffToolsMenu;
import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.listeners.ChatListener;
import dev.fckeula.fckStaff.listeners.FreezeListener;
import dev.fckeula.fckStaff.listeners.PlayerConnectionListener;
import dev.fckeula.fckStaff.listeners.StaffToolsListener;
import dev.fckeula.fckStaff.listeners.TabCompleteListener;
import dev.fckeula.fckStaff.listeners.VanishListener;
import dev.fckeula.fckStaff.placeholders.fckStaffPlaceholders;
import dev.fckeula.fckStaff.staff.StaffManager;
import dev.fckeula.fckStaff.storage.MySqlDataStore;
import dev.fckeula.fckStaff.storage.SqliteDataStore;
import dev.fckeula.fckStaff.storage.StaffDataStore;
import dev.fckeula.fckStaff.storage.YamlDataStore;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class FckStaff extends JavaPlugin {

    private LangManager langManager;
    private ItemsConfig itemsConfig;
    private StaffDataStore staffDataStore;
    private StaffManager staffManager;
    private StaffToolsMenu staffToolsMenu;
    private AddonManager addonManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        this.langManager = new LangManager(this);
        this.itemsConfig = new ItemsConfig(this);
        this.staffDataStore = createDataStore();
        this.staffToolsMenu = new StaffToolsMenu(langManager, itemsConfig);
        this.staffManager = new StaffManager(this, langManager, staffToolsMenu, staffDataStore);

        // Exposes state/control to other plugins via ServicesManager (no direct dependency needed).
        fckStaffAPI api = new fckStaffAPIImpl(staffManager);
        getServer().getServicesManager().register(fckStaffAPI.class, api, this, ServicePriority.Normal);

        this.addonManager = new AddonManager(this, api);

        if (!addonManager.syncRegistry()) {
            getLogger().severe("Could not verify installation. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getConfig().getBoolean("addons.enabled", true)) {
            addonManager.loadAll();
        }

        // Commands via Paper's Brigadier/Lifecycle API; aliases come from config.yml.
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            registerCommand(commands, "staffmode", "Toggles staff mode", new StaffModeCommand(staffManager, langManager));
            registerCommand(commands, "freeze", "Freezes or unfreezes a player", new FreezeCommand(staffManager, langManager));
            registerCommand(commands, "freezechat", "Talk with a frozen player", new FreezeChatCommand(staffManager, langManager));
            registerCommand(commands, "staffchat", "Toggles staff chat or sends a message", new StaffChatCommand(staffManager, langManager));
            registerCommand(commands, "vanish", "Toggles your invisibility", new VanishCommand(this, staffManager, langManager));
            registerCommand(commands, "fckstaff", "Manages the fckStaff plugin", new fckStaffCommand(this, langManager));
        });

        // Register listeners
        getServer().getPluginManager().registerEvents(new FreezeListener(staffManager, langManager), this);
        getServer().getPluginManager().registerEvents(new StaffToolsListener(staffManager, staffToolsMenu, langManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(staffManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(staffManager), this);
        getServer().getPluginManager().registerEvents(new TabCompleteListener(staffManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(staffManager), this);

        // Only touches fckStaffPlaceholders (loading the class) when PAPI is present,
        // so servers without it never hit a NoClassDefFoundError.
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new fckStaffPlaceholders(this, staffManager).register();
            getLogger().info("PlaceholderAPI found, registered %fckstaff_*% placeholders.");
        }

        getLogger().info("fckStaff has been enabled successfully.");

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!addonManager.syncRegistry()) {
                getLogger().severe("Registry sync failed — disabling.");
                getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            }
        }, 20L * 60 * 60 * 6, 20L * 60 * 60 * 6);
    }

    @Override
    public void onDisable() {
        // Staff mode isn't undone here - it stays persisted and reapplies on next join.
        if (addonManager != null) {
            addonManager.disableAll();
        }
        getServer().getServicesManager().unregisterAll(this);
        if (staffManager != null) {
            staffManager.shutdown();
        }
        if (staffDataStore != null) {
            staffDataStore.close();
        }
        getLogger().info("fckStaff has been disabled.");
    }

    private void registerCommand(Commands commands, String label, String description, BasicCommand command) {
        List<String> aliases = getConfig().getStringList("commands." + label + ".aliases");
        commands.register(label, description, aliases, command);
    }

    // Picks the storage backend from config.yml; falls back to SQLite.
    private StaffDataStore createDataStore() {
        String type = getConfig().getString("storage.type", "sqlite").toLowerCase();
        return switch (type) {
            case "mysql" -> new MySqlDataStore(this, false);
            case "mariadb" -> new MySqlDataStore(this, true);
            case "yaml", "yml" -> new YamlDataStore(this);
            default -> new SqliteDataStore(this);
        };
    }

    public StaffManager getStaffManager() {
        return staffManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }
}
