package com.johnymuffin.legacyping.bukkit;

import com.johnymuffin.legacyping.LPSettings;
import org.bukkit.Bukkit;
import org.bukkit.util.config.Configuration;

import java.io.File;

public class LegacyConfig extends Configuration implements LPSettings {
    private LegacyPingBukkit plugin;

    public LegacyConfig(LegacyPingBukkit plugin) {
        super(new File(plugin.getDataFolder(), "config.yml"));
        this.plugin = plugin;
        this.reload();
    }


    private void write() {
        //Main
        generateConfigOption("config-version", 1);
        //Setting
        generateConfigOption("query-port", (Bukkit.getServer().getPort() + 1));
        generateConfigOption("show-players", true);
        generateConfigOption("show-players-coordinates", true);
        generateConfigOption("show-worlds", true);
        generateConfigOption("show-plugins", true);
        generateConfigOption("show-plugins-versions", false);

    }

    private void generateConfigOption(String key, Object defaultValue) {
        if (this.getProperty(key) == null) {
            this.setProperty(key, defaultValue);
        }
        final Object value = this.getProperty(key);
        this.removeProperty(key);
        this.setProperty(key, value);
    }


    //Getters Start
    public Object getConfigOption(String key) {
        return this.getProperty(key);
    }

    public String getConfigString(String key) {
        return String.valueOf(getConfigOption(key));
    }

    public Integer getConfigInteger(String key) {
        return Integer.valueOf(getConfigString(key));
    }

    public Long getConfigLong(String key) {
        return Long.valueOf(getConfigString(key));
    }

    public Double getConfigDouble(String key) {
        return Double.valueOf(getConfigString(key));
    }

    public Boolean getConfigBoolean(String key) {
        return Boolean.valueOf(getConfigString(key));
    }


    //Getters End


    private void reload() {
        this.load();
        this.write();
        this.save();
    }


    public int getPort() {
        return getConfigInteger("query-port");
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean showPlayers() {
        return getConfigBoolean("show-players");
    }

    public boolean showCoordinates() {
        return getConfigBoolean("show-players-coordinates");
    }

    public boolean showWorlds() {
        return getConfigBoolean("show-worlds");
    }

    public boolean showPlugins() {
        return getConfigBoolean("show-plugins");
    }

    public boolean showPluginVersions() {
        return getConfigBoolean("show-plugins-versions");
    }
}
