package dev.spawnershopbridge.economy;

import dev.spawnershopbridge.SpawnerShopBridge;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Thin wrapper around Vault's {@link Economy} service, used to deposit
 * funds when SpawnerShopBridge completes a sale itself (fallback path).
 */
public class EconomyManager {

    private final SpawnerShopBridge plugin;
    private Economy economy;

    public EconomyManager(SpawnerShopBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * Locates the Vault economy provider.
     *
     * @return true if an economy provider was found and is ready to use
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }

        this.economy = registration.getProvider();
        return this.economy != null;
    }

    public boolean isReady() {
        return economy != null;
    }

    /**
     * Deposits an amount into a player's account.
     *
     * @return true if the deposit succeeded
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isReady()) {
            plugin.getLogger().warning("Attempted to deposit funds but no economy provider is available.");
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (isReady()) {
            return economy.format(amount);
        }
        return String.format("%.2f", amount);
    }
}
