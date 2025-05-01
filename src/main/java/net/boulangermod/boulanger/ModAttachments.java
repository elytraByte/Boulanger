// ModAttachments.java
package net.boulangermod.boulanger;

import net.boulangermod.boulanger.energy.BatteryEnergyStorage;
import net.boulangermod.boulanger.energy.ModEnergyStorage;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Boulanger.MODID);

    /**
     * A deferred holder for our battery energy attachment.
     * Note the generic types: <RegistryType, ConcreteType>
     */
// register basic battery
// ModAttachments.java (excerpt)
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<BatteryEnergyStorage>>
            BASIC_BATTERY = ATTACHMENTS.register("battery_tier1", () ->
            AttachmentType.serializable(
                    () -> new BatteryEnergyStorage(BatteryEnergyStorage.Tier.BASIC)
            ).build()
    );

    public static final DeferredHolder<AttachmentType<?>,AttachmentType<BatteryEnergyStorage>>
            ADVANCED_BATTERY = ATTACHMENTS.register("battery_tier2", () ->
            AttachmentType.serializable(
                    () -> new BatteryEnergyStorage(BatteryEnergyStorage.Tier.ADVANCED)
            ).build()
    );



}

