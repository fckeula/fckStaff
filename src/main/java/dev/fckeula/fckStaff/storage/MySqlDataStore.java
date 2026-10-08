package dev.fckeula.fckStaff.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.staff.StaffPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * MySQL/MariaDB-backed implementation, selected via storage.type: mysql or mariadb.
 * Connection details come from the storage.mysql section in config.yml (shared by
 * both, since the fields are identical).
 *
 * Unlike the local SQLite file, a remote connection can silently drop after a
 * period of inactivity, so this uses a small HikariCP pool instead of a single
 * long-lived Connection. Writes still run async to avoid blocking the main
 * thread on network I/O.
 */
public class MySqlDataStore implements StaffDataStore {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS staff_data (
                uuid VARCHAR(36) PRIMARY KEY,
                manual_vanish TINYINT(1) NOT NULL,
                saved_inventory MEDIUMBLOB,
                saved_armor MEDIUMBLOB,
                saved_offhand MEDIUMBLOB,
                saved_world VARCHAR(255),
                saved_x DOUBLE,
                saved_y DOUBLE,
                saved_z DOUBLE,
                saved_yaw FLOAT,
                saved_pitch FLOAT,
                saved_gamemode VARCHAR(32),
                saved_health DOUBLE,
                saved_food INT,
                saved_exp FLOAT,
                saved_level INT,
                saved_allow_flight TINYINT(1),
                saved_flying TINYINT(1)
            )
            """;

    private static final String UPSERT = """
            INSERT INTO staff_data (
                uuid, manual_vanish, saved_inventory, saved_armor, saved_offhand,
                saved_world, saved_x, saved_y, saved_z, saved_yaw, saved_pitch,
                saved_gamemode, saved_health, saved_food, saved_exp, saved_level,
                saved_allow_flight, saved_flying
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                manual_vanish = VALUES(manual_vanish),
                saved_inventory = VALUES(saved_inventory),
                saved_armor = VALUES(saved_armor),
                saved_offhand = VALUES(saved_offhand),
                saved_world = VALUES(saved_world),
                saved_x = VALUES(saved_x),
                saved_y = VALUES(saved_y),
                saved_z = VALUES(saved_z),
                saved_yaw = VALUES(saved_yaw),
                saved_pitch = VALUES(saved_pitch),
                saved_gamemode = VALUES(saved_gamemode),
                saved_health = VALUES(saved_health),
                saved_food = VALUES(saved_food),
                saved_exp = VALUES(saved_exp),
                saved_level = VALUES(saved_level),
                saved_allow_flight = VALUES(saved_allow_flight),
                saved_flying = VALUES(saved_flying)
            """;

    private static final String SELECT_ONE = "SELECT 1 FROM staff_data WHERE uuid = ?";
    private static final String SELECT_ALL = "SELECT * FROM staff_data WHERE uuid = ?";
    private static final String UPDATE_VANISH = "UPDATE staff_data SET manual_vanish = ? WHERE uuid = ?";
    private static final String DELETE = "DELETE FROM staff_data WHERE uuid = ?";

    private final FckStaff plugin;
    private HikariDataSource dataSource;

    public MySqlDataStore(FckStaff plugin, boolean mariadb) {
        this.plugin = plugin;
        connect(mariadb);
        createTable();
    }

    private void connect(boolean mariadb) {
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "fckstaff");
        String username = plugin.getConfig().getString("storage.mysql.username", "root");
        String password = plugin.getConfig().getString("storage.mysql.password", "");
        String parameters = plugin.getConfig().getString("storage.mysql.parameters",
                "useSSL=false&autoReconnect=true&characterEncoding=utf8");

        String driverClass = mariadb ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver";
        String urlPrefix = mariadb ? "jdbc:mariadb://" : "jdbc:mysql://";

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("JDBC driver not found on the classpath (" + driverClass + "). "
                    + "Make sure it's declared under libraries: in paper-plugin.yml.");
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(urlPrefix + host + ":" + port + "/" + database + "?" + parameters);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClass);
        hikariConfig.setPoolName("fckStaff-Pool");
        hikariConfig.setMaximumPoolSize(4);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(10_000);
        hikariConfig.setIdleTimeout(600_000);
        hikariConfig.setMaxLifetime(1_800_000);

        try {
            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to the " + (mariadb ? "MariaDB" : "MySQL")
                    + " database: " + e.getMessage());
        }
    }

    private void createTable() {
        if (dataSource == null) return;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create the staff_data table: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public boolean hasEntry(UUID uuid) {
        if (dataSource == null) return false;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ONE)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not check staff data for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void saveStaffState(UUID uuid, StaffPlayerData staffData, boolean manualVanish) {
        if (dataSource == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Location location = staffData.savedLocation();

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(UPSERT)) {
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
        });
    }

    @Override
    public void updateManualVanish(UUID uuid, boolean manualVanish) {
        if (dataSource == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(UPDATE_VANISH)) {
                statement.setInt(1, manualVanish ? 1 : 0);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not update manual vanish for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void removeEntry(UUID uuid) {
        if (dataSource == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> removeEntrySync(uuid));
    }

    private void removeEntrySync(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not remove staff data for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public StoredStaffState loadStaffState(UUID uuid) {
        if (dataSource == null) return null;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL)) {
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
