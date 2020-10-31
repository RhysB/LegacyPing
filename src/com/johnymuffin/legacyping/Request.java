package com.johnymuffin.legacyping;

import com.johnymuffin.legacyping.simplejson.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
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
    private final LegacyPingImplimentation legacyPing;

    /**
     * The socket we are using to obtain a request.
     */
    private final Socket socket;

    /**
     * The logging utility.
     */
    private final Logger log = Logger.getLogger("Minecraft");


    public Request(LegacyPingImplimentation legacyPing, Socket socket) {
        this.legacyPing = legacyPing;
        this.socket = socket;
    }

    /**
     * Listens for a request.
     */
    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(legacyPing.jsonResponse().toJSONString());


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
//        if (request == null) {
//            return;
//        }

//        if (request.equalsIgnoreCase("QUERY")) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(legacyPing.jsonResponse().toJSONString());

//        }
    }


    /**
     * Gets the <code>Minequery</code> parent plugin object.
     *
     * @return The Minequery object
     */
    public LegacyPingImplimentation getLegacyPing() {
        return legacyPing;
    }
}
