package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Tends the dead with its shovel: every corpse it reaches rises far sooner. Kill it before it gets to them. */
public class Gravedigger extends HeraldMob {
    public Gravedigger(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(26, 0.27, 4);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new SeekCorpseGoal(this, 1.0, 20, corpse -> {
            if (tickCount % 20 != 0 || !(level() instanceof ServerLevel level)) return;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            corpse.hasten(200);
            level.playSound(null, corpse.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    corpse.getX(), corpse.getY() + 0.3, corpse.getZ(), 8, 0.3, 0.1, 0.3, 0.1);
        }));
    }
}
