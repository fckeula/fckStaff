package dev.fckeula.fckStaff.storage;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.staff.StaffPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * SQLite-backed implementation (staffdata.db), selected via storage.type: sqlite
 * (the default). Zero external setup - just a local file.
 *
 * Writes run async since SQLite fsyncs on every write transaction, which would
 * otherwise stall the main thread (e.g. every /vanish toggle). Reads stay
 * synchronous since their result is needed immediately, but are cheap since
 * they skip the fsync. Everything is synchronized on `lock` so a read never
 * overlaps an in-flight async write on the same Connection.
 */
public class SqliteDataStore implements StaffDataStore {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS staff_data (
                uuid TEXT PRIMARY KEY,
                manual_vanish INTEGER NOT NULL,
                saved_inventory BLOB,
                saved_armor BLOB,
                saved_offhand BLOB,
                saved_world TEXT,
                saved_x REAL,
                saved_y REAL,
                saved_z REAL,
                saved_yaw REAL,
                saved_pitch REAL,
                saved_gamemode TEXT,
                saved_health REAL,
                saved_food INTEGER,
                saved_exp REAL,
                saved_level INTEGER,
                saved_allow_flight INTEGER,
                saved_flying INTEGER
            )
            """;

    private static final String UPSERT = """
            INSERT INTO staff_data (
                uuid, manual_vanish, saved_inventory, saved_armor, saved_offhand,
                saved_world, saved_x, saved_y, saved_z, saved_yaw, saved_pitch,
                saved_gamemode, saved_health, saved_food, saved_exp, saved_level,
                saved_allow_flight, saved_flying
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                manual_vanish = excluded.manual_vanish,
                saved_inventory = excluded.saved_inventory,
                saved_armor = excluded.saved_armor,
                saved_offhand = excluded.saved_offhand,
                saved_world = excluded.saved_world,
                saved_x = excluded.saved_x,
                saved_y = excluded.saved_y,
                saved_z = excluded.saved_z,
                saved_yaw = excluded.saved_yaw,
                saved_pitch = excluded.saved_pitch,
                saved_gamemode = excluded.saved_gamemode,
                saved_health = excluded.saved_health,
                saved_food = excluded.saved_food,
                saved_exp = excluded.saved_exp,
                saved_level = excluded.saved_level,
                saved_allow_flight = excluded.saved_allow_flight,
                saved_flying = excluded.saved_flying
            """;

    private static final String SELECT_ONE = "SELECT 1 FROM staff_data WHERE uuid = ?";
    private static final String SELECT_ALL = "SELECT * FROM staff_data WHERE uuid = ?";
    private static final String UPDATE_VANISH = "UPDATE staff_data SET manual_vanish = ? WHERE uuid = ?";
    private static final String DELETE = "DELETE FROM staff_data WHERE uuid = ?";

    private final FckStaff plugin;
    private final Object lock = new Object();
    private Connection connection;

    public SqliteDataStore(FckStaff plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            plugin.getDataFolder().mkdirs();
            File file = new File(plugin.getDataFolder(), "staffdata.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite driver not found on the classpath. "
                    + "Make sure org.xerial:sqlite-jdbc is declared under libraries: in paper-plugin.yml.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to staffdata.db: " + e.getMessage());
        }
    }

    private void createTable() {
        if (connection == null) return;
        try (PreparedStatement statement = connection.prepareStatement(CREATE_TABLE)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create the staff_data table: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (connection == null) return;
        synchronized (lock) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not close the SQLite connection: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean hasEntry(UUID uuid) {
        if (connection == null) return false;

        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_ONE)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not check staff data for " + uuid + ": " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void saveStaffState(UUID uuid, StaffPlayerData staffData, boolean manualVanish) {
        if (connection == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lock) {
                Location location = staffData.savedLocation();

                try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                    statement.setString(1, uuid.toString());
                    statement.setInt(2, manualVanish ? 1 : 0);
                    statement.setBytes(3, ItemSerializer.serialize(staffData.savedInventory()));
                    statement.setBytes(4, ItemSerializer.serialize(staffData.savedArmor()));
                    statement.setBytes(5, ItemSerializer.serialize(new ItemStack[]{staffData.savedOffhand()}));
                    statement.setString(6, location.getWorld() != null ? location.getWorld().getName() : null);
                    statement.setDouble(7, location.getX());
                    statement.setDouble(8, location.getY());
                    statement.setDouble(9, location.getZ());
                    statement.setFloat(10, location.getYaw());
                    statement.setFloat(11, location.getPitch());
                    statement.setString(12, staffData.savedGameMode().name());
                    statement.setDouble(13, staffData.savedHealth());
                    statement.setInt(14, staffData.savedFoodLevel());
                    statement.setFloat(15, staffData.savedExp());
                    statement.setInt(16, staffData.savedLevel());
                    statement.setInt(17, staffData.savedAllowFlight() ? 1 : 0);
                    statement.setInt(18, staffData.savedFlying() ? 1 : 0);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Could not save staff data for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void updateManualVanish(UUID uuid, boolean manualVanish) {
        if (connection == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lock) {
                try (PreparedStatement statement = connection.prepareStatement(UPDATE_VANISH)) {
                    statement.setInt(1, manualVanish ? 1 : 0);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Could not update manual vanish for " + uuid + ": " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void removeEntry(UUID uuid) {
        if (connection == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> removeEntrySync(uuid));
    }

    private void removeEntrySync(UUID uuid) {
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not remove staff data for " + uuid + ": " + e.getMessage());
            }
        }
    }

    @Override
    public StoredStaffState loadStaffState(UUID uuid) {
        if (connection == null) return null;

        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;

                    World world = Bukkit.getWorld(result.getString("saved_world"));
                    if (world == null) {
                        plugin.getLogger().warning("Saved world for " + uuid + " no longer exists, dropping staff data.");
                        removeEntrySync(uuid);
                        return null;
                    }

                    Location location = new Location(
                            world,
                            result.getDouble("saved_x"),
                            result.getDouble("saved_y"),
                            result.getDouble("saved_z"),
                            result.getFloat("saved_yaw"),
                            result.getFloat("saved_pitch")
                    );

                    ItemStack[] inventory = ItemSerializer.deserialize(result.getBytes("saved_inventory"));
                    ItemStack[] armor = ItemSerializer.deserialize(result.getBytes("saved_armor"));
                    ItemStack[] offhandArray = ItemSerializer.deserialize(result.getBytes("saved_offhand"));
                    ItemStack offhand = offhandArray.length > 0 ? offhandArray[0] : new ItemStack(Material.AIR);

                    StaffPlayerData staffData = new StaffPlayerData(
                            inventory,
                            armor,
                            offhand,
                            location,
                            GameMode.valueOf(result.getString("saved_gamemode")),
                            result.getDouble("saved_health"),
                            result.getInt("saved_food"),
                            result.getFloat("saved_exp"),
                            result.getInt("saved_level"),
                            result.getInt("saved_allow_flight") == 1,
                            result.getInt("saved_flying") == 1
                    );

                    return new StoredStaffState(staffData, result.getInt("manual_vanish") == 1);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not load staff data for " + uuid + ": " + e.getMessage());
                return null;
            }
        }
    }
}
