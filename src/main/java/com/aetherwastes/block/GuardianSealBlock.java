package com.aetherwastes.block;

import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.registry.ModParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Печать Стража в зале босса: когда игрок подходит ближе 9 блоков, пробуждает хозяина подземелья. */
public class GuardianSealBlock extends BaseEntityBlock {
    public static final MapCodec<GuardianSealBlock> CODEC = simpleCodec(GuardianSealBlock::new);
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public GuardianSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, 0).setValue(ACTIVE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(KIND, ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuardianSealBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GUARDIAN_SEAL.get(), GuardianSealBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 1.2 + random.nextDouble() * 1.5;
            level.addParticle(ModParticles.RUNE.get(), pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 0.3,
                    pos.getZ() + 0.5 + Math.sin(a) * r, 0, 0.03, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.ETHER_SPARK.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.1, 0.08, (random.nextDouble() - 0.5) * 0.1);
        }
    }
}
