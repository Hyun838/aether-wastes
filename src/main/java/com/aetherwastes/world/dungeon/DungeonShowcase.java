package com.aetherwastes.world.dungeon;

import com.aetherwastes.block.GuardianSealBlockEntity;
import com.aetherwastes.entity.boss.DungeonBoss;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Постройка подземелья по команде /aether dungeon (для операторов: показ, проверка, скриншоты)
 * и камера-обзор его залов.
 */
public final class DungeonShowcase {
    private static final Map<DungeonKind, List<DungeonPiece>> BUILT = new EnumMap<>(DungeonKind.class);

    private DungeonShowcase() {}

    /** Построить все части в мире, чанк за чанком (как это делает генератор мира). */
    public static void place(ServerLevel level, List<DungeonPiece> pieces) {
        BoundingBox all = BoundingBox.encapsulatingBoxes(pieces.stream().map(StructurePiece::getBoundingBox).toList()).orElseThrow();
        for (int cx = (all.minX() - 30) >> 4; cx <= (all.maxX() + 30) >> 4; cx++) {
            for (int cz = (all.minZ() - 30) >> 4; cz <= (all.maxZ() + 30) >> 4; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                BoundingBox box = new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                        cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ());
                for (DungeonPiece piece : pieces) {
                    BoundingBox pb = piece.getBoundingBox().inflatedBy(8);
                    if (!pb.intersects(box)) continue;
                    piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, cp, BlockPos.ZERO);
                }
            }
        }
    }

    public static List<DungeonPiece> build(ServerLevel level, DungeonKind kind, BlockPos near) {
        List<DungeonPiece> existing = BUILT.get(kind);
        if (existing != null) return existing;
        int ox = near.getX() + 120 + kind.ordinal() * 200;
        int oz = near.getZ() - 30;
        int cx = ox + kind.width * DungeonStructure.CELL / 2, cz = oz + kind.depth * DungeonStructure.CELL / 2;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
        int y0 = kind.surface ? surface : Math.max(level.getMinBuildHeight() + 12, Math.min(surface - 32, 20));
        List<DungeonPiece> pieces = DungeonStructure.layout(kind, RandomSource.create(near.asLong() ^ kind.ordinal()), ox, y0, oz,
                (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        place(level, pieces);
        BUILT.put(kind, pieces);
        return pieces;
    }

    /** Поставить игрока-камеру в нужную точку: boss | room | outside. */
    public static boolean view(ServerPlayer p, DungeonKind kind, String view) {
        ServerLevel level = p.serverLevel();
        List<DungeonPiece> pieces = build(level, kind, level.getSharedSpawnPos());
        Vec3 cam, look;
        switch (view) {
            case "boss" -> {
                DungeonPiece boss = find(pieces, DungeonPiece.Role.BOSS);
                BoundingBox b = boss.getBoundingBox();
                int fy = b.minY();
                // Между колоннами у северной стены, взгляд на помост с Печатью
                cam = new Vec3(b.minX() + 8.5, fy + 6.5, b.minZ() + 1.6);
                look = new Vec3(b.minX() + 12.5, fy + 3.5, b.minZ() + 12.5);
                BlockPos seal = new BlockPos(b.minX() + 12, fy + 2, b.minZ() + 12);
                if (level.getBlockState(seal).is(ModBlocks.GUARDIAN_SEAL.get())
                        && level.getBlockState(seal).getValue(com.aetherwastes.block.GuardianSealBlock.ACTIVE)) {
                    DungeonBoss e = GuardianSealBlockEntity.awaken(level, seal, level.getBlockState(seal));
                    if (e != null) {
                        e.setNoAi(true);
                        double fx = cam.x - (seal.getX() + 0.5), fz = cam.z - (seal.getZ() + 0.5);
                        float face = (float) Math.toDegrees(Math.atan2(-fx, fz));
                        e.moveTo(seal.getX() + 0.5, seal.getY() + 1, seal.getZ() + 0.5, face, 0f);
                        e.setYHeadRot(face);
                        e.setYBodyRot(face);
                    }
                }
            }
            case "room" -> {
                DungeonPiece room = find(pieces, DungeonPiece.Role.THEMED);
                if (room == null) room = find(pieces, DungeonPiece.Role.HALL);
                if (room == null) room = find(pieces, DungeonPiece.Role.ANTECHAMBER);
                BoundingBox b = room.getBoundingBox();
                int fy = room.role() == DungeonPiece.Role.PIT ? b.minY() + 6 : b.minY();
                cam = new Vec3(b.minX() + 2.5, fy + 4.2, b.minZ() + 2.5);
                look = new Vec3(b.minX() + 8.5, fy + 1.5, b.minZ() + 8.5);
            }
            default -> {
                BoundingBox all = BoundingBox.encapsulatingBoxes(pieces.stream().map(StructurePiece::getBoundingBox).toList()).orElseThrow();
                // Камера над землёй: сначала прогрузить чанк, иначе высота рельефа неизвестна
                int camX = all.minX() - 18, camZ = all.minZ() - 18;
                level.getChunk(camX >> 4, camZ >> 4);
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, camX, camZ);
                int topAt = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (all.minX() + all.maxX()) / 2, (all.minZ() + all.maxZ()) / 2);
                cam = new Vec3(camX, Math.max(Math.max(ground, topAt) + 14, all.maxY() + 10), camZ);
                look = new Vec3((all.minX() + all.maxX()) / 2.0, all.minY() + 4, (all.minZ() + all.maxZ()) / 2.0);
            }
        }
        Vec3 d = look.subtract(cam);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
        p.teleportTo(level, cam.x, cam.y, cam.z, yaw, pitch);
        p.getAbilities().flying = p.getAbilities().mayfly;
        p.onUpdateAbilities();
        return true;
    }

    private static DungeonPiece find(List<DungeonPiece> pieces, DungeonPiece.Role role) {
        for (DungeonPiece p : pieces) if (p.role() == role) return p;
        return null;
    }
}
