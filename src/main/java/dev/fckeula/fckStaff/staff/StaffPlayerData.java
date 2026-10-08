package dev.fckeula.fckStaff.staff;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/** Immutable snapshot of a player's state right before entering staff mode. */
public record StaffPlayerData(
        ItemStack[] savedInventory,
        ItemStack[] savedArmor,
        ItemStack savedOffhand,
        Location savedLocation,
        GameMode savedGameMode,
        double savedHealth,
        int savedFoodLevel,
        float savedExp,
        int savedLevel,
        boolean savedAllowFlight,
        boolean savedFlying
) {}
