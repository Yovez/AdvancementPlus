package com.yovez.advancementPlus.utils;

/*
 * CustomConfig.java
 *
 * A simple utility class for managing custom YAML configuration files
 * in Spigot/Paper plugins.
 *
 * Supports loading, saving, reloading, default resource configs,
 * and optional subdirectory paths.
 *
 * Author: Yovez
 * License: MIT
 * Use freely in personal or commercial projects.
 */

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.logging.Level;

public class CustomConfig {

    // The in-memory YAML configuration
    private YamlConfiguration customConfig;

    // The physical config file on disk
    private File customConfigFile;

    // Reference to the main plugin instance
    private final Plugin plugin;

    // Name of the config file (without .yml)
    private final String configName;

    // Optional subfolder path inside the plugin data folder
    private final String path;

    // Whether this config has a default version bundled as a plugin resource
    private final boolean isResource;

    /**
     * Constructor for configs stored in the plugin root folder.
     *
     * @param plugin     Plugin instance
     * @param configName Config file name (without .yml)
     * @param isResource Whether a default config exists in the plugin jar
     */
    public CustomConfig(Plugin plugin, String configName, boolean isResource) {
        this.plugin = plugin;
        this.configName = configName;
        this.isResource = isResource;
        this.path = "";

        // Create a file reference to plugins/YourPlugin/configName.yml
        this.customConfigFile = new File(plugin.getDataFolder() + "/" + path, configName + ".yml");
    }

    /**
     * Constructor for configs stored in a subfolder.
     *
     * @param plugin     Plugin instance
     * @param configName Config file name (without .yml)
     * @param path       Subfolder path inside the plugin data folder
     */
    public CustomConfig(Plugin plugin, String configName, String path) {
        this.plugin = plugin;
        this.configName = configName;
        this.path = path;
        this.isResource = false;

        // Create a file reference to plugins/YourPlugin/path/configName.yml
        this.customConfigFile = new File(plugin.getDataFolder() + "/" + path, configName + ".yml");
    }

    /**
     * Reloads the configuration from disk.
     * If a default resource exists, it will be used as fallback defaults.
     */
    public void reloadConfig() {
        // Load the config file from disk
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // If this config has a default version bundled in the jar
        if (isResource) {
            Reader defConfigStream = null;

            try {
                // Load the default config from the plugin resources
                defConfigStream = new InputStreamReader(
                        plugin.getResource(configName + ".yml"), "UTF8"
                );
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // Apply default values if present
            if (defConfigStream != null) {
                YamlConfiguration defConfig =
                        YamlConfiguration.loadConfiguration(defConfigStream);
                customConfig.setDefaults(defConfig);
            }
        }
    }

    /**
     * Returns the loaded configuration.
     * Automatically reloads it if it hasn't been loaded yet.
     *
     * @return YamlConfiguration instance
     */
    public YamlConfiguration getConfig() {
        if (customConfig == null) {
            reloadConfig();
        }
        return customConfig;
    }

    /**
     * Saves the current configuration to disk.
     */
    public void saveConfig() {
        try {
            customConfig.save(customConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not save config to " + customConfigFile,
                    ex
            );
        }
    }

    /**
     * Saves the default config from the plugin jar
     * if the file does not already exist.
     */
    public void saveDefaultConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(
                    plugin.getDataFolder() + "/" + path,
                    configName + ".yml"
            );
        }

        // Only copy the resource if the file does not exist yet
        if (!customConfigFile.exists()) {
            plugin.saveResource(configName + ".yml", false);
        }
    }

    /**
     * Returns the underlying config file.
     *
     * @return File reference to the config file
     */
    public File getCustomConfigFile() {
        return customConfigFile;
    }
}