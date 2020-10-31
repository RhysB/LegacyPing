package com.johnymuffin.legacyping.bukkit;

import com.johnymuffin.legacyping.AFKInfo;
import com.johnymuffin.legacyping.LPSettings;
import com.johnymuffin.legacyping.LegacyPingImplimentation;
import com.johnymuffin.legacyping.QueryServer;
import com.johnymuffin.legacyping.simplejson.JSONArray;
import com.johnymuffin.legacyping.simplejson.JSONObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LegacyPingBukkit extends JavaPlugin implements LegacyPingImplimentation {
    //Basic Plugin Info
    private static LegacyPingBukkit plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;
    private LPSettings lpSettings;
    public final String configFile = "server.properties";
    //cached json response
    private JSONObject response = new JSONObject();
    private long lastUpdate = 0;
    private Object syncLock = new Object();
    private QueryServer queryServer;
    private AFKTracker afkTracker;

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());
        lpSettings = new LegacyConfig(plugin);

        afkTracker = new AFKTracker();
        Bukkit.getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, afkTracker, Event.Priority.Lowest, plugin);
        Bukkit.getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, afkTracker, Event.Priority.Lowest, plugin);
        Bukkit.getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, afkTracker, Event.Priority.Lowest, plugin);

        updateJSON();
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            synchronized (syncLock) {
                updateJSON();
            }
        }, 0, 20 * 5);
        //Load serverIP
        String serverIP = "";
        try {
            Properties props = new Properties();
            props.load(new FileReader(configFile));
            serverIP = props.getProperty("server-ip", "ANY");
        } catch (IOException exception) {
            log(Level.WARNING, "Failed to get server-ip from server.properties. Shutting down!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        //Start Query Server
        try {
            queryServer = new QueryServer(this, serverIP, lpSettings.getPort());
            queryServer.start();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error initializing LegacyPing", ex);
            return;
        }

    }

    public void onDisable() {
        try {
            queryServer.getListener().close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Unable to close the LegacyPing listener", ex);
        }

        log.info("[" + pluginName + "] Plugin disabled.");
    }

    public void log(Level level, String string) {
        log.log(level, "[" + pluginName + "] " + string);
    }

    private void updateJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverPort", Bukkit.getPort());
        jsonObject.put("playerCount", Bukkit.getServer().getOnlinePlayers().length);
        jsonObject.put("version", pdf.getVersion());
        if (lpSettings.showPlayers()) {
            JSONArray playerList = new JSONArray();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                JSONObject playerObject = new JSONObject();
                playerObject.put("name", player.getName());
                playerObject.put("displayName", player.getName());
                if (lpSettings.showCoordinates()) {
                    playerObject.put("world", player.getWorld().getName());
                    playerObject.put("x", player.getLocation().getX());
                    playerObject.put("y", player.getLocation().getY());
                    playerObject.put("z", player.getLocation().getZ());
                }
                playerObject.put("isAlive", !player.isDead());
                playerObject.put("isInVehicle", player.isInsideVehicle());
                playerObject.put("lastMove", afkTracker.lastPlayerMove(player));
                playerList.add(playerObject);
            }
            jsonObject.put("players", playerList);
        }
        if (lpSettings.showWorlds()) {
            JSONArray worldList = new JSONArray();
            for (World w : Bukkit.getServer().getWorlds()) {
                JSONObject worldObject = new JSONObject();
                worldObject.put("name", w.getName());
                worldObject.put("uuid", String.valueOf(w.getUID()));
                worldObject.put("gameTicks", w.getTime());
                worldObject.put("worldTicks", w.getFullTime());
                worldObject.put("rain", w.isThundering() || w.hasStorm());
                worldList.add(worldObject);
            }
            jsonObject.put("worlds", worldList);
        }

        if (lpSettings.showPlugins()) {
            JSONArray pluginList = new JSONArray();
            for (Plugin p : Bukkit.getServer().getPluginManager().getPlugins()) {
                JSONObject pluginObject = new JSONObject();
                pluginObject.put("name", p.getDescription().getName());
                if (lpSettings.showPluginVersions()) {
                    pluginObject.put("version", p.getDescription().getVersion());
                }
                pluginList.add(pluginObject);
            }
            jsonObject.put("plugins", pluginList);
        }

        synchronized (syncLock) {
            response = jsonObject;
        }
    }


    public JSONObject jsonResponse() {
        synchronized (syncLock) {
            return (JSONObject) response.clone();
        }
    }

    @Override
    public String getClientMod() {
        return "BUKKIT";
    }

    public class AFKTracker extends PlayerListener implements AFKInfo {
        private HashMap<String, Long> lastMoveTime = new HashMap<>();

        public boolean hasPlayerMoved(Object rawPlayer, int seconds) {
            Player player = (Player) rawPlayer;
            String sanitizedName = player.getName().toLowerCase();
            if (!lastMoveTime.containsKey(sanitizedName)) {
                return false;
            }
            return ((System.currentTimeMillis() / 1000L) < (lastMoveTime.get(sanitizedName) + seconds));
        }

        public int lastPlayerMove(Object rawPlayer) {
            Player player = (Player) rawPlayer;
            String sanitizedName = player.getName().toLowerCase();
            if (!lastMoveTime.containsKey(sanitizedName)) {
                return 0;
            }
            return Integer.valueOf((int) ((System.currentTimeMillis() / 1000L) - lastMoveTime.get(sanitizedName)));
        }


        public void onPlayerJoin(PlayerJoinEvent event) {
            if (event != null && event.getPlayer() != null) {
                lastMoveTime.put(event.getPlayer().getName().toLowerCase(), (System.currentTimeMillis() / 1000L));
            }
        }

        public void onPlayerQuit(PlayerQuitEvent event) {
            if (event != null && event.getPlayer() != null) {
                lastMoveTime.remove(event.getPlayer().getName().toLowerCase());
            }
        }

        public void onPlayerMove(PlayerMoveEvent event) {
            if (event != null && event.getPlayer() != null) {
                if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    lastMoveTime.put(event.getPlayer().getName().toLowerCase(), System.currentTimeMillis() / 1000L);
                }
            }
        }
    }

}
