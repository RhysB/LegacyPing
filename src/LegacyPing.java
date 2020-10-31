import com.johnymuffin.legacyping.AFKInfo;
import com.johnymuffin.legacyping.LPSettings;
import com.johnymuffin.legacyping.LegacyPingImplimentation;
import com.johnymuffin.legacyping.PluginInformation;
import com.johnymuffin.legacyping.QueryServer;
import com.johnymuffin.legacyping.simplejson.JSONArray;
import com.johnymuffin.legacyping.simplejson.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LegacyPing extends Plugin implements LegacyPingImplimentation {

    private Logger log = Logger.getLogger("Minecraft");
    private Server server;
    private LPSettings lpSettings;
    private int serverPort;
    public AFKInfo afkInfo;
    //cached json response
    private JSONObject response;
    private Object syncLock = new Object();
    private QueryServer queryServer;

    @Override
    public void enable() {
        this.server = etc.getServer();
        log(Level.INFO, "Enabling, Version: " + PluginInformation.getPluginVersion());
        serverPort = 25565;
        lpSettings = new LPHConfig(new File("/legacyping/Config.properties"), serverPort + 1);
        AFKTracker afkTracker = new AFKTracker();
        etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, afkTracker, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.LOGIN, afkTracker, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, afkTracker, this, PluginListener.Priority.MEDIUM);
        afkInfo = afkTracker;
        updateJSON();

        //Start Query Server

        try {
            this.queryServer = new QueryServer(this, "ANY", lpSettings.getPort());
            this.queryServer.start();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error initializing LegacyPing", ex);
            return;
        }


    }

    @Override
    public void disable() {
        try {
            queryServer.getListener().close();
        } catch (IOException ex) {
            log.log(Level.WARNING, "Unable to close the LegacyPing listener", ex);
        }

        log(Level.INFO, "Disabling plugin");
    }


    public void log(Level level, String string) {
        log.log(level, "[" + PluginInformation.getPluginName() + "] " + string);
    }

    public void updateJSON() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverPort", serverPort);
        jsonObject.put("playerCount", server.getPlayerList().size());
        jsonObject.put("version", PluginInformation.getPluginVersion());
        if (lpSettings.showPlayers()) {
            JSONArray playerList = new JSONArray();
            for (Player player : server.getPlayerList()) {
                if(!player.isConnected()) {
                    continue;
                }
                JSONObject playerObject = new JSONObject();
                playerObject.put("name", player.getName());
                playerObject.put("displayName", player.getName());
                if (lpSettings.showCoordinates()) {
                    playerObject.put("world", String.valueOf(player.getLocation().dimension));
                    playerObject.put("x", player.getLocation().x);
                    playerObject.put("y", player.getLocation().y);
                    playerObject.put("z", player.getLocation().z);
                }
                playerObject.put("isAlive", true);
                playerObject.put("isInVehicle", true);
                playerObject.put("lastMove", afkInfo.lastPlayerMove(player));
                playerList.add(playerObject);
            }
            jsonObject.put("players", playerList);
        }
        if (lpSettings.showWorlds()) {

        }

        if (lpSettings.showPlugins()) {

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

    public String getClientMod() {
        return "HMOD";
    }

    public class AFKTracker extends PluginListener implements AFKInfo {
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

        public void onPlayerMove(Player player, Location from, Location to) {
            if (from.x != to.x || from.z != to.x) {
                lastMoveTime.put(player.getName().toLowerCase(), System.currentTimeMillis() / 1000L);
                updateJSON();
            }
        }

        public void onLogin(Player player) {
            lastMoveTime.put(player.getName().toLowerCase(), (System.currentTimeMillis() / 1000L));
            updateJSON();
        }

        public void onDisconnect(Player player) {
            lastMoveTime.remove(player.getName().toLowerCase());
            updateJSON();
        }

    }
}
