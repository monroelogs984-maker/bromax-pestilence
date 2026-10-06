package com.bromax.pestilence;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * BROMAX's Pestilence: a plague that grows through nine world phases. Rats, rotten flesh and
 * coughing spread it; plague deaths leave corpses that rise as the Afflicted unless burned; and
 * curing, cleansing and burning push the phase back down. Players are never converted.
 */
@Mod(Pestilence.MODID)
public class Pestilence {
    public static final String MODID = "bromaxpestilence";

    public Pestilence(IEventBus bus, ModContainer container) {
        ModRegistry.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, PestConfig.SPEC);
        NeoForge.EVENT_BUS.register(PlagueEvents.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
