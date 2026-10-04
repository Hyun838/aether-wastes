package com.aetherwastes.world;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Переходы между Верхним миром и Изнанкой (1 блок Изнанки = 4 блока поверхности). */
public final class Underside {
    private Underside() {}

    public static void travel(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel from = p.serverLevel();
        PlayerData d = Data.get(p);
        if (d.era < 3 && !p.isCreative()) {
            Msg.bar(p, ChatFormatting.RED, "underside.aetherwastes.need_era");
            return;
        }
        ServerLevel to;
        int x, y, z;
        if (Dims.isUnderside(from)) {
            to = server.overworld();
            x = p.getBlockX() * 4;
            z = p.getBlockZ() * 4;
            to.getChunk(x >> 4, z >> 4);
            y = to.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        } else {
            to = server.getLevel(Dims.UNDERSIDE);
            if (to == null) {
                Msg.bar(p, ChatFormatting.RED, "underside.aetherwastes.missing");
                return;
            }
            x = p.getBlockX() / 4;
            z = p.getBlockZ() / 4;
            y = findGround(to, x, z);
        }
        BlockPos arrival = new BlockPos(x, y, z);
        ensurePortal(to, arrival);
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1, 0.5, 0.1);
        p.teleportTo(to, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
        to.playSound(null, arrival, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.2f);
        if (Dims.isUnderside(to)) {
            if (!d.visitedUnderside) {
                d.visitedUnderside = true;
                Msg.chat(p, ChatFormatting.DARK_PURPLE, "underside.aetherwastes.first");
            }
            Insights.add(p, "land", "underside");
            EraManager.check(p);
            if (d.era >= 5) HeartManager.ensureBuilt(server);
        }
        Sync.all(p);
    }

    /** Найти твёрдую землю в Изнанке или построить островок. */
    public static int findGround(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        for (int y = 200; y > 8; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                return y;
            }
        }
        int y = 100;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                level.setBlockAndUpdate(new BlockPos(x + dx, y - 1, z + dz), ModBlocks.ASH_BLOCK.get().defaultBlockState());
        return y;
    }

    private static void ensurePortal(ServerLevel level, BlockPos arrival) {
        for (BlockPos pos : BlockPos.betweenClosed(arrival.offset(-6, -3, -6), arrival.offset(6, 3, 6))) {
            if (level.getBlockState(pos).is(ModBlocks.UNDERSIDE_PORTAL.get())) return;
        }
        BlockPos spot = arrival.east(2);
        BlockState below = level.getBlockState(spot.below());
        if (!below.isSolid()) level.setBlockAndUpdate(spot.below(), Blocks.OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(spot, ModBlocks.UNDERSIDE_PORTAL.get().defaultBlockState());
    }
}
