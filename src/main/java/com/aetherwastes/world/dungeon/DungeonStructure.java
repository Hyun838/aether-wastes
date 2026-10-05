package com.aetherwastes.world.dungeon;

import com.aetherwastes.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Процедурное подземелье: сетка комнат 13×13, связанных случайным остовным деревом с петлями,
 * зал босса 2×2 клетки в дальнем конце, винтовая лестница к поверхности (для подземных видов).
 */
public class DungeonStructure extends Structure {
    public static final MapCodec<DungeonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            DungeonKind.CODEC.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(i, DungeonStructure::new));

    public static final int CELL = 13;
    public static final int ROOM_H = 9;
    public static final int BOSS_H = 15;

    private final DungeonKind kind;

    public DungeonStructure(StructureSettings settings, DungeonKind kind) {
        super(settings);
        this.kind = kind;
    }

    public DungeonKind kind() {
        return kind;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DUNGEON.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        WorldgenRandom rnd = ctx.random();
        int w = kind.width, d = kind.depth;
        int ox = cp.getMiddleBlockX() - (w * CELL) / 2;
        int oz = cp.getMiddleBlockZ() - (d * CELL) / 2;
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();

        int y0;
        if (kind.surface) {
            int[] hs = {
                    height(ctx, ox, oz), height(ctx, ox + w * CELL, oz), height(ctx, ox, oz + d * CELL),
                    height(ctx, ox + w * CELL, oz + d * CELL), height(ctx, cx, cz)};
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int h : hs) {
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
            if (min <= ctx.chunkGenerator().getSeaLevel() + 1) return Optional.empty();
            if (max - min > 14) return Optional.empty();
            y0 = hs[4];
        } else {
            int surface = height(ctx, cx, cz);
            int minY = ctx.heightAccessor().getMinBuildHeight() + 12;
            int top = Math.min(surface - 26, 30);
            int low = Math.max(minY, top - 30);
            if (top < minY) return Optional.empty();
            y0 = Mth.randomBetweenInclusive(rnd, low, top);
        }
        int fy0 = y0;
        return Optional.of(new GenerationStub(new BlockPos(cx, y0, cz),
                builder -> layout(kind, rnd, ox, fy0, oz, (x, z) -> height(ctx, x, z)).forEach(builder::addPiece)));
    }

    private static int height(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
    }

    // ---------------- Планировка ----------------
    private static final int[] DX = {0, 1, 0, -1};   // 0 — север (-Z), 1 — восток, 2 — юг, 3 — запад
    private static final int[] DZ = {-1, 0, 1, 0};

    /** Планировка подземелья (также используется тестами). surface(x, z) — высота поверхности. */
    public static List<DungeonPiece> layout(DungeonKind kind, net.minecraft.util.RandomSource rnd, int ox, int y0, int oz,
                                            java.util.function.IntBinaryOperator surface) {
        List<DungeonPiece> out = new ArrayList<>();
        int w = kind.width, d = kind.depth;
        int[][] doors = new int[w][d];
        boolean[][] boss = new boolean[w][d];
        int bi = w - 2;
        int bj = rnd.nextInt(d - 1);
        boss[bi][bj] = boss[bi + 1][bj] = boss[bi][bj + 1] = boss[bi + 1][bj + 1] = true;
        int ei = 0;
        int ej = rnd.nextInt(d);

        // Остовное дерево (DFS со случайным порядком).
        boolean[][] seen = new boolean[w][d];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{ei, ej});
        seen[ei][ej] = true;
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
            java.util.Collections.shuffle(dirs, new java.util.Random(rnd.nextLong()));
            boolean moved = false;
            for (int dir : dirs) {
                int ni = c[0] + DX[dir], nj = c[1] + DZ[dir];
                if (ni < 0 || nj < 0 || ni >= w || nj >= d || seen[ni][nj] || boss[ni][nj]) continue;
                seen[ni][nj] = true;
                doors[c[0]][c[1]] |= 1 << dir;
                doors[ni][nj] |= 1 << ((dir + 2) % 4);
                stack.push(new int[]{ni, nj});
                moved = true;
                break;
            }
            if (!moved) stack.pop();
        }
        // Несколько петель, чтобы подземелье не было простым лабиринтом.
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < d; j++) {
                if (boss[i][j]) continue;
                for (int dir = 1; dir <= 2; dir++) {
                    int ni = i + DX[dir], nj = j + DZ[dir];
                    if (ni >= w || nj >= d || boss[ni][nj]) continue;
                    if ((doors[i][j] & (1 << dir)) == 0 && rnd.nextFloat() < 0.18f) {
                        doors[i][j] |= 1 << dir;
                        doors[ni][nj] |= 1 << ((dir + 2) % 4);
                    }
                }
            }
        }
        // Вход в зал босса — с запада, из клетки (bi-1, bj или bj+1).
        int side = rnd.nextInt(2);
        int ai = bi - 1, aj = bj + side;
        doors[ai][aj] |= 1 << 1;
        int bossDoors = 1 << (3 * 2 + side);   // запад, половина side

        // Расстояния от входа.
        int[][] dist = new int[w][d];
        for (int[] row : dist) java.util.Arrays.fill(row, -1);
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ei, ej});
        dist[ei][ej] = 0;
        int maxDist = 0;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int dir = 0; dir < 4; dir++) {
                if ((doors[c[0]][c[1]] & (1 << dir)) == 0) continue;
                int ni = c[0] + DX[dir], nj = c[1] + DZ[dir];
                if (ni < 0 || nj < 0 || ni >= w || nj >= d || boss[ni][nj] || dist[ni][nj] >= 0) continue;
                dist[ni][nj] = dist[c[0]][c[1]] + 1;
                maxDist = Math.max(maxDist, dist[ni][nj]);
                q.add(new int[]{ni, nj});
            }
        }

        // Роли комнат.
        List<DungeonPiece.Role> pool = new ArrayList<>();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < d; j++) {
                if (boss[i][j]) continue;
                DungeonPiece.Role role;
                int deg = Integer.bitCount(doors[i][j]);
                if (i == ei && j == ej) role = DungeonPiece.Role.ENTRANCE;
                else if (i == ai && j == aj) role = DungeonPiece.Role.ANTECHAMBER;
                else if (deg == 1) role = rnd.nextFloat() < 0.6f ? DungeonPiece.Role.TREASURE : DungeonPiece.Role.SHRINE;
                else role = pickRole(rnd, kind, dist[i][j], maxDist);
                int minY = role == DungeonPiece.Role.PIT ? y0 - 6 : y0;
                DungeonPiece piece = new DungeonPiece(kind, role, doors[i][j], rnd.nextLong(), Math.max(0, dist[i][j]),
                        new net.minecraft.world.level.levelgen.structure.BoundingBox(ox + i * CELL, minY, oz + j * CELL,
                                ox + i * CELL + CELL - 1, y0 + ROOM_H - 1, oz + j * CELL + CELL - 1));
                piece.setExtra(i == 0 || j == 0 || i == w - 1 || j == d - 1 ? 1 : 0);
                out.add(piece);
            }
        }
        // Зал босса.
        out.add(new DungeonPiece(kind, DungeonPiece.Role.BOSS, bossDoors, rnd.nextLong(), maxDist + 1,
                new net.minecraft.world.level.levelgen.structure.BoundingBox(ox + bi * CELL, y0, oz + bj * CELL,
                        ox + bi * CELL + 2 * CELL - 1, y0 + BOSS_H - 1, oz + bj * CELL + 2 * CELL - 1)));

        // Шахта к поверхности над входом.
        if (!kind.surface) {
            int sx = ox + ei * CELL + 3, sz = oz + ej * CELL + 3;
            int top = surface.applyAsInt(sx + 3, sz + 3);
            DungeonPiece shaft = new DungeonPiece(kind, DungeonPiece.Role.SHAFT, 0, rnd.nextLong(), 0,
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(sx, y0 + 1, sz, sx + 6, Math.max(y0 + ROOM_H + 4, top + 6), sz + 6));
            shaft.setExtra(Math.max(top, y0 + ROOM_H));
            out.add(shaft);
        }
        return out;
    }

    private static DungeonPiece.Role pickRole(net.minecraft.util.RandomSource rnd, DungeonKind kind, int dist, int maxDist) {
        float f = rnd.nextFloat();
        if (f < 0.20f) return DungeonPiece.Role.SPAWNER;
        if (f < 0.34f) return DungeonPiece.Role.TRAP;
        if (f < 0.48f) return DungeonPiece.Role.PILLARS;
        if (f < 0.66f) return DungeonPiece.Role.THEMED;
        if (f < 0.76f) return DungeonPiece.Role.PIT;
        if (f < 0.84f) return dist > maxDist / 2 ? DungeonPiece.Role.TREASURE : DungeonPiece.Role.SPAWNER;
        return DungeonPiece.Role.HALL;
    }
}
