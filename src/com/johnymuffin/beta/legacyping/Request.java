package com.johnymuffin.beta.legacyping;

import com.johnymuffin.beta.legacyping.simplejson.JSONArray;
import com.johnymuffin.beta.legacyping.simplejson.JSONObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles Minequery requests.
 *
 * @author Kramer Campbell
 * @author Blake Beaupain
 * @since 1.2
 */
public final class Request extends Thread {
    /**
     * The parent plugin object.
     */
    private final LegacyPing legacyPing;

    /**
     * The socket we are using to obtain a request.
     */
    private final Socket socket;

    /**
     * The logging utility.
     */
    private final Logger log = Logger.getLogger("Minecraft");


    private LegacyConfig config;

    /**
     * Creates a new <code>QueryServer</code> object.
     *
     * @param legacyPing The parent plugin object
     * @param socket     The socket we are using to obtain a request
     */
    public Request(LegacyPing legacyPing, Socket socket) {
        this.legacyPing = legacyPing;
        this.socket = socket;
        config = this.legacyPing.getConfig();
    }

    /**
     * Listens for a request.
     */
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Read the request and handle it.
            handleRequest(socket, reader.readLine());

            // Finally close the socket.
            socket.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Minequery server thread shutting down", ex);
        }
    }

    /**
     * Handles a received request.
     *
     * @param request The request message
     * @throws java.io.IOException If an I/O error occurs
     */
    private void handleRequest(Socket socket, String request) throws IOException {
        // Handle a query request.
        if (request == null) {
            return;
        }

        if (request.equalsIgnoreCase("QUERY")) {
            // Handle a standard Minequery request. - DEPRECATED QUERY TYPE
            LegacyPing m = getLegacyPing();

            String[] playerList = new String[m.getServer().getOnlinePlayers().length];
            for (int i = 0; i < m.getServer().getOnlinePlayers().length; i++) {
                playerList[i] = m.getServer().getOnlinePlayers()[i].getName();
            }

            // Build the response.
            StringBuilder resp = new StringBuilder();
            resp.append("SERVERPORT " + m.getServerPort() + "\n");
            resp.append("PLAYERCOUNT " + m.getServer().getOnlinePlayers().length + "\n");
            resp.append("MAXPLAYERS " + m.getMaxPlayers() + "\n");
            resp.append("PLAYERLIST " + Arrays.toString(playerList) + "\n");

            // Send the response.
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(resp.toString());
        } else if (request.equalsIgnoreCase("QUERY_JSON")) {
            // Handle a request, respond in JSON format. - DEPRECATED QUERY TYPE
            LegacyPing m = getLegacyPing();

            // Build the JSON response.
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("serverPort", m.getServerPort());
            jsonObject.put("playerCount", m.getServer().getOnlinePlayers().length);
            jsonObject.put("maxPlayers", m.getMaxPlayers());
            JSONArray playerList = new JSONArray();
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                playerList.add(p.getName());
            }
            jsonObject.put("playerList", playerList);

            // Send the JSON response.
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(jsonObject.toJSONString());
        } else {
            //Json Advanced Query Type
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("serverPort", Bukkit.getServer().getPort());
            jsonObject.put("playerCount", Bukkit.getServer().getOnlinePlayers().length);
            jsonObject.put("version", getLegacyPing().getDescription().getVersion());
            //Player List
            if (config.getConfigBoolean("show-players")) {
                JSONArray playerList = new JSONArray();
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    JSONObject playerObject = new JSONObject();
                    playerObject.put("name", p.getName());
                    playerObject.put("displayName", p.getDisplayName());
                    //Locations if enabled
                    if (config.getConfigBoolean("player.coordinates")) {
                        playerObject.put("world", p.getLocation().getWorld());
                        playerObject.put("x", p.getLocation().getX());
                        playerObject.put("y", p.getLocation().getY());
                        playerObject.put("z", p.getLocation().getZ());

                    }
                    playerObject.put("isAlive", !p.isDead());
                    playerObject.put("isInVehicle", p.isInsideVehicle());
                    playerObject.put("isOp", p.isOp());
                    playerList.add(playerObject);
                }
                jsonObject.put("players", playerList);
            }
            //World List
            if (config.getConfigBoolean("show-worlds")) {
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
            //Plugin Info
            if (config.getConfigBoolean("show-plugins")) {
                JSONArray pluginList = new JSONArray();
                for (Plugin p : Bukkit.getServer().getPluginManager().getPlugins()) {
                    JSONObject pluginObject = new JSONObject();
                    pluginObject.put("name", p.getDescription().getName());
                    if (config.getConfigBoolean("show-plugins-versions")) {
                        pluginObject.put("version", p.getDescription().getVersion());
                    }
                    pluginList.add(pluginObject);
                }
                jsonObject.put("plugins", pluginList);
            }


            // Send the JSON response.
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(jsonObject.toJSONString());
        }


        // Different requests may be introduced in the future.
    }

    /**
     * Gets the <code>Minequery</code> parent plugin object.
     *
     * @return The Minequery object
     */
    public LegacyPing getLegacyPing() {
        return legacyPing;
    }
}
