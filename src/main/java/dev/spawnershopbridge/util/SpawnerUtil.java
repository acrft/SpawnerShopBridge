package dev.spawnershopbridge.util;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Optional;

/**
 * Pure Bukkit/Paper API helpers for reading vanilla spawner items.
 * <p>
 * Deliberately avoids NMS: everything here is done through
 * {@link BlockStateMeta} / {@link CreatureSpawner}, which Paper keeps
 * in sync with the underlying block entity data (the modern
 * "block_entity_data" component), regardless of how the item was
 * obtained (Silk Touch break, /give, creative inventory, etc).
 */
public final class SpawnerUtil {

    private SpawnerUtil() {
    }

    /**
     * @param item the item to check, may be null
     * @return true if the item is a vanilla spawner block item
     */
    public static boolean isSpawner(ItemStack item) {
        return item != null && item.getType() == Material.SPAWNER;
    }

    /**
     * Reads the mob type stored in a spawner item's block state data.
     *
     * @param item the spawner item
     * @return the spawned {@link EntityType}, or empty if the item is not a
     * spawner, has no block state, or has no configured spawn entity
     * (i.e. an "empty" spawner with no mob assigned).
     */
    public static Optional<EntityType> getSpawnedEntityType(ItemStack item) {
        if (!isSpawner(item)) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return Optional.empty();
        }

        if (!blockStateMeta.hasBlockState()) {
            return Optional.empty();
        }

        BlockState state = blockStateMeta.getBlockState();
        if (!(state instanceof CreatureSpawner spawner)) {
            return Optional.empty();
        }

        EntityType type = spawner.getSpawnedType();
        if (type == null || type == EntityType.UNKNOWN) {
            return Optional.empty();
        }

        return Optional.of(type);
    }

    /**
     * Produces a human readable name for an entity type, e.g. CAVE_SPIDER -> "Cave Spider".
     */
    public static String formatEntityName(EntityType type) {
        String[] parts = type.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
