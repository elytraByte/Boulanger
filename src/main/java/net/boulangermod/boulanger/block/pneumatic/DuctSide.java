package net.boulangermod.boulanger.block.pneumatic;

import net.minecraft.util.StringRepresentable;

public enum DuctSide implements StringRepresentable {
    CLOSED("closed"),
    OPEN("open"),
    CONNECTED("connected"),
    FLANGED("flanged");

    private final String id;
    DuctSide(String id) { this.id = id; }

    @Override public String getSerializedName() { return id; }
}
