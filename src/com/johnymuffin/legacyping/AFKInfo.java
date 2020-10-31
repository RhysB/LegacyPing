package com.johnymuffin.legacyping;

public interface AFKInfo {

    public boolean hasPlayerMoved(Object rawPlayer, int seconds);

    public int lastPlayerMove(Object rawPlayer);
}
