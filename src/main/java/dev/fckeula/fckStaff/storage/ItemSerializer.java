package dev.fckeula.fckStaff.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

/** Shared ItemStack[] <-> byte[] conversion for the SQL-backed stores (SQLite, MySQL/MariaDB). */
final class ItemSerializer {

    private static final Logger LOGGER = Logger.getLogger(ItemSerializer.class.getName());

    private ItemSerializer() {}

    static byte[] serialize(ItemStack[] items) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(byteStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.flush();
            return byteStream.toByteArray();
        } catch (Exception e) {
            LOGGER.warning("Could not serialize items: " + e.getMessage());
            return new byte[0];
        }
    }

    static ItemStack[] deserialize(byte[] data) {
        if (data == null || data.length == 0) return new ItemStack[0];

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(byteStream)) {
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (Exception e) {
            LOGGER.warning("Could not deserialize items: " + e.getMessage());
            return new ItemStack[0];
        }
    }
}
