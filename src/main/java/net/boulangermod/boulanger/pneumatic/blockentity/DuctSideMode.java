package net.boulangermod.boulanger.pneumatic.blockentity;

public enum DuctSideMode {
    CONNECTED,
    BLOCKED;

    public boolean allowsConnection() {
        return this == CONNECTED;
    }

    public DuctSideMode next() {
        return this == CONNECTED ? BLOCKED : CONNECTED;
    }
}
