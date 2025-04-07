package net.boulangermod.boulanger.block;

import net.minecraft.util.StringRepresentable;

public enum PorcelainTileVariant implements StringRepresentable {
    BLACK("black"),
    BLUE("blue"),
    DARK_BLUE("dark_blue"),
    DARK_BLUE_WHITE("dark_blue_white"),
    L3E("l3e"),
    WHITE("white");

    private final String name;

    PorcelainTileVariant(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

