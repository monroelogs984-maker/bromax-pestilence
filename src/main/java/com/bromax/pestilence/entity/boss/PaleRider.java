package com.bromax.pestilence.entity.boss;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Pestilence itself, on a pale horse. Shoots Rot-tipped arrows at those who keep away and rides
 * down those who don't; its bell fills everyone within 32 blocks with Dread; at three-quarters,
 * half and a quarter health it calls up its Heralds; below half, miasma rains around its quarry.
 * Killing it breaks the Pestilence entirely.
 */
public class PaleRider extends PlagueBoss {
    public static final String ROT_ARROW = Pestilence.MODID + ":rot_arrow";
    private static final List<EntityType<? extends Mob>> HOST = List.of(ModRegistry.FLAGELLANT.get(), ModRegistry.PLAGUE_BEARER.get(),
            ModRegistry.CENSER_MONK.get(), ModRegistry.MOURNER.get(), ModRegistry.AFFLICTED_HORSE.get(), ModRegistry.AFFLICTED_WOLF.get());
    private int calls;

    public PaleRider(EntityType<? extends AfflictedMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return base(600, 0.32, 8).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected int phasesBroken() {
        return 99;
    }

    @Override
    protected Disease contactDisease() {
        return Disease.ROT;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 300 == 0) toll(level);
        LivingEntity target = getTarget();
        if (target == null) return;
        float hp = getHealth() / getMaxHealth();
        if (calls == 0 && hp < 0.75f || calls == 1 && hp < 0.5f || calls == 2 && hp < 0.25f) {
            calls++;
            callTheHost(level, target);
        }
        double dist = distanceToSqr(target);
        if (tickCount % 40 == 0 && dist > 64 && hasLineOfSight(target)) shoot(level, target);
        if (tickCount % 200 == 100 && onGround() && dist > 16) {
            Vec3 charge = target.position().subtract(position()).normalize().scale(1.4);
            setDeltaMovement(charge.x, 0.2, charge.z);
            playSound(SoundEvents.HORSE_ANGRY, 2f, 0.6f);
        }
        if (hp < 0.5f && tickCount % 200 == 0) {
            for (int i = 0; i < 3; i++) {
                MiasmaCloud.spawn(level, target.getX() + random.nextGaussian() * 6, target.getY(), target.getZ() + random.nextGaussian() * 6, 2.5f, 200);
            }
        }
    }

    private void toll(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 4f, 0.4f);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), p -> !p.isCreative() && !p.isSpectator())) {
            p.addEffect(new MobEffectInstance(ModRegistry.DREAD, 200, 0));
        }
    }

    private void shoot(ServerLevel level, LivingEntity target) {
        Arrow arrow = new Arrow(level, this, new ItemStack(Items.ARROW), null);
        arrow.getPersistentData().putBoolean(ROT_ARROW, true);
        arrow.setBaseDamage(5);
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = target.getY(0.33) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * 0.2;
        arrow.shoot(dx, dy, dz, 2.0f, 4f);
        level.addFreshEntity(arrow);
        playSound(SoundEvents.SKELETON_SHOOT, 1.5f, 0.6f);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void callTheHost(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4f, 0.8f);
        for (int i = 0; i < 2 + calls; i++) {
            Mob mob = HOST.get(random.nextInt(HOST.size())).create(level);
            if (mob == null) continue;
            mob.moveTo(getX() + random.nextGaussian() * 4, getY(), getZ() + random.nextGaussian() * 4, random.nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(target);
            level.addFreshEntity(mob);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("calls", calls);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        calls = tag.getInt("calls");
    }

    @Override protected SoundEvent ambient() { return SoundEvents.HORSE_BREATHE; }
    @Override protected SoundEvent hurt() { return SoundEvents.SKELETON_HORSE_HURT; }
    @Override protected SoundEvent death() { return SoundEvents.SKELETON_HORSE_DEATH; }
    @Override protected SoundEvent step() { return SoundEvents.HORSE_GALLOP; }
}
