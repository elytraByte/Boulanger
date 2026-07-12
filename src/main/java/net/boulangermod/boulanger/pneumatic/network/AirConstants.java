package net.boulangermod.boulanger.pneumatic.network;

public final class AirConstants {
    private AirConstants() {}

    public static final double ATM_KPA = 101.325;

    /** Duct internal volume contribution (liters) per duct block. Tune later. */
    public static final double DUCT_VOLUME_L = 0.75;

    /** Hard clamp so pressure can’t go negative. */
    public static final double MIN_PRESSURE_KPA = 0.0;

    /** Optional clamp for sanity while prototyping; you can raise/remove later. */
    public static final double MAX_PRESSURE_KPA = 1200.0;

    // fraction per tick toward atmosphere when vented
    public static final double VENT_RATE = 0.25;

    // sealed leak rates (fraction per tick of *overpressure* that bleeds off)
    public static final double LEAK_PER_DUCT = 0.00015;
    public static final double LEAK_PER_ENDPOINT = 0.00050;

}