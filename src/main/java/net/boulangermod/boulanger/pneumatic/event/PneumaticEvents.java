//pneumatic ducting deprecated for the time being
//package net.boulangermod.boulanger.pneumatic.event;
//
//import net.boulangermod.boulanger.pneumatic.network.AirNetworkManager;
//import net.minecraft.server.level.ServerLevel;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.neoforge.event.tick.LevelTickEvent;
//
//public final class PneumaticEvents {
//
//    @SubscribeEvent
//    public static void onLevelTick(LevelTickEvent.Post e) {
//        if (!(e.getLevel() instanceof ServerLevel level)) return;
//        AirNetworkManager.get(level).serverTick(level);
//    }
//}