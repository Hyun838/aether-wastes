package com.aetherwastes.block;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.threats.Scar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Эфирный Шрам: медленно расползается, ранит живых, усиливает монстров. После «Исцеления» отступает. */
public class AetherScarBlock extends Block {
    public AetherScarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (WorldState.ENDING_HEAL.equals(WorldState.get(level.getServer()).ending)) {
            if (random.nextFloat() < 0.3f) level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            return;
        }
        if (Scar.protectedAt(level, pos)) {
            if (random.nextFloat() < 0.2f) level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
            return;
        }
        double spread = AetherConfig.SCAR_SPREAD.get();
        if (spread <= 0 || random.nextFloat() > 0.25f * spread) return;
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
        if (Scar.convertible(level.getBlockState(target)) && !Scar.protectedAt(level, target)) {
            level.setBlockAndUpdate(target, defaultBlockState());
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !(entity instanceof Enemy)
                && living.tickCount % 40 == 0) {
            living.hurt(level.damageSources().magic(), 0.5f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
