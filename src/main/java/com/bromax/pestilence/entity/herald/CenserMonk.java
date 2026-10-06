package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Swings a censer of foul smoke: leaves miasma behind it, and from the Outbreak on blights the ground it walks. */
public class CenserMonk extends HeraldMob {
    public CenserMonk(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(22, 0.24, 3);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 160 != 0 || !(level() instanceof ServerLevel level) || Tracker.inBrazier(level, blockPosition())) return;
        MiasmaCloud.spawn(level, getX(), getY(), getZ(), 2.5f, 140);
        if (PestilenceData.phase(level) >= Phase.OUTBREAK) Blight.spread(level, blockPosition(), 3, 2);
    }
}
