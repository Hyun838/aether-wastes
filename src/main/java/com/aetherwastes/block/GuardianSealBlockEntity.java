package com.aetherwastes.block;

import com.aetherwastes.entity.boss.DungeonBoss;
import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GuardianSealBlockEntity extends BlockEntity {
    public GuardianSealBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUARDIAN_SEAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuardianSealBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !state.getValue(GuardianSealBlock.ACTIVE)) return;
        if (level.getGameTime() % 10 != 0) return;
        Player near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 9.0,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
        if (near == null) return;
        awaken(server, pos, state);
    }

    /** Пробудить стража (также используется командой и тестами). */
    public static DungeonBoss awaken(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(GuardianSealBlock.ACTIVE, false), 3);
        EntityType<? extends DungeonBoss> type = switch (state.getValue(GuardianSealBlock.KIND)) {
            case 0 -> ModEntities.ARCHIVE_KEEPER.get();
            case 1 -> ModEntities.ECHO_LORD.get();
            default -> ModEntities.ASH_COLOSSUS.get();
        };
        DungeonBoss boss = type.create(level);
        if (boss == null) return null;
        boss.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        boss.setHome(pos);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.RUNE.get(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 80, 2.5, 1.5, 2.5, 0.05);
        level.playSound(null, pos, ModSounds.SEAL_BREAK.get(), SoundSource.HOSTILE, 2f, 1f);
        level.playSound(null, pos, ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2.5f, 0.8f);
        return boss;
    }
}
