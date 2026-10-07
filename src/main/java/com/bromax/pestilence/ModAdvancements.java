package com.bromax.pestilence;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Advancements that no vanilla trigger can see (data/bromaxpestilence/advancement/pestilence/*.json
 * with a single "code" criterion of type minecraft:impossible), granted from where they happen.
 */
public final class ModAdvancements {
    public static final String ASHES = "ashes_to_ashes";
    public static final String CONSECRATED = "consecrated";
    public static final String SAVED_VILLAGE = "saved_village";
    public static final String STAYED = "stayed_the_tide";
    public static final String BLACKBLIGHT = "blackblight";
    public static final String LAST_BREATH = "last_breath";

    private ModAdvancements() {}

    public static void grant(Entity entity, String name) {
        if (!(entity instanceof ServerPlayer player)) return;
        AdvancementHolder advancement = player.server.getAdvancements().get(Pestilence.id("pestilence/" + name));
        if (advancement != null) player.getAdvancements().award(advancement, "code");
    }
}
