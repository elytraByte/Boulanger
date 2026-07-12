package net.boulangermod.boulanger.pneumatic.debug;

import com.mojang.logging.LogUtils;
import net.boulangermod.boulanger.Config;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

/**
 * Small helper for pneumatic debugging.
 *
 * - Reads flags from {@link Config}.
 * - Provides categorized console logging.
 */
public final class PneumaticDebug {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PneumaticDebug() { }

    public static boolean enabled() { return Config.debugPneumaticEnabled; }
    public static boolean overlayEnabled() { return enabled() && Config.debugPneumaticOverlay; }
    public static boolean logTopology() { return enabled() && Config.debugPneumaticLogTopology; }
    public static boolean logAssignments() { return enabled() && Config.debugPneumaticLogAssignments; }
    public static boolean logSideChanges() { return enabled() && Config.debugPneumaticLogDuctSideChanges; }
    public static boolean logNetworkTick() { return enabled() && Config.debugPneumaticLogNetworkTick; }
    public static boolean logEndpointPv() { return enabled() && Config.debugPneumaticLogEndpointPv; }
    public static int logEveryTicks() { return Math.max(1, Config.debugPneumaticLogEveryTicks); }

    public static void topology(String msg, Object... args) {
        if (logTopology()) LOGGER.info("[PNEU/TOPO] " + msg, args);
    }

    public static void assign(String msg, Object... args) {
        if (logAssignments()) LOGGER.info("[PNEU/ASSIGN] " + msg, args);
    }

    public static void side(String msg, Object... args) {
        if (logSideChanges()) LOGGER.info("[PNEU/SIDE] " + msg, args);
    }

    public static void tick(String msg, Object... args) {
        if (logNetworkTick()) LOGGER.info("[PNEU/TICK] " + msg, args);
    }

    public static void endpoint(String msg, Object... args) {
        if (logEndpointPv()) LOGGER.info("[PNEU/ENDPOINT] " + msg, args);
    }

    public static String pos(BlockPos p) {
        if (p == null) return "null";
        return p.getX() + "," + p.getY() + "," + p.getZ();
    }

    public static boolean rateLimit(long gameTime) {
        int every = logEveryTicks();
        return every <= 1 || (gameTime % every) == 0;
    }
}
