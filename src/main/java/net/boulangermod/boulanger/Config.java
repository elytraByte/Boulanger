package net.boulangermod.boulanger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = Boulanger.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------------
    // Debug -> Pneumatic section
    // ---------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_ENABLED;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_OVERLAY;
    private static final ModConfigSpec.IntValue DEBUG_PNEUMATIC_OVERLAY_MAX_DISTANCE;

    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_TOPOLOGY;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_ENQUEUE;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_ASSIGNMENTS;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_DUCT_SIDE_CHANGES;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_NETWORK_TICK;
    private static final ModConfigSpec.BooleanValue DEBUG_PNEUMATIC_LOG_ENDPOINT_PV;

    private static final ModConfigSpec.IntValue DEBUG_PNEUMATIC_LOG_EVERY_TICKS;

    static {
        BUILDER.push("debug");
        BUILDER.push("pneumatic");

        DEBUG_PNEUMATIC_ENABLED = BUILDER
                .comment("Master toggle for all pneumatic debugging (logs + overlay).")
                .define("enabled", false);

        DEBUG_PNEUMATIC_OVERLAY = BUILDER
                .comment("Shows a small in-game overlay when looking at ducts/endpoints (client-side).")
                .define("overlay", false);

        DEBUG_PNEUMATIC_OVERLAY_MAX_DISTANCE = BUILDER
                .comment("Max distance (blocks) to show overlay when looking at a duct/endpoint.")
                .defineInRange("overlayMaxDistance", 8, 1, 64);

        DEBUG_PNEUMATIC_LOG_TOPOLOGY = BUILDER
                .comment("Logs topology rebuilds / network formation events. (Recommended first toggle.)")
                .define("logTopology", true);

        DEBUG_PNEUMATIC_LOG_ENQUEUE = BUILDER
                .comment("Logs each enqueueTopologyChange call (spammy). Disable to only see rebuild summaries.")
                .define("logEnqueue", false);

        DEBUG_PNEUMATIC_LOG_ASSIGNMENTS = BUILDER
                .comment("Logs per-duct network assignment details (very spammy).")
                .define("logAssignments", false);

        DEBUG_PNEUMATIC_LOG_DUCT_SIDE_CHANGES = BUILDER
                .comment("Logs duct side mode changes (blocked/open, etc.).")
                .define("logDuctSideChanges", true);

        DEBUG_PNEUMATIC_LOG_NETWORK_TICK = BUILDER
                .comment("Logs network tick pressure/volume changes (rate-limited by logEveryTicks).")
                .define("logNetworkTick", false);

        DEBUG_PNEUMATIC_LOG_ENDPOINT_PV = BUILDER
                .comment("Logs per-endpoint PV contributions/add-remove details (extremely spammy).")
                .define("logEndpointPv", false);

        DEBUG_PNEUMATIC_LOG_EVERY_TICKS = BUILDER
                .comment("Rate limit for certain logs (e.g., network tick logging). 20 = once per second.")
                .defineInRange("logEveryTicks", 20, 1, 20 * 60);

        BUILDER.pop(); // pneumatic
        BUILDER.pop(); // debug
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    // Pneumatic debug cached toggles
    public static boolean debugPneumaticEnabled;
    public static boolean debugPneumaticOverlay;
    public static int debugPneumaticOverlayMaxDistance;

    public static boolean debugPneumaticLogTopology;
    public static boolean debugPneumaticLogEnqueue;
    public static boolean debugPneumaticLogAssignments;
    public static boolean debugPneumaticLogDuctSideChanges;
    public static boolean debugPneumaticLogNetworkTick;
    public static boolean debugPneumaticLogEndpointPv;

    public static int debugPneumaticLogEveryTicks;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // Cache pneumatic debug toggles
        debugPneumaticEnabled = DEBUG_PNEUMATIC_ENABLED.get();
        debugPneumaticOverlay = DEBUG_PNEUMATIC_OVERLAY.get();
        debugPneumaticOverlayMaxDistance = DEBUG_PNEUMATIC_OVERLAY_MAX_DISTANCE.get();

        debugPneumaticLogTopology = DEBUG_PNEUMATIC_LOG_TOPOLOGY.get();
        debugPneumaticLogEnqueue = DEBUG_PNEUMATIC_LOG_ENQUEUE.get();
        debugPneumaticLogAssignments = DEBUG_PNEUMATIC_LOG_ASSIGNMENTS.get();
        debugPneumaticLogDuctSideChanges = DEBUG_PNEUMATIC_LOG_DUCT_SIDE_CHANGES.get();
        debugPneumaticLogNetworkTick = DEBUG_PNEUMATIC_LOG_NETWORK_TICK.get();
        debugPneumaticLogEndpointPv = DEBUG_PNEUMATIC_LOG_ENDPOINT_PV.get();

        debugPneumaticLogEveryTicks = DEBUG_PNEUMATIC_LOG_EVERY_TICKS.get();
    }
}
