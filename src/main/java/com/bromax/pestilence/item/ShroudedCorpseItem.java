package com.bromax.pestilence.item;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.entity.Corpse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A plague corpse wrapped for carrying to a pyre. It is heavy (Slowness), carrying it unmasked can
 * infect you, and it keeps rotting: once it starts to stir you drop it where you stand.
 */
public class ShroudedCorpseItem extends Item {
    public ShroudedCorpseItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(EntityType<?> source, int age) {
        ItemStack stack = new ItemStack(com.bromax.pestilence.ModRegistry.SHROUDED_CORPSE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("source", BuiltInRegistries.ENTITY_TYPE.getKey(source).toString());
        tag.putInt("age", age);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Nullable
    private static EntityType<?> source(ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(data(stack).getString("source"));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(holder instanceof Player player) || player.tickCount % 20 != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false, true));
        if (player.tickCount % 200 == 0 && !Plague.wearsMask(player)
                && player.getRandom().nextDouble() < PestConfig.CARRY_INFECT_CHANCE.get()) {
            Plague.infect(player);
        }

        CompoundTag tag = data(stack);
        int age = tag.getInt("age") + 20;
        EntityType<?> type = source(stack);
        if (type == null) {
            stack.setCount(0);
            return;
        }
        if (age >= PestConfig.CORPSE_RISE_TICKS.get() - Corpse.STIR_TICKS) {
            Corpse.place(server, type, age, player.getX(), player.getY(), player.getZ(), player.getYRot());
            stack.setCount(0);
            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".corpse_dropped")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        tag.putInt("age", age);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        EntityType<?> type = source(context.getItemInHand());
        if (type == null) return InteractionResult.FAIL;
        if (context.getLevel() instanceof ServerLevel level) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            Corpse.place(level, type, data(context.getItemInHand()).getInt("age"),
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation());
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = source(stack);
        return type == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".of", type.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
