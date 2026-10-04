package com.aetherwastes.block;

import com.aetherwastes.craft.Engraving;
import com.aetherwastes.item.GlyphItem;
import com.aetherwastes.item.StaffItem;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.magic.SpellData;
import com.aetherwastes.registry.ModComponents;
import com.aetherwastes.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
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
 * Скрижаль Глифики.
 * ПКМ глифом — положить; ПКМ жезлом — записать заклинание; Shift + ПКМ предметом
 * снаряжения — выгравировать глифы на предмет; ПКМ пустой рукой — узор; Shift + ПКМ пустой — забрать глиф.
 */
public class SlateBlock extends BaseEntityBlock {
    public static final MapCodec<SlateBlock> CODEC = simpleCodec(SlateBlock::new);

    public SlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SlateBlockEntity slate)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof GlyphItem glyphItem) {
            if (!level.isClientSide) {
                Glyph glyph = glyphItem.glyph();
                String reason = slate.rejectReason(glyph);
                if (reason != null) {
                    player.displayClientMessage(Component.translatable(reason).withStyle(ChatFormatting.RED), true);
                } else {
                    slate.add(glyph);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 1.4f);
                    player.displayClientMessage(describe(slate), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof StaffItem) {
            if (!level.isClientSide) {
                SpellData spell = slate.compose();
                if (spell == null) {
                    player.displayClientMessage(Component.translatable("message.aetherwastes.slate.incomplete")
                            .withStyle(ChatFormatting.RED), true);
                } else {
                    stack.set(ModComponents.SPELL.get(), spell);
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1f);
                    ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1,
                            pos.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.5);
                    player.displayClientMessage(Component.translatable("message.aetherwastes.slate.inscribed",
                            spell.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player.isShiftKeyDown() && stack.isDamageableItem()) {
            if (!level.isClientSide) Engraving.engrave((ServerPlayer) player, slate, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SlateBlockEntity slate)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                Glyph g = slate.removeLast();
                if (g != null) {
                    ItemStack out = new ItemStack(ModItems.glyph(g));
                    if (!player.addItem(out)) player.drop(out, false);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1f, 0.8f);
                }
            }
            player.displayClientMessage(describe(slate), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SlateBlockEntity slate) {
            for (Glyph g : slate.glyphs()) Block.popResource(level, pos, new ItemStack(ModItems.glyph(g)));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static Component describe(SlateBlockEntity slate) {
        if (slate.glyphs().isEmpty()) {
            return Component.translatable("message.aetherwastes.slate.empty").withStyle(ChatFormatting.GRAY);
        }
        MutableComponent line = Component.translatable("message.aetherwastes.slate.pattern").withStyle(ChatFormatting.GRAY);
        for (int i = 0; i < slate.glyphs().size(); i++) {
            Glyph g = slate.glyphs().get(i);
            line.append(Component.literal(i == 0 ? " " : " + "));
            line.append(Component.translatable(g.translationKey()).withStyle(g.type().color));
        }
        return line;
    }
}
