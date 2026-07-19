package dev.spawnershopbridge.shop;

import dev.spawnershopbridge.SpawnerShopBridge;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads EconomyShopGUI's own shop configuration files directly, without
 * touching EconomyShopGUI's source or internals, to build a lookup table
 * of {@link EntityType} -> buy/sell price for spawner shop entries such as:
 *
 * <pre>
 * zombie_spawner:
 *   material: SPAWNER
 *   spawnertype: ZOMBIE
 *   buy: 120000
 *   sell: 60000
 * </pre>
 *
 * The parser is intentionally schema-tolerant: it looks for any
 * configuration section that has both a "material: SPAWNER" entry and a
 * "spawnertype" entry, wherever it is nested, and accepts a few common
 * key spellings for the buy/sell prices so it keeps working across
 * EconomyShopGUI config revisions.
 */
public class ShopConfigManager {

    private static final String[] BUY_KEYS = {"buy", "price-buy", "buyprice", "price_buy", "buy-price"};
    private static final String[] SELL_KEYS = {"sell", "price-sell", "sellprice", "price_sell", "sell-price"};

    private final SpawnerShopBridge plugin;
    private final Map<EntityType, SpawnerShopEntry> entries = new EnumMap<>(EntityType.class);

    public ShopConfigManager(SpawnerShopBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * Clears and rebuilds the price table by scanning EconomyShopGUI's shop files on disk.
     */
    public void reload() {
        entries.clear();

        File shopsFolder = resolveShopsFolder();
        if (shopsFolder == null || !shopsFolder.isDirectory()) {
            plugin.getLogger().warning("Could not locate EconomyShopGUI's shops folder at '"
                    + (shopsFolder == null ? "unknown" : shopsFolder.getPath())
                    + "'. Spawner price lookup will be empty until this is fixed and /ssb reload is run.");
            return;
        }

        try (Stream<File> files = listYamlFilesRecursively(shopsFolder)) {
            files.forEach(this::scanFile);
        }

        plugin.getLogger().info("Loaded " + entries.size() + " spawner shop price entries from '" + shopsFolder.getPath() + "'.");
    }

    public Optional<SpawnerShopEntry> getEntry(EntityType type) {
        return Optional.ofNullable(entries.get(type));
    }

    public int size() {
        return entries.size();
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private File resolveShopsFolder() {
        String economyShopGuiFolderName = plugin.getConfig().getString("economyshopgui-folder", "EconomyShopGUI");
        String shopsSubfolder = plugin.getConfig().getString("shops-subfolder", "shops");

        Plugin economyShopGui = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
        File baseFolder = economyShopGui != null
                ? economyShopGui.getDataFolder()
                : new File(pluginsFolder(), economyShopGuiFolderName);

        return new File(baseFolder, shopsSubfolder);
    }

    private File pluginsFolder() {
        // plugin.getDataFolder() == plugins/SpawnerShopBridge, so its parent is the plugins/ folder.
        return plugin.getDataFolder().getParentFile();
    }

    private Stream<File> listYamlFilesRecursively(File root) {
        File[] children = root.listFiles();
        if (children == null) {
            return Stream.empty();
        }

        return Stream.of(children).flatMap(file -> {
            if (file.isDirectory()) {
                return listYamlFilesRecursively(file);
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                return Stream.of(file);
            }
            return Stream.empty();
        });
    }

    private void scanFile(File file) {
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse shop file '" + file.getPath() + "': " + e.getMessage());
            return;
        }
        scanSection(config, file);
    }

    private void scanSection(ConfigurationSection section, File sourceFile) {
        for (String key : section.getKeys(false)) {
            if (!(section.get(key) instanceof ConfigurationSection sub)) {
                continue;
            }

            if (isSpawnerEntry(sub)) {
                registerEntry(sub, sourceFile);
            } else {
                scanSection(sub, sourceFile);
            }
        }
    }

    private boolean isSpawnerEntry(ConfigurationSection section) {
        String material = section.getString("material", "");
        return material.equalsIgnoreCase("SPAWNER") && section.isSet("spawnertype");
    }

    private void registerEntry(ConfigurationSection section, File sourceFile) {
        String rawType = section.getString("spawnertype", "");
        EntityType type = parseEntityType(rawType);

        if (type == null) {
            plugin.getLogger().warning("Unknown spawnertype '" + rawType + "' in '" + sourceFile.getPath()
                    + "' at '" + section.getCurrentPath() + "' - skipping this entry.");
            return;
        }

        double buy = readPrice(section, BUY_KEYS);
        double sell = readPrice(section, SELL_KEYS);

        entries.put(type, new SpawnerShopEntry(type, buy, sell));
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double readPrice(ConfigurationSection section, String[] keys) {
        for (String key : keys) {
            if (section.isSet(key)) {
                return section.getDouble(key, -1);
            }
        }
        return -1;
    }
}
