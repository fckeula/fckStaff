package dev.fckeula.fckStaff.staff;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.api.event.PlayerFreezeEvent;
import dev.fckeula.fckStaff.api.event.StaffChatMessageEvent;
import dev.fckeula.fckStaff.api.event.StaffModeToggleEvent;
import dev.fckeula.fckStaff.api.event.VanishToggleEvent;
import dev.fckeula.fckStaff.gui.StaffToolsMenu;
import dev.fckeula.fckStaff.lang.LangManager;
import dev.fckeula.fckStaff.storage.StaffDataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffManager {

    // Fixed fade-in/stay/fade-out for every freeze title - reused instead of
    // rebuilt on each refresh since it never changes.
    private static final Title.Times FREEZE_TITLE_TIMES =
            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500));

    private final FckStaff plugin;
    private final LangManager lang;
    private final StaffToolsMenu toolsMenu;
    private final StaffDataStore dataStore;

    private final Map<UUID, StaffPlayerData> activeStaff = new HashMap<>();
    private final Set<UUID> manualVanish = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Set<UUID> staffChatToggled = new HashSet<>();
    private final Map<UUID, BukkitTask> actionBarTasks = new HashMap<>();
    private final Map<UUID, ActionBarSnapshot> actionBarState = new HashMap<>();
    private final Map<UUID, Component> actionBarCache = new HashMap<>();
    private final Map<UUID, BukkitTask> freezeTitleTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> freezeChatTasks = new HashMap<>();

    public StaffManager(FckStaff plugin, LangManager lang, StaffToolsMenu toolsMenu, StaffDataStore dataStore) {
        this.plugin = plugin;
        this.lang = lang;
        this.toolsMenu = toolsMenu;
        this.dataStore = dataStore;
    }

    public boolean isInStaffMode(Player player) {
        return activeStaff.containsKey(player.getUniqueId());
    }

    public boolean enableStaffMode(Player player) {
        if (isInStaffMode(player)) return false;

        StaffModeToggleEvent event = new StaffModeToggleEvent(player, true);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        UUID id = player.getUniqueId();
        StaffPlayerData data = new StaffPlayerData(
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getInventory().getItemInOffHand().clone(),
                player.getLocation().clone(),
                player.getGameMode(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getExp(),
                player.getLevel(),
                player.getAllowFlight(),
                player.isFlying()
        );

        activeStaff.put(id, data);

        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
        player.setFoodLevel(20);

        toolsMenu.giveTools(player);
        manualVanish.add(id);
        refreshVisibility(player);
        startActionBar(player);

        dataStore.saveStaffState(id, data, true);

        player.sendMessage(lang.get("staffmode.enabled"));
        return true;
    }

    public boolean disableStaffMode(Player player) {
        if (!isInStaffMode(player)) return false;

        StaffModeToggleEvent event = new StaffModeToggleEvent(player, false);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        UUID id = player.getUniqueId();
        StaffPlayerData data = activeStaff.remove(id);
        if (data == null) return false;

        player.getInventory().setContents(data.savedInventory());
        player.getInventory().setArmorContents(data.savedArmor());
        player.getInventory().setItemInOffHand(data.savedOffhand());
        player.teleport(data.savedLocation());
        player.setGameMode(data.savedGameMode());
        player.setAllowFlight(data.savedAllowFlight());
        player.setFlying(data.savedFlying());

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        player.setHealth(Math.min(data.savedHealth(), maxHealth));
        player.setFoodLevel(data.savedFoodLevel());
        player.setExp(data.savedExp());
        player.setLevel(data.savedLevel());

        setFrozen(player, false);
        stopActionBar(player);
        dataStore.removeEntry(id);
        manualVanish.remove(id);

        // A leftover staffchat toggle would silently swallow future chat messages
        // once they're no longer staff.
        if (staffChatToggled.remove(id)) {
            player.sendMessage(lang.get("staffchat.disabled"));
        }

        refreshVisibility(player);

        player.sendMessage(lang.get("staffmode.disabled"));
        return true;
    }

    public boolean toggleStaffMode(Player player) {
        return isInStaffMode(player) ? disableStaffMode(player) : enableStaffMode(player);
    }

    // ---------------- Vanish ----------------

    // Applies target's current vanish state to every online viewer. Idempotent
    // and self-correcting - safe to call as often as needed.
    private void refreshVisibility(Player target) {
        boolean vanished = isVanished(target);
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                applyVisibility(viewer, target, vanished && !viewer.hasPermission("fckstaff.see"));
            }
        }
    }

    // Applies every online target's vanish state to a single viewer. Used on join.
    private void refreshVisibilityForViewer(Player viewer) {
        boolean canSeeVanished = viewer.hasPermission("fckstaff.see");
        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (!target.equals(viewer)) {
                applyVisibility(viewer, target, isVanished(target) && !canSeeVanished);
            }
        }
    }

    private void applyVisibility(Player viewer, Player target, boolean shouldHide) {
        if (shouldHide) {
            viewer.hidePlayer(plugin, target);
        } else {
            viewer.showPlayer(plugin, target);
        }
    }

    // Single source of truth for vanish: staff mode adds the player here on
    // enable and removes them on disable, but it can be toggled independently
    // (via /vanish or the kit item) at any point while in staff mode too.
    public boolean isVanished(Player player) {
        return manualVanish.contains(player.getUniqueId());
    }

    /**
     * Toggles vanish (/vanish or the kit item), independent of staff mode.
     *
     * @return the new vanish state, or null if cancelled via {@link VanishToggleEvent}.
     *         Callers must check for null before showing a status message.
     */
    public Boolean toggleVanish(Player player) {
        UUID id = player.getUniqueId();
        boolean wantVanish = !manualVanish.contains(id);

        VanishToggleEvent event = new VanishToggleEvent(player, wantVanish);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        if (wantVanish) {
            manualVanish.add(id);
        } else {
            manualVanish.remove(id);
        }

        refreshVisibility(player);

        if (isInStaffMode(player)) {
            dataStore.updateManualVanish(id, wantVanish);
        }
        return wantVanish;
    }

    // Restores staff mode/vanish for a player who had it active before leaving
    // (or before a restart), and syncs visibility both ways with other players.
    public void handleJoin(Player joined) {
        UUID id = joined.getUniqueId();

        if (!activeStaff.containsKey(id) && dataStore.hasEntry(id)) {
            StaffDataStore.StoredStaffState stored = dataStore.loadStaffState(id);
            if (stored != null) {
                activeStaff.put(id, stored.staffData());
                if (stored.manualVanish()) {
                    manualVanish.add(id);
                }
            }
        }

        if (isInStaffMode(joined)) {
            startActionBar(joined);
        }

        refreshVisibility(joined);
        refreshVisibilityForViewer(joined);
    }

    // Staff mode is NOT undone on disconnect - it stays active until /staffmode
    // is run again, surviving a relog or a restart (see the storage backend).
    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        stopActionBar(player);

        if (frozenPlayers.remove(id)) {
            stopFreezeTasks(player);
            punishFrozenDisconnect(player);
        }

        staffChatToggled.remove(id);
        if (!isInStaffMode(player)) {
            manualVanish.remove(id);
        }
    }

    // ---------------- Action bar ----------------

    private record ActionBarSnapshot(boolean vanished, boolean staffChat) {}

    private void startActionBar(Player player) {
        stopActionBar(player);
        UUID id = player.getUniqueId();

        long interval = Math.max(1L, plugin.getConfig().getLong("actionbar.interval-ticks", 10L));

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isInStaffMode(player)) {
                stopActionBar(player);
                return;
            }

            ActionBarSnapshot snapshot = new ActionBarSnapshot(isVanished(player), hasStaffChatToggled(player));

            // Resending every tick keeps the bar from fading, but rebuilding the
            // Component (MiniMessage parsing) is comparatively expensive and the
            // state rarely changes between ticks - so only rebuild it then.
            Component bar = snapshot.equals(actionBarState.get(id))
                    ? actionBarCache.get(id)
                    : buildActionBar(id, snapshot);

            player.sendActionBar(bar);
        }, 0L, interval);

        actionBarTasks.put(id, task);
    }

    private Component buildActionBar(UUID id, ActionBarSnapshot snapshot) {
        Component active = lang.get("actionbar.active");
        Component inactive = lang.get("actionbar.inactive");

        Component bar = lang.get("actionbar.format",
                Placeholder.component("staffmode_status", active),
                Placeholder.component("vanish_status", snapshot.vanished() ? active : inactive),
                Placeholder.component("staffchat_status", snapshot.staffChat() ? active : inactive));

        actionBarState.put(id, snapshot);
        actionBarCache.put(id, bar);
        return bar;
    }

    private void stopActionBar(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = actionBarTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        actionBarState.remove(id);
        actionBarCache.remove(id);
    }

    // ---------------- Freeze ----------------

    /** Freezes/unfreezes a player. No-op if already in that state. */
    public boolean setFrozen(Player player, boolean frozen) {
        return setFrozen(player, frozen, null);
    }

    /**
     * Same as {@link #setFrozen(Player, boolean)}, but records who triggered it
     * for {@link PlayerFreezeEvent}. Pass null if there's no specific sender.
     *
     * @return true if applied; false if already in that state or cancelled.
     */
    public boolean setFrozen(Player player, boolean frozen, CommandSender initiator) {
        UUID id = player.getUniqueId();
        if (frozen == frozenPlayers.contains(id)) return false;

        PlayerFreezeEvent event = new PlayerFreezeEvent(player, initiator, frozen);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        if (frozen) {
            frozenPlayers.add(id);
            sendFreezeTitle(player, "freeze.title-frozen");
            startFreezeTasks(player);
        } else {
            frozenPlayers.remove(id);
            stopFreezeTasks(player);
            sendFreezeTitle(player, "freeze.title-unfrozen");
        }
        return true;
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    private void sendFreezeTitle(Player player, String titleKey) {
        player.showTitle(Title.title(lang.get(titleKey), Component.empty(), FREEZE_TITLE_TIMES));
    }

    private void startFreezeTasks(Player player) {
        stopFreezeTasks(player);
        UUID id = player.getUniqueId();

        long titleInterval = Math.max(20L, plugin.getConfig().getLong("freeze.title-refresh-interval-seconds", 3) * 20L);
        long chatInterval = Math.max(20L, plugin.getConfig().getLong("freeze.chat-reminder-interval-seconds", 5) * 20L);

        BukkitTask titleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isFrozen(player)) return;
            sendFreezeTitle(player, "freeze.title-still-frozen");
        }, titleInterval, titleInterval);

        BukkitTask chatTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isFrozen(player)) return;
            for (Component line : lang.getList("freeze.chat-reminder")) {
                player.sendMessage(line);
            }
        }, chatInterval, chatInterval);

        freezeTitleTasks.put(id, titleTask);
        freezeChatTasks.put(id, chatTask);
    }

    private void stopFreezeTasks(Player player) {
        UUID id = player.getUniqueId();

        BukkitTask titleTask = freezeTitleTasks.remove(id);
        if (titleTask != null) titleTask.cancel();

        BukkitTask chatTask = freezeChatTasks.remove(id);
        if (chatTask != null) chatTask.cancel();
    }

    // Runs the configured anti-evasion command if a player disconnects while frozen.
    private void punishFrozenDisconnect(Player player) {
        String rawCommand = plugin.getConfig().getString("freeze.disconnect-command", "");
        if (rawCommand == null || rawCommand.isBlank()) return;

        String command = rawCommand
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }

    // ---------------- Freeze chat ----------------

    // A frozen player's own chat is redirected here instead of going out publicly.
    public void sendFreezeChatFromFrozen(Player frozenPlayer, String message) {
        Component formatted = lang.get("freezechat.format-player-to-staff",
                Placeholder.unparsed("player", frozenPlayer.getName()),
                Placeholder.unparsed("message", message));

        broadcastFreezeChat(formatted);
        if (!frozenPlayer.hasPermission("fckstaff.freezechat")) {
            frozenPlayer.sendMessage(formatted);
        }
        plugin.getLogger().info("[FreezeChat] " + frozenPlayer.getName() + " -> Staff: " + message);
    }

    // Used by /freezechat <player> <message> so staff can reply to that frozen player.
    public void sendFreezeChatFromStaff(CommandSender staffSender, Player target, String message) {
        Component formatted = lang.get("freezechat.format-staff-to-player",
                Placeholder.unparsed("staff", staffSender.getName()),
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("message", message));

        broadcastFreezeChat(formatted);
        if (!target.hasPermission("fckstaff.freezechat")) {
            target.sendMessage(formatted);
        }
        plugin.getLogger().info("[FreezeChat] " + staffSender.getName() + " -> " + target.getName() + ": " + message);
    }

    private void broadcastFreezeChat(Component formatted) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("fckstaff.freezechat")) {
                online.sendMessage(formatted);
            }
        }
    }

    // ---------------- Staff chat ----------------

    public boolean toggleStaffChat(Player player) {
        UUID id = player.getUniqueId();
        if (staffChatToggled.contains(id)) {
            staffChatToggled.remove(id);
            return false;
        }
        staffChatToggled.add(id);
        return true;
    }

    public boolean hasStaffChatToggled(Player player) {
        return staffChatToggled.contains(player.getUniqueId());
    }

    public void broadcastStaffChat(Player sender, String message) {
        StaffChatMessageEvent event = new StaffChatMessageEvent(sender, message);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        String finalMessage = event.getMessage();

        var formatted = lang.get("staffchat.format",
                Placeholder.unparsed("player", sender.getName()),
                Placeholder.unparsed("message", finalMessage));

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("fckstaff.staffchat")) {
                online.sendMessage(formatted);
            }
        }
        plugin.getLogger().info("[StaffChat] " + sender.getName() + ": " + finalMessage);
    }

    // ---------------- Utility ----------------

    // Only stops schedulers on disable - staff mode itself stays persisted so
    // it survives the restart.
    public void shutdown() {
        for (BukkitTask task : actionBarTasks.values()) task.cancel();
        actionBarTasks.clear();
        actionBarState.clear();
        actionBarCache.clear();

        for (BukkitTask task : freezeTitleTasks.values()) task.cancel();
        freezeTitleTasks.clear();

        for (BukkitTask task : freezeChatTasks.values()) task.cancel();
        freezeChatTasks.clear();
    }
}
