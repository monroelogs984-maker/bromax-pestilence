package com.bromax.pestilence.item;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.entity.MiasmaCloud;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A swinging brass censer of smouldering herbs. Use it to light it (it burns a dried herb from
 * your inventory, and keeps taking more while lit). While lit and held in either hand, its smoke
 * protects you and anyone within RADIUS blocks from coughs and miasma, and clears miasma clouds.
 */
public class CenserItem extends Item {
    public static final int RADIUS = 4;
    private static final int BURN_TICKS = 2400;

    public CenserItem(Properties properties) {
        super(properties);
    }

    private static int burn(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("burn");
    }

    private static void setBurn(ItemStack stack, int ticks) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("burn", Math.max(0, ticks));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof CenserItem && burn(stack) > 0;
    }

    public static boolean isSwinging(Player player) {
        return isLit(player.getMainHandItem()) || isLit(player.getOffhandItem()) || isLit(belt(player));
    }

    /** A censer hung from the belt (Curios), lit or not. */
    public static ItemStack belt(Player player) {
        return com.bromax.pestilence.compat.Compat.worn(player, s -> s.getItem() instanceof CenserItem);
    }

    /** Burns on from the belt as it would in hand (called from the player tick: Curios slots get no inventoryTick). */
    public static void tickBelt(ServerLevel level, Player player) {
        ItemStack censer = belt(player);
        if (isLit(censer)) smoulder(level, player, censer);
    }

    /** Burn one herb from the inventory; false if there is none. */
    private static boolean refuel(Player player, ItemStack censer) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModRegistry.CENSER_FUEL)) {
                if (!player.getAbilities().instabuild) s.shrink(1);
                setBurn(censer, BURN_TICKS);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isLit(stack)) {
                setBurn(stack, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
            } else if (refuel(player, stack)) {
                level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1f, 1f);
            } else {
                player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".censer_empty").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(holder instanceof Player player) || !isLit(stack)) return;
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (held) smoulder(server, player, stack);
    }

    private static void smoulder(ServerLevel server, Player player, ItemStack stack) {
        if (player.tickCount % 20 != 0) return;
        int left = burn(stack) - 20;
        if (left <= 0 && !refuel(player, stack)) setBurn(stack, 0);
        else if (left > 0) setBurn(stack, left);
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(0.5), player.getZ(), 1, 0.2, 0.1, 0.2, 0.005);
        server.sendParticles(ParticleTypes.WHITE_ASH, player.getX(), player.getY(0.5), player.getZ(), 6, RADIUS * 0.5, 0.5, RADIUS * 0.5, 0);
        for (MiasmaCloud cloud : server.getEntitiesOfClass(MiasmaCloud.class, new AABB(player.blockPosition()).inflate(RADIUS + 1))) cloud.discard();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isLit(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (isLit(stack)) tooltip.add(Component.translatable(getDescriptionId() + ".lit", burn(stack) / 20).withStyle(ChatFormatting.GOLD));
    }
}
