package dev.spawnershopbridge;

import dev.spawnershopbridge.command.ReloadCommand;
import dev.spawnershopbridge.economy.EconomyManager;
import dev.spawnershopbridge.listener.SellCommandListener;
import dev.spawnershopbridge.shop.ShopConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SpawnerShopBridge
 * <p>
 * A compatibility bridge that lets EconomyShopGUI buy/sell vanilla mob
 * spawners obtained via Silk Touch on Paper 1.21.2.
 * <p>
 * Architecture:
 * <ul>
 *     <li>{@link ShopConfigManager} reads EconomyShopGUI's own shop yml
 *     files directly (read-only) to build a mob-type -> price table,
 *     without modifying EconomyShopGUI in any way.</li>
 *     <li>{@link dev.spawnershopbridge.util.SpawnerUtil} detects vanilla
 *     spawner items and reads their assigned mob type purely through the
 *     Bukkit/Paper API ({@link org.bukkit.inventory.meta.BlockStateMeta} /
 *     {@link org.bukkit.block.CreatureSpawner}), with no NMS involved.</li>
 *     <li>{@link SellCommandListener} is the fallback described in the
 *     project requirements: it intercepts "/sell hand" via
 *     {@link org.bukkit.event.player.PlayerCommandPreprocessEvent} before
 *     EconomyShopGUI's command executes, and if the held item is a priced
 *     vanilla spawner, completes the sale itself through Vault and cancels
 *     the command. Anything it doesn't recognize is passed straight
 *     through to EconomyShopGUI untouched.</li>
 *     <li>{@link EconomyManager} wraps the Vault economy provider used to
 *     pay the player.</li>
 * </ul>
 */
public final class SpawnerShopBridge extends JavaPlugin {

    private EconomyManager economyManager;
    private ShopConfigManager shopConfigManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().warning("No Vault economy provider was found. SpawnerShopBridge will not be able "
                    + "to complete spawner sales until an economy plugin is installed alongside Vault.");
        }

        shopConfigManager = new ShopConfigManager(this);
        shopConfigManager.reload();

        checkSoftDependencies();
        registerListeners();
        registerCommands();

        getLogger().info("SpawnerShopBridge enabled with " + shopConfigManager.size() + " spawner price entries.");
    }

    @Override
    public void onDisable() {
        // No persistent resources (schedulers, connections, files held open) to release.
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopConfigManager getShopConfigManager() {
        return shopConfigManager;
    }

    private void checkSoftDependencies() {
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") == null) {
            getLogger().warning("EconomyShopGUI was not detected. SpawnerShopBridge was designed to "
                    + "complement it and will read its shop files once it is installed.");
        }
        if (Bukkit.getPluginManager().getPlugin("RoseStacker") == null) {
            getLogger().info("RoseStacker was not detected. This is fine - SpawnerShopBridge only needs "
                    + "vanilla spawner items and does not require RoseStacker to function.");
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new SellCommandListener(this), this);
    }

    private void registerCommands() {
        ReloadCommand reloadCommand = new ReloadCommand(this);
        var command = getCommand("spawnershopbridge");
        if (command != null) {
            command.setExecutor(reloadCommand);
        }
    }
}
