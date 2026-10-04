package com.aetherwastes.threats;

import com.aetherwastes.base.Devices;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Эфирный Шрам (Порча): засевание, распространение и очищение. Якоря и Обелиски его не пускают. */
public final class Scar {
    private Scar() {}

    public static boolean convertible(BlockState st) {
        return st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(BlockTags.BASE_STONE_OVERWORLD)
                || st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || st.is(Blocks.GRAVEL);
    }

    public static boolean protectedAt(ServerLevel level, BlockPos pos) {
        return EtherField.inAnchor(level, pos) || Devices.nearObelisk(level, pos, 16);
    }

    /** Попытаться заразить землю где-то рядом. */
    public static void seedNear(ServerLevel level, BlockPos center, int radius) {
        for (int i = 0; i < 6; i++) {
            int x = center.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - center.getY()) > 12) continue;
            if (convertible(level.getBlockState(pos)) && !protectedAt(level, pos)) {
                level.setBlockAndUpdate(pos, ModBlocks.AETHER_SCAR.get().defaultBlockState());
                level.sendParticles(ParticleTypes.WITCH, x + 0.5, y + 1.1, z + 0.5, 20, 0.4, 0.2, 0.4, 0.02);
                return;
            }
        }
    }

    /** Огонь Горнила, Гниль и Обелиски выжигают Порчу. */
    public static int cleanse(ServerLevel level, BlockPos center, int radius) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.AETHER_SCAR.get())) {
                level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0);
                n++;
            }
        }
        return n;
    }
}
