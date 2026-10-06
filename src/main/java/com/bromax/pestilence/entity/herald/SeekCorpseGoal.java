package com.bromax.pestilence.entity.herald;

import com.bromax.pestilence.entity.Corpse;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Consumer;

/** Walk (or fly) to the nearest unburning corpse within range and do something to it once there. */
public class SeekCorpseGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private final int range;
    private final Consumer<Corpse> atCorpse;
    private Corpse corpse;
    private int cooldown;

    public SeekCorpseGoal(PathfinderMob mob, double speed, int range, Consumer<Corpse> atCorpse) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.atCorpse = atCorpse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null || --cooldown > 0) return false;
        cooldown = 40;
        corpse = mob.level().getEntitiesOfClass(Corpse.class, mob.getBoundingBox().inflate(range), c -> !c.isBurning()).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return corpse != null;
    }

    @Override
    public boolean canContinueToUse() {
        return corpse != null && corpse.isAlive() && !corpse.isBurning() && mob.getTarget() == null;
    }

    @Override
    public void tick() {
        if (mob.distanceToSqr(corpse) > 4) {
            if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(corpse, speed);
        } else {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(corpse);
            atCorpse.accept(corpse);
        }
    }

    @Override
    public void stop() {
        corpse = null;
    }
}
