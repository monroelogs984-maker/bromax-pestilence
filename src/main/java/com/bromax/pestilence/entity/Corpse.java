package com.bromax.pestilence.entity;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.block.PyreBlock;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import com.bromax.pestilence.item.ShroudedCorpseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The body of an animal that died of the plague. Flies gather over it; after a while it twitches,
 * groans, and rises as the Afflicted form of what it was. Fire stops that: flint and steel, a fire
 * charge, lava, a flaming arrow, or a lit pyre nearby. An empty hand wraps it up to carry.
 */
public class Corpse extends Entity {
    private static final EntityDataAccessor<String> SOURCE = SynchedEntityData.defineId(Corpse.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> BURNING = SynchedEntityData.defineId(Corpse.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STIRRING = SynchedEntityData.defineId(Corpse.class, EntityDataSerializers.BOOLEAN);
    /** The last stretch before rising, when the corpse twitches and groans. */
    public static final int STIR_TICKS = 400;

    private int age;
    private int burnTicks;
    private boolean flies;

    public Corpse(EntityType<? extends Corpse> type, Level level) {
        super(type, level);
    }

    public static void spawnFrom(ServerLevel level, LivingEntity dead) {
        place(level, dead.getType(), 0, dead.getX(), dead.getY(), dead.getZ(), dead.getYRot());
        // Crows come to the dead.
        if (level.random.nextFloat() < 0.4f && level.canSeeSky(dead.blockPosition())) {
            for (int i = 0; i < 1 + level.random.nextInt(2); i++) {
                var crow = ModRegistry.CARRION_CROW.get().create(level);
                if (crow == null) continue;
                crow.moveTo(dead.getX() + level.random.nextGaussian() * 3, dead.getY() + 8, dead.getZ() + level.random.nextGaussian() * 3, 0f, 0f);
                level.addFreshEntity(crow);
            }
        }
    }

    public static void place(ServerLevel level, EntityType<?> source, int age, double x, double y, double z, float yaw) {
        Corpse corpse = ModRegistry.CORPSE.get().create(level);
        if (corpse == null) return;
        corpse.setSource(source);
        corpse.age = age;
        corpse.moveTo(x, y, z, yaw, 0f);
        level.addFreshEntity(corpse);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SOURCE, "minecraft:cow");
        builder.define(BURNING, false);
        builder.define(STIRRING, false);
    }

    public void setSource(EntityType<?> type) {
        entityData.set(SOURCE, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    @Nullable
    public EntityType<?> getSource() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(SOURCE));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    /** A Gravedigger's work: bring the rising closer. */
    public void hasten(int ticks) {
        age += ticks;
    }

    public boolean isBurning() {
        return entityData.get(BURNING);
    }

    public boolean isStirring() {
        return entityData.get(STIRRING);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.6, 0.98, 0.6));
        if (onGround()) setDeltaMovement(getDeltaMovement().multiply(0.5, -0.5, 0.5));

        if (level().isClientSide) {
            clientEffects();
            return;
        }
        if (isBurning()) {
            if (++burnTicks >= PestConfig.CORPSE_BURN_TICKS.get()) consume();
            return;
        }
        if (isOnFire() || isInLava() || tickCount % 20 == 0 && nearLitPyre()) {
            ignite();
            return;
        }
        int rise = PestConfig.CORPSE_RISE_TICKS.get();
        age++;
        if (!flies && age > 1200 && level() instanceof ServerLevel level) {
            // Left lying a minute, a body draws a fly swarm.
            flies = true;
            var swarm = ModRegistry.FLY_SWARM.get().create(level);
            if (swarm != null) {
                swarm.moveTo(getX(), getY() + 0.5, getZ());
                level.addFreshEntity(swarm);
            }
        }
        if (!isStirring() && age >= rise - STIR_TICKS) {
            entityData.set(STIRRING, true);
            level().playSound(null, blockPosition(), SoundEvents.HUSK_AMBIENT, SoundSource.HOSTILE, 1f, 0.6f);
        }
        if (age >= rise) rise();
    }

    private void clientEffects() {
        if (isBurning()) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.6), getY() + random.nextDouble() * 0.6, getRandomZ(0.6), 0, 0.03, 0);
            }
            level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.5), getY() + 0.5, getRandomZ(0.5), 0, 0.05, 0);
        } else if (tickCount % 4 == 0) {
            level().addParticle(ModRegistry.FLY.get(), getRandomX(0.8), getY() + 0.3 + random.nextDouble() * 0.6, getRandomZ(0.8), 0, 0, 0);
        }
    }

    private boolean nearLitPyre() {
        int r = PestConfig.PYRE_RADIUS.get();
        BlockPos pos = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
            BlockState state = level().getBlockState(p);
            if (state.is(ModRegistry.PYRE.get()) && state.getValue(PyreBlock.LIT)) return true;
        }
        return false;
    }

    public void ignite() {
        if (isBurning()) return;
        entityData.set(BURNING, true);
        level().playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 0.8f);
    }

    /** Quicklime: gone at once, as if burned. */
    public void dissolve() {
        consume();
    }

    /** Burned: gone for good, and the Pestilence loses ground (and whoever is nearest earns a deed). */
    private void consume() {
        if (level() instanceof ServerLevel lvl) {
            net.minecraft.world.entity.player.Player near = lvl.getNearestPlayer(this, 16);
            com.bromax.pestilence.disease.Consecration.deed(near, com.bromax.pestilence.disease.Consecration.BURN);
        }
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 15, 0.4, 0.3, 0.4, 0.02);
            level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1f);
            spawnAtLocation(new ItemStack(Items.BONE_MEAL, 1 + random.nextInt(2)));
            Plague.addPoints(this, PestConfig.POINTS_CORPSE_BURN.get());
        }
        discard();
    }

    private void rise() {
        EntityType<? extends AfflictedMob> form = AfflictedMob.formOf(getSource());
        if (form != null && level() instanceof ServerLevel level) {
            AfflictedMob mob = form.create(level);
            if (mob != null) {
                mob.moveTo(getX(), getY(), getZ(), getYRot(), 0f);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
                level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.2f, 0.7f);
                Plague.addPoints(mob, PestConfig.POINTS_CORPSE_RISE.get());
                com.bromax.pestilence.world.NestSeeding.corpseRose(level, blockPosition());
            }
        }
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (isBurning()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(ModRegistry.PYRE_TORCH.get())) {
            if (!level().isClientSide) {
                ignite();
                if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                else stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(ModRegistry.QUICKLIME.get())) {
            if (!level().isClientSide) {
                stack.consume(1, player);
                consume();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.isEmpty() && getSource() != null) {
            if (!level().isClientSide) {
                player.setItemInHand(hand, ShroudedCorpseItem.of(getSource(), age));
                level().playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SOURCE, tag.getString("source"));
        age = tag.getInt("age");
        entityData.set(BURNING, tag.getBoolean("burning"));
        entityData.set(STIRRING, tag.getBoolean("stirring"));
        flies = tag.getBoolean("flies");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("source", entityData.get(SOURCE));
        tag.putInt("age", age);
        tag.putBoolean("burning", isBurning());
        tag.putBoolean("stirring", isStirring());
        tag.putBoolean("flies", flies);
    }
}
