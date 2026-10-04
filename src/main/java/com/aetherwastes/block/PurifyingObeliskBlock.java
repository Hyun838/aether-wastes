package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.threats.Scar;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Очищающий обелиск: раз в 2 секунды выжигает Порчу в радиусе 16 и не даёт ей расти рядом. */
public class PurifyingObeliskBlock extends BaseEntityBlock {
    public static final MapCodec<PurifyingObeliskBlock> CODEC = simpleCodec(PurifyingObeliskBlock::new);

    public PurifyingObeliskBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this)) Devices.placed(level, pos, Devices.OBELISK);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this)) Devices.removed(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PURIFYING_OBELISK.get(), Entity::serverTick);
    }

    public static class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PURIFYING_OBELISK.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Entity be) {
            if (level.getGameTime() % 40 != 0 || !(level instanceof ServerLevel server)) return;
            if (!Devices.powered(server, pos)) return;
            Scar.cleanse(server, pos, 16);
        }
    }
}
