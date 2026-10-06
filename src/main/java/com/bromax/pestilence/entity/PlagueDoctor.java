package com.bromax.pestilence.entity;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * A wandering trader in a beaked mask. Sells the cure, the mask and the pyre, pays for rat tails,
 * never catches the plague, and treats sick villagers and animals around him for free.
 */
public class PlagueDoctor extends WanderingTrader implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PlagueDoctor(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /** The wandering trader's goals, minus its invisibility and milk potions. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, AbstractIllager.class, 12f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, PlagueRat.class, 6f, 0.5, 0.5));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, AfflictedMob.class, 10f, 0.5, 0.5));
        goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3f, 1f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8f));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(ModRegistry.FOUR_THIEVES_VINEGAR.get()), 8, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), Optional.of(new ItemCost(Items.LEATHER, 3)),
                new ItemStack(ModRegistry.PLAGUE_MASK.get()), 2, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack(ModRegistry.PYRE_ITEM.get()), 6, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(ModRegistry.QUICKLIME.get(), 4), 12, 1, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(ModRegistry.RAT_TAIL.get(), 4), new ItemStack(Items.EMERALD), 16, 1, 0.05f));
        DataComponentType<?> bookComponent = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("patchouli", "book"));
        if (bookComponent != null) {
            ItemStack book = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book")));
            if (!book.isEmpty()) {
                book.set((DataComponentType<ResourceLocation>) bookComponent, Pestilence.id("treatise"));
                offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), book, 4, 1, 0.05f));
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(ModRegistry.PLAGUE) && super.canBeAffected(effect);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && tickCount % 100 == 0) {
            for (LivingEntity sick : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8),
                    e -> e != this && !(e instanceof Player) && e.hasEffect(ModRegistry.PLAGUE))) {
                Plague.cure(sick);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sick.getX(), sick.getEyeY(), sick.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
