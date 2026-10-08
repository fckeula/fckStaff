package dev.fckeula.fckStaff.storage;

import dev.fckeula.fckStaff.staff.StaffPlayerData;

import java.util.UUID;

/**
 * Persists which players are currently in staff mode (and their saved pre-staff
 * state), so staff mode survives a relog or a full server restart until the
 * player explicitly disables it again with /staffmode.
 *
 * The concrete backend (SQLite, MySQL/MariaDB, or YAML) is chosen via
 * storage.type in config.yml - see FckStaff for how it's picked. All
 * implementations share this contract so StaffManager doesn't need to know
 * which one is active.
 */
public interface StaffDataStore {

    boolean hasEntry(UUID uuid);

    void saveStaffState(UUID uuid, StaffPlayerData staffData, boolean manualVanish);

    void updateManualVanish(UUID uuid, boolean manualVanish);

    void removeEntry(UUID uuid);

    StoredStaffState loadStaffState(UUID uuid);

    void close();

    record StoredStaffState(StaffPlayerData staffData, boolean manualVanish) {}
}
