import com.johnymuffin.legacyping.LPSettings;

import java.io.File;

public class LPHConfig implements LPSettings {
    private boolean newFile = false;
    private PropertiesFile propertiesFile;
    private int defaultPort;

    public LPHConfig(File file, int defaultPort) {
        this.defaultPort = defaultPort;
        if (!file.exists()) {
            newFile = true;
            file.getParentFile().mkdirs();
        }
        propertiesFile = new PropertiesFile(file.getAbsolutePath());
        write();
        propertiesFile.save();
    }

    public void write() {
        //Main
        generateConfigOption("config-version", 1);
        //Setting
        generateConfigOption("query-port", defaultPort);
        generateConfigOption("show-players", true);
        generateConfigOption("show-players-coordinates", true);
        generateConfigOption("show-worlds", true);
        generateConfigOption("show-plugins", true);
        generateConfigOption("show-plugins-versions", false);
    }

    private void generateConfigOption(String key, Object defaultValue) {
        if (propertiesFile.getProperty(key) == null) {
            propertiesFile.setString(key, String.valueOf(defaultValue));
        }
        final Object value = propertiesFile.getProperty(key);
        propertiesFile.removeKey(key);
        propertiesFile.setString(key, String.valueOf(value));
    }


    public int getPort() {
        return Integer.valueOf(propertiesFile.getString("query-port", "25566"));
    }

    public String getVersion() {
        return null;
    }

    public boolean showPlayers() {
        return Boolean.valueOf(propertiesFile.getString("show-players", "true"));
    }

    public boolean showCoordinates() {
        return Boolean.valueOf(propertiesFile.getString("show-players-coordinates", "false"));
    }

    public boolean showWorlds() {
        return false;
    }

    public boolean showPlugins() {
        return false;
    }

    public boolean showPluginVersions() {
        return false;
    }
}
