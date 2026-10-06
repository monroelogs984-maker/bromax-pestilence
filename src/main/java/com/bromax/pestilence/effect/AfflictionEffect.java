package com.bromax.pestilence.effect;

import com.bromax.pestilence.Pestilence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Heralds' lesser afflictions. None of them hurt; all of them are unpleasant and obvious.
 * DREAD (Mourner): you cannot sprint, and walk a little slower.
 * DELIRIUM (Miasma Wraith): you hear things that are not there. Purely client-side.
 * ROTTING (Fly Swarm): raw meat in your inventory spoils, one piece every 15 seconds.
 */
public class AfflictionEffect extends MobEffect {
    public enum Kind { DREAD, DELIRIUM, ROTTING }

    private static final TagKey<Item> RAW_MEAT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_meat"));
    private static final SoundEvent[] PHANTOMS = {SoundEvents.SILVERFISH_AMBIENT, SoundEvents.ZOMBIE_AMBIENT, SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.GRAVEL_STEP, SoundEvents.PANDA_SNEEZE, SoundEvents.BELL_BLOCK, SoundEvents.ALLAY_HURT};

    private final Kind kind;

    public AfflictionEffect(Kind kind, int colour) {
        super(MobEffectCategory.HARMFUL, colour);
        this.kind = kind;
        if (kind == Kind.DREAD) {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, Pestilence.id("dread"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return switch (kind) {
            case DREAD -> true;
            case DELIRIUM -> duration % 50 == 0;
            case ROTTING -> duration % 300 == 0;
        };
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        switch (kind) {
            case DREAD -> {
                if (entity.isSprinting()) entity.setSprinting(false);
            }
            case DELIRIUM -> {
                if (entity.level().isClientSide && entity instanceof Player p && p.isLocalPlayer() && entity.getRandom().nextInt(3) == 0) {
                    double a = entity.getRandom().nextDouble() * Math.PI * 2, d = 3 + entity.getRandom().nextDouble() * 6;
                    entity.level().playLocalSound(entity.getX() + Math.cos(a) * d, entity.getY(), entity.getZ() + Math.sin(a) * d,
                            PHANTOMS[entity.getRandom().nextInt(PHANTOMS.length)], SoundSource.AMBIENT, 0.8f, 0.7f + entity.getRandom().nextFloat() * 0.4f, false);
                }
            }
            case ROTTING -> {
                if (!entity.level().isClientSide && entity instanceof Player player) {
                    for (ItemStack stack : player.getInventory().items) {
                        if (stack.is(RAW_MEAT)) {
                            stack.shrink(1);
                            if (!player.getInventory().add(new ItemStack(Items.ROTTEN_FLESH))) player.drop(new ItemStack(Items.ROTTEN_FLESH), false);
                            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".rotting").withStyle(ChatFormatting.DARK_GREEN), true);
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }
}
