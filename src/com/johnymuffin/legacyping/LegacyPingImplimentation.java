package com.johnymuffin.legacyping;

import com.johnymuffin.legacyping.simplejson.JSONObject;

import java.util.logging.Level;

public interface LegacyPingImplimentation {

    public void log(Level level, String string);

    public JSONObject jsonResponse();

    public String getClientMod();

}
