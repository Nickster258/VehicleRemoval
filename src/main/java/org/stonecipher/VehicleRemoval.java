package org.stonecipher;

import java.io.File;

import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class VehicleRemoval extends JavaPlugin implements Listener, CommandExecutor {

    private FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        setupConfig();
        if (config.getBoolean("proactive_boat_removal")) {
            getLogger().info("Enabling proactive boat removal...");
            new BoatRemovalTask(this, config.getInt("boat_removal_delay"));
        }
        if (config.getBoolean("proactive_cart_removal")) {
            getLogger().info("Enabling proactive cart removal...");
            new CartRemovalTask(this, config.getInt("cart_removal_delay"));

        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    private void setupConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            File file = new File(getDataFolder(), "config.yml");

            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating.");
                config.addDefault("proactive_boat_removal", false);
                config.addDefault("boat_removal_delay", 5);
                config.addDefault("proactive_cart_removal", false);
                config.addDefault("cart_removal_delay", 5);
                config.options().copyDefaults(true);
                saveConfig();
            } else {
                getLogger().info("Config.yml found, loading.");
                config.addDefault("proactive_boat_removal", false);
                config.addDefault("boat_removal_delay", 5);
                config.addDefault("proactive_cart_removal", false);
                config.addDefault("cart_removal_delay", 5);
                config.options().copyDefaults(true);
                saveConfig();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}