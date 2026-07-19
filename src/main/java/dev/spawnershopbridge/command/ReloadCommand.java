package dev.spawnershopbridge.command;

import dev.spawnershopbridge.SpawnerShopBridge;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Handles "/spawnershopbridge reload" (aliased to "/ssb reload").
 */
public class ReloadCommand implements CommandExecutor {

    private final SpawnerShopBridge plugin;

    public ReloadCommand(SpawnerShopBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reload");
            return true;
        }

        plugin.reloadConfig();
        plugin.getShopConfigManager().reload();

        String message = plugin.getConfig().getString("messages.reload-success",
                        "&aSpawnerShopBridge configuration reloaded. %count% spawner price entries loaded.")
                .replace("%count%", String.valueOf(plugin.getShopConfigManager().size()));

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return true;
    }
}
