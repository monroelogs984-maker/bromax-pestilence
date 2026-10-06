package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.apothecary.ApothecaryInput;
import com.bromax.pestilence.apothecary.ApothecaryRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Slot 0 is the base (bottle, bowl or paper), slots 1-4 the ingredients. */
public class ApothecaryTableBlockEntity extends ItemHolderBlockEntity {
    public ApothecaryTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.APOTHECARY_TABLE_BE.get(), pos, state, 5);
    }

    public static boolean isBase(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BOWL) || stack.is(Items.PAPER);
    }

    public boolean place(ItemStack stack) {
        if (isBase(stack) && items.get(0).isEmpty()) {
            items.set(0, stack);
        } else {
            int free = -1;
            for (int i = 1; i < items.size(); i++) if (items.get(i).isEmpty()) { free = i; break; }
            if (free < 0) return false;
            items.set(free, stack);
        }
        if (level != null) level.playSound(null, worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8f, 1.2f);
        changed();
        return true;
    }

    public ItemStack takeLast() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack s = items.get(i);
                items.set(i, ItemStack.EMPTY);
                changed();
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public void grind(Player player, ItemStack mortar, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return;
        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 1; i < items.size(); i++) if (!items.get(i).isEmpty()) ingredients.add(items.get(i));
        ApothecaryInput input = new ApothecaryInput(items.get(0), ingredients);
        Optional<RecipeHolder<ApothecaryRecipe>> match = server.getRecipeManager().getRecipeFor(ModRegistry.APOTHECARY_TYPE.get(), input, server);
        String key = "message." + Pestilence.MODID + ".apothecary.";
        if (match.isEmpty()) {
            player.displayClientMessage(Component.translatable(key + "nothing").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ApothecaryRecipe recipe = match.get().value();
        if (recipe.heated() && !heated()) {
            player.displayClientMessage(Component.translatable(key + "needs_heat").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        changed();
        Block.popResource(server, worldPosition.above(), recipe.assemble(input, server.registryAccess()));
        mortar.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
        if (recipe.heated()) server.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1f);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0);
    }

    /** A fire, lit campfire, lava, magma or lit Herb Brazier within two blocks. */
    private boolean heated() {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-2, -1, -2), worldPosition.offset(2, 1, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(s) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)
                    || s.is(ModRegistry.HERB_BRAZIER.get()) && s.getValue(HerbBrazierBlock.LIT)) return true;
        }
        return false;
    }
}
