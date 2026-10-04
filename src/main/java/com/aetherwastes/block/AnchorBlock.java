package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.AnchorData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Якорь: сердце базы. Стабилизирует давление (радиус 16/32/48/72/96 по уровням), восстанавливает Ясность,
 * питает устройства. Уровень N+1 стоит N Ядер Якоря и требует нужной эпохи.
 * Shift + ПКМ пустой рукой — сделать точкой возрождения.
 */
public class AnchorBlock extends Block {
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 5);

    public AnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIER, 1));
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server) AnchorData.get(server).put(pos, state.getValue(TIER));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this)) AnchorData.get(server).remove(pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.ANCHOR_CORE.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        int tier = state.getValue(TIER);
        PlayerData d = Data.get(player);
        if (tier >= 5) {
            player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.max").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (tier + 1 > EraManager.maxAnchorTier(d)) {
            player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.need_era").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.SUCCESS;
        }
        int need = tier;
        if (stack.getCount() < need && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.need_cores", need).withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(TIER, tier + 1), Block.UPDATE_ALL);
        if (!player.getAbilities().instabuild) stack.shrink(need);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.2f);
        ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.05);
        player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.upgraded",
                tier + 1, EtherField.anchorRadius(tier + 1)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            sp.setRespawnPosition(level.dimension(), pos.above(), sp.getYRot(), true, false);
            player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.spawn").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }
        int tier = state.getValue(TIER);
        EtherField.Anchor a = new EtherField.Anchor(pos, tier, 1f);
        player.displayClientMessage(Component.translatable("message.aetherwastes.anchor.info",
                tier, EtherField.anchorRadius(tier), Math.round(EtherField.natural(server, pos)),
                Devices.load(server, a), Devices.capacity(server, a)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5 + (random.nextDouble() - 0.5),
                    pos.getY() + 1.0 + random.nextDouble() * 0.5, pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0, 0.05, 0);
        }
    }
}
