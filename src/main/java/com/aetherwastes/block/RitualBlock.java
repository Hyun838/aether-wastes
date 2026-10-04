package com.aetherwastes.block;

import com.aetherwastes.craft.Rituals;
import com.aetherwastes.item.StaffItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Фокус и Постамент ритуального круга. Постаменты стоят в 2 блоках от Фокуса по сторонам света.
 * ПКМ предметом — положить, ПКМ пустой рукой — забрать, ПКМ жезлом по Фокусу — начать ритуал.
 */
public class RitualBlock extends BaseEntityBlock {
    public static final MapCodec<RitualBlock> FOCUS_CODEC = simpleCodec(p -> new RitualBlock(p, true));
    public static final MapCodec<RitualBlock> PEDESTAL_CODEC = simpleCodec(p -> new RitualBlock(p, false));
    private final boolean focus;

    public RitualBlock(Properties properties, boolean focus) {
        super(properties);
        this.focus = focus;
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return focus ? FOCUS_CODEC : PEDESTAL_CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return focus ? HolderBlockEntity.focus(pos, state) : HolderBlockEntity.pedestal(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HolderBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (focus && stack.getItem() instanceof StaffItem) {
            Rituals.perform((ServerPlayer) player, pos);
            return ItemInteractionResult.SUCCESS;
        }
        if (!be.item().isEmpty()) {
            player.displayClientMessage(Component.translatable("ritual.aetherwastes.holds", be.item().getHoverName())
                    .withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.SUCCESS;
        }
        be.setItem(stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
        player.displayClientMessage(Component.translatable("ritual.aetherwastes.placed", be.item().getHoverName())
                .withStyle(ChatFormatting.GRAY), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HolderBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (be.item().isEmpty()) {
            player.displayClientMessage(Component.translatable(focus ? "ritual.aetherwastes.focus_hint" : "ritual.aetherwastes.pedestal_hint")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        ItemStack out = be.item();
        be.setItem(ItemStack.EMPTY);
        if (!player.addItem(out)) player.drop(out, false);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HolderBlockEntity be && !be.item().isEmpty()) {
            Block.popResource(level, pos, be.item());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
