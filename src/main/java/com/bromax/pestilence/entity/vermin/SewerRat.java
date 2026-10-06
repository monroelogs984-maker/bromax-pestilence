package com.bromax.pestilence.entity.vermin;

import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.phase.Phase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** A big, slick rat from under the ground: twice the bite, more than twice the hide. Caves only. */
public class SewerRat extends PlagueRat {
    public SewerRat(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PlagueRat.createAttributes().add(Attributes.MAX_HEALTH, 14).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    @Override
    public int minPhase() {
        return Phase.RAT_SEASON;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6f;
    }
}
