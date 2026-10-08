package dev.fckeula.fckStaff.api.addon;

import dev.fckeula.fckStaff.api.fckStaffAPI;
import org.bukkit.plugin.Plugin;

/**
 * Implement this on the main class declared in your addon's addon.yml to hook
 * into fckStaff without being compiled together with it.
 *
 * <p>Example addon.yml (placed at the root of your addon's jar):
 * <pre>{@code
 * name: ExampleAddon
 * version: '1.0'
 * main: com.example.ExampleAddon
 * }</pre>
 *
 * <p>Drop the built jar into {@code plugins/fckStaff/addons/} and restart the
 * server (or start it for the first time) - addons are only loaded on startup,
 * not by /fckstaff reload.
 */
public interface fckStaffAddon {

    /**
     * Called once, right after fckStaff finishes its own setup. Register your
     * own listeners/commands against {@code hostPlugin} here, e.g.:
     * <pre>{@code hostPlugin.getServer().getPluginManager().registerEvents(new MyListener(api), hostPlugin);}</pre>
     */
    void onEnable(fckStaffAPI api, Plugin hostPlugin);

    /**
     * Called when fckStaff is disabling (server stop/reload). Clean up any
     * resources you opened in {@link #onEnable}.
     */
    void onDisable();
}
