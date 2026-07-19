package dev.spawnershopbridge.listener;

import dev.spawnershopbridge.SpawnerShopBridge;
import dev.spawnershopbridge.shop.SpawnerShopEntry;
import dev.spawnershopbridge.util.SpawnerUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Fallback sell path (see requirement #15 of the project spec).
 * <p>
 * EconomyShopGUI's own spawner detection does not reliably read the mob
 * type of vanilla spawners obtained via Silk Touch on some server
 * versions/builds, so "/sell hand" reports "No items found which can be
 * sold" even though the shop config already has a matching
 * "spawnertype" entry.
 * <p>
 * This listener runs before EconomyShopGUI processes the command. If the
 * player is holding a vanilla spawner with a mob type that has a
 * matching entry in EconomyShopGUI's own shop files, SpawnerShopBridge
 * performs the sale itself (Vault deposit + item removal) and cancels
 * the command so EconomyShopGUI never sees it.
 * <p>
 * Anything that is not a recognized, priced spawner is left completely
 * untouched and passed through to EconomyShopGUI as normal.
 */
public class SellCommandListener implements Listener {

    private final SpawnerShopBridge plugin;

    public SellCommandListener(SpawnerShopBridge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("intercept-sell-hand", true)) {
            return;
        }

        if (!isSellHandCommand(event.getMessage())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("spawnershopbridge.sell")) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!SpawnerUtil.isSpawner(heldItem)) {
            // Not a spawner at all - let EconomyShopGUI handle it as usual.
            return;
        }

        Optional<EntityType> spawnedType = SpawnerUtil.getSpawnedEntityType(heldItem);
        if (spawnedType.isEmpty()) {
            // A spawner with no assigned mob (rare) - nothing SpawnerShopBridge can price.
            return;
        }

        Optional<SpawnerShopEntry> entryOptional = plugin.getShopConfigManager().getEntry(spawnedType.get());
        if (entryOptional.isEmpty() || !entryOptional.get().isSellable()) {
            // No matching priced entry in EconomyShopGUI's shop config -
            // let EconomyShopGUI attempt it and produce its own message.
            return;
        }

        SpawnerShopEntry entry = entryOptional.get();

        if (!plugin.getEconomyManager().isReady()) {
            event.setCancelled(true);
            player.sendMessage(colorize(plugin.getConfig().getString("messages.no-economy",
                    "&cEconomy is not available. Please contact an administrator.")));
            return;
        }

        // We are handling this sale ourselves - stop EconomyShopGUI from processing the command.
        event.setCancelled(true);

        int amount = heldItem.getAmount();
        double totalPrice = entry.sellPrice() * amount;

        player.getInventory().setItemInMainHand(null);
        plugin.getEconomyManager().deposit(player, totalPrice);

        String message = plugin.getConfig().getString("messages.sold",
                        "&aSold &f%amount%x %mob% Spawner&a for &6%price%&a.")
                .replace("%amount%", String.valueOf(amount))
                .replace("%mob%", SpawnerUtil.formatEntityName(spawnedType.get()))
                .replace("%price%", plugin.getEconomyManager().format(totalPrice));

        player.sendMessage(colorize(message));
    }

    /**
     * Matches "/sell hand" (and "sell hand" without a leading slash, which
     * Bukkit's PlayerCommandPreprocessEvent always includes) case
     * insensitively, ignoring any extra whitespace.
     */
    private boolean isSellHandCommand(String rawMessage) {
        String message = rawMessage.trim();
        if (message.isEmpty() || message.charAt(0) != '/') {
            return false;
        }

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 2) {
            return false;
        }

        return parts[0].equalsIgnoreCase("sell") && parts[1].equalsIgnoreCase("hand");
    }

    private String colorize(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
