package dev.spawnershopbridge.shop;

import org.bukkit.entity.EntityType;

/**
 * A single spawner price entry, mirroring one "spawnertype" section
 * found in an EconomyShopGUI shop yml file.
 *
 * @param type      the mob type the spawner spawns
 * @param buyPrice  price to buy this spawner, -1 if not configured/buyable
 * @param sellPrice price to sell this spawner, -1 if not configured/sellable
 */
public record SpawnerShopEntry(EntityType type, double buyPrice, double sellPrice) {

    public boolean isSellable() {
        return sellPrice >= 0;
    }

    public boolean isBuyable() {
        return buyPrice >= 0;
    }
}
