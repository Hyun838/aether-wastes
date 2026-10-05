package com.aetherwastes.world.dungeon;

import com.aetherwastes.block.DungeonTrapBlock;
import com.aetherwastes.block.GuardianSealBlock;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.Supplier;

/**
 * Одна комната подземелья (или зал босса, или шахта к поверхности).
 * Всё строится детерминированно из seed, поэтому кусок, попавший в несколько чанков, собирается одинаково.
 */
public class DungeonPiece extends StructurePiece {
    public enum Role {ENTRANCE, ANTECHAMBER, TREASURE, SHRINE, SPAWNER, TRAP, PILLARS, THEMED, PIT, HALL, BOSS, SHAFT}

    private final DungeonKind kind;
    private final Role role;
    private final int doors;
    private final long seed;
    private final int depth;
    private int extra;
    private int floorY;

    // Текущий вызов postProcess
    private WorldGenLevel level;
    private BoundingBox clip;
    private Palette pal;

    public DungeonPiece(DungeonKind kind, Role role, int doors, long seed, int depth, BoundingBox box) {
        super(ModStructures.DUNGEON_ROOM.get(), depth, box);
        this.kind = kind;
        this.role = role;
        this.doors = doors;
        this.seed = seed;
        this.depth = depth;
        this.floorY = role == Role.PIT ? box.minY() + 6 : role == Role.SHAFT ? box.minY() - 1 : box.minY();
        setOrientation(null);
    }

    public DungeonPiece(CompoundTag tag) {
        super(ModStructures.DUNGEON_ROOM.get(), tag);
        this.kind = DungeonKind.byId(tag.getInt("kind"));
        this.role = Role.values()[Math.floorMod(tag.getInt("role"), Role.values().length)];
        this.doors = tag.getInt("doors");
        this.seed = tag.getLong("seed");
        this.depth = tag.getInt("depth");
        this.extra = tag.getInt("extra");
        this.floorY = tag.getInt("fy");
        setOrientation(null);
    }

    public void setExtra(int extra) {
        this.extra = extra;
    }

    public Role role() {
        return role;
    }

    public DungeonKind kind() {
        return kind;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("kind", kind.ordinal());
        tag.putInt("role", role.ordinal());
        tag.putInt("doors", doors);
        tag.putLong("seed", seed);
        tag.putInt("depth", depth);
        tag.putInt("extra", extra);
        tag.putInt("fy", floorY);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.level = level;
        this.clip = chunkBox;
        this.pal = Palette.of(kind);
        try {
            switch (role) {
                case BOSS -> buildBoss();
                case SHAFT -> buildShaft();
                default -> buildRoom();
            }
        } finally {
            this.level = null;
            this.clip = null;
        }
    }

    // =====================================================================
    // Помощники
    // =====================================================================
    private int x0() {
        return boundingBox.minX();
    }

    private int z0() {
        return boundingBox.minZ();
    }

    /** Детерминированный шум 0..1 по координатам. */
    private float h(int x, int y, int z, int salt) {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L) ^ (salt * 0x27D4EB2F165667C5L);
        v ^= v >>> 31;
        v *= 0x7fb5d329728ea185L;
        v ^= v >>> 27;
        v *= 0x81dadef4bc2dd44dL;
        v ^= v >>> 33;
        return (v >>> 40) / (float) (1L << 24);
    }

    private BlockPos abs(int lx, int ly, int lz) {
        return new BlockPos(x0() + lx, floorY + ly, z0() + lz);
    }

    private boolean inside(BlockPos p) {
        return clip.isInside(p);
    }

    private void set(int lx, int ly, int lz, BlockState s) {
        BlockPos p = abs(lx, ly, lz);
        if (!inside(p)) return;
        level.setBlock(p, s, 2);
        if (!s.getFluidState().isEmpty()) level.scheduleTick(p, s.getFluidState().getType(), 0);
    }

    private void set(int lx, int ly, int lz, Block b) {
        set(lx, ly, lz, b.defaultBlockState());
    }

    private void fill(int ax, int ay, int az, int bx, int by, int bz, BlockState s) {
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++)
            for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++)
                for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) set(x, y, z, s);
    }

    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

    private void chest(int lx, int ly, int lz, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos p = abs(lx, ly, lz);
        if (!inside(p)) return;
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(p) instanceof ChestBlockEntity be) {
            be.setLootTable(loot, seed ^ p.asLong());
        }
    }

    private void spawner(int lx, int ly, int lz, EntityType<?> type) {
        BlockPos p = abs(lx, ly, lz);
        if (!inside(p)) return;
        level.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(p) instanceof SpawnerBlockEntity be) {
            be.setEntityId(type, RandomSource.create(seed ^ p.asLong()));
        }
    }

    private void arrowTrap(int lx, int ly, int lz, Direction facing) {
        BlockPos p = abs(lx, ly, lz);
        if (!inside(p)) return;
        level.setBlock(p, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), 2);
        if (level.getBlockEntity(p) instanceof DispenserBlockEntity be) {
            be.setItem(0, new ItemStack(Items.ARROW, 16));
            be.setItem(4, new ItemStack(kind == DungeonKind.CITADEL ? Items.FIRE_CHARGE : Items.ARROW, 8));
        }
    }

    private BlockState trap() {
        return ModBlocks.DUNGEON_TRAP.get().defaultBlockState().setValue(DungeonTrapBlock.KIND, kind.ordinal());
    }

    private EntityType<?> mob(int salt) {
        Supplier<? extends EntityType<?>>[] pool = pal.mobs;
        return pool[(int) (h(salt, depth, 7, 99) * pool.length) % pool.length].get();
    }

    private ResourceKey<LootTable> loot(String type) {
        return kind.loot(type);
    }

    private static BlockState facing(Block b, Direction d) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) s = s.setValue(HorizontalDirectionalBlock.FACING, d);
        return s;
    }

    private static BlockState stairs(Block b, Direction d, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, d)
                .setValue(StairBlock.HALF, top ? net.minecraft.world.level.block.state.properties.Half.TOP
                        : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
    }

    private static BlockState slab(Block b, boolean top) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState pillar(Block b) {
        BlockState s = b.defaultBlockState();
        return s.hasProperty(RotatedPillarBlock.AXIS) ? s.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : s;
    }

    private static BlockState candles(int n, boolean lit) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, n))).setValue(CandleBlock.LIT, lit);
    }

    private static BlockState hanging(Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState skull(int rot) {
        return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, rot & 15);
    }

    private boolean hasDoor(int dir) {
        return (doors & (1 << dir)) != 0;
    }

    // =====================================================================
    // Обычная комната 13×13
    // =====================================================================
    private static final int S = DungeonStructure.CELL;      // 13
    private static final int M = S - 1;                      // 12
    private static final int H = DungeonStructure.ROOM_H;   // 9
    private static final int C = H - 1;                      // 8 — потолок

    private void buildRoom() {
        // Под полом комнаты-ямы — сплошная кладка.
        if (role == Role.PIT) {
            for (int x = 0; x <= M; x++)
                for (int z = 0; z <= M; z++)
                    for (int y = -6; y < 0; y++) set(x, y, z, pal.wall(h(x, y, z, 1)));
        }
        // Оболочка
        for (int x = 0; x <= M; x++) {
            for (int z = 0; z <= M; z++) {
                boolean edge = x == 0 || z == 0 || x == M || z == M;
                for (int y = 0; y <= C; y++) {
                    BlockState s;
                    if (y == 0) s = floor(x, z, S);
                    else if (y == C) s = pal.ceiling(h(x, y, z, 2));
                    else if (edge) s = y == 1 ? pal.wallBase : y == C - 1 ? pal.wallTop : pal.wall(h(x, y, z, 3));
                    else s = AIR;
                    set(x, y, z, s);
                }
            }
        }
        // Колонны по внутренним углам
        for (int[] c : new int[][]{{1, 1}, {1, M - 1}, {M - 1, 1}, {M - 1, M - 1}}) {
            for (int y = 1; y < C; y++) set(c[0], y, c[1], pal.pillar);
        }
        // Двери
        for (int dir = 0; dir < 4; dir++) if (hasDoor(dir)) door(dir, 0, S);
        // Светильники на стенах без дверей
        wallLights(S);
        // Фундамент и зубцы для цитадели
        if (kind == DungeonKind.CITADEL) citadelExterior(S);

        switch (role) {
            case ENTRANCE -> entrance();
            case ANTECHAMBER -> antechamber();
            case TREASURE -> treasure();
            case SHRINE -> shrine();
            case SPAWNER -> spawnerRoom();
            case TRAP -> trapRoom();
            case PILLARS -> pillarsRoom();
            case THEMED -> themed();
            case PIT -> pit();
            default -> hall();
        }
        scatter(S, C);
    }

    private BlockState floor(int x, int z, int size) {
        int m = size - 1;
        if (x == 0 || z == 0 || x == m || z == m) return pal.wallBase;
        if (x == 1 || z == 1 || x == m - 1 || z == m - 1) return pal.floorBorder;
        float n = h(x, 0, z, 4);
        if (n < 0.06f) return pal.floorWorn;
        return ((x + z) & 1) == 0 ? pal.floorA : pal.floorB;
    }

    /** Проём двери шириной 3 в стене dir; half — половина стены зала босса (0/1). */
    private void door(int dir, int half, int size) {
        int m = size - 1;
        int off = half * S;
        for (int k = -1; k <= 1; k++) {
            int t = off + 6 + k;
            for (int y = 1; y <= 4; y++) {
                int[] p = wallPos(dir, t, m);
                set(p[0], y, p[1], AIR);
            }
            int[] p = wallPos(dir, t, m);
            set(p[0], 5, p[1], pal.accent);
            set(p[0], 0, p[1], pal.floorBorder);
        }
        for (int side = -1; side <= 1; side += 2) {
            int[] p = wallPos(dir, off + 6 + side * 2, m);
            for (int y = 1; y <= 5; y++) set(p[0], y, p[1], pal.pillar);
        }
    }

    private static int[] wallPos(int dir, int t, int m) {
        return switch (dir) {
            case 0 -> new int[]{t, 0};
            case 1 -> new int[]{m, t};
            case 2 -> new int[]{t, m};
            default -> new int[]{0, t};
        };
    }

    private void wallLights(int size) {
        int m = size - 1;
        for (int dir = 0; dir < 4; dir++) {
            for (int t = 3; t < m; t += 3) {
                if (t >= 4 && t <= 8 && size == S && hasDoor(dir)) continue;
                if (size != S && isBossDoorZone(dir, t)) continue;
                if (h(dir, t, 0, 5) < 0.45f) continue;
                int[] p = wallPos(dir, t, m);
                set(p[0], 4, p[1], pal.wallLight);
            }
        }
    }

    private boolean isBossDoorZone(int dir, int t) {
        for (int half = 0; half < 2; half++) {
            if ((doors & (1 << (dir * 2 + half))) != 0 && t >= half * S + 4 && t <= half * S + 8) return true;
        }
        return false;
    }

    /** Мелкий мусор, паутина, свечи, кости — после основной планировки. */
    private void scatter(int size, int ceil) {
        int m = size - 1;
        for (int x = 2; x < m - 1; x++) {
            for (int z = 2; z < m - 1; z++) {
                BlockPos p = abs(x, 1, z);
                if (!inside(p) || !level.getBlockState(p).isAir()) continue;
                if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
                if (nearDoorPath(x, z, size)) continue;
                float n = h(x, 1, z, 6);
                if (n < 0.025f) set(x, 1, z, pal.junk1);
                else if (n < 0.04f) set(x, 1, z, pal.junk2);
                // паутина в верхних углах
                BlockPos up = abs(x, ceil - 1, z);
                if (inside(up) && level.getBlockState(up).isAir() && h(x, ceil - 1, z, 7) < pal.cobweb
                        && (x <= 2 || z <= 2 || x >= m - 2 || z >= m - 2)) set(x, ceil - 1, z, Blocks.COBWEB);
            }
        }
    }

    private boolean nearDoorPath(int x, int z, int size) {
        if (size != S) return false;
        boolean cx = x >= 5 && x <= 7, cz = z >= 5 && z <= 7;
        return (cx && (hasDoor(0) && z <= 6 || hasDoor(2) && z >= 6)) || (cz && (hasDoor(3) && x <= 6 || hasDoor(1) && x >= 6));
    }

    private void citadelExterior(int size) {
        int m = size - 1;
        // Фундамент вниз до земли
        for (int x = 0; x <= m; x++) {
            for (int z = 0; z <= m; z++) {
                for (int y = -1; y > -26; y--) {
                    BlockPos p = abs(x, y, z);
                    if (!inside(p)) break;
                    BlockState cur = level.getBlockState(p);
                    if (!cur.isAir() && cur.getFluidState().isEmpty() && !cur.canBeReplaced()) break;
                    level.setBlock(p, (x == 0 || z == 0 || x == m || z == m) ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState(), 2);
                }
            }
        }
        // Внешние стены — зубцы на крыше
        if (extra >= 1) {
            for (int x = 0; x <= m; x++) {
                for (int z = 0; z <= m; z++) {
                    boolean edge = x == 0 || z == 0 || x == m || z == m;
                    if (!edge) continue;
                    int roof = size == S ? C : DungeonStructure.BOSS_H - 1;
                    if (((x + z) & 1) == 0) set(x, roof + 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                    else set(x, roof + 1, z, slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, false));
                }
            }
        }
    }

    // ---------------- Роли ----------------
    private void entrance() {
        boolean under = !kind.surface;
        // Сундук с припасами и надписи на стенах
        chest(2, 1, 6, Direction.EAST, loot("entrance"));
        set(2, 1, 5, pal.junk2);
        if (under) {
            // Проём в потолке под шахту (сама шахта — отдельная часть)
            fill(4, C, 4, 8, C, 8, AIR);
        } else {
            // Ворота цитадели наружу (на западе) и решётка
            for (int k = -1; k <= 1; k++) {
                for (int y = 1; y <= 4; y++) set(0, y, 6 + k, AIR);
                set(0, 5, 6 + k, Blocks.CHAIN.defaultBlockState());
                set(0, 4, 6 + k, Blocks.IRON_BARS.defaultBlockState());
            }
            for (int y = 1; y <= 6; y++) {
                set(0, y, 4, pal.pillar);
                set(0, y, 8, pal.pillar);
            }
            set(0, 7, 5, pal.accent);
            set(0, 7, 6, pal.wallLight);
            set(0, 7, 7, pal.accent);
            // Ступени к земле перед воротами — вниз по фундаменту
            for (int k = -1; k <= 1; k++) {
                for (int s = 1; s <= 6; s++) {
                    BlockPos p = abs(-s, 1 - s, 6 + k);
                    if (!inside(p)) continue;
                    if (!level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) break;
                    level.setBlock(p, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, false), 2);
                    for (int y = -1; y > -6 - s; y--) {
                        BlockPos q = p.offset(0, y + 1 - 1, 0);
                        if (!inside(q) || (!level.getBlockState(q).isAir() && level.getFluidState(q).isEmpty())) break;
                        level.setBlock(q, Blocks.BLACKSTONE.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Знамя-указатель: ряд света к выходу
        set(6, 0, 6, pal.floorBorder);
    }

    private void antechamber() {
        // Ковровая дорожка к двери босса (восток)
        for (int x = 1; x <= M - 1; x++) set(x, 1, 6, pal.carpet);
        for (int z = 1; z <= M - 1; z++) if (hasDoor(0) && z <= 6 || hasDoor(2) && z >= 6) set(6, 1, z, pal.carpet);
        // Колонны с огнями вдоль дорожки
        for (int x : new int[]{3, 9}) {
            for (int z : new int[]{4, 8}) {
                for (int y = 1; y <= 6; y++) set(x, y, z, pal.pillar);
                set(x, 7, z, pal.accent);
                set(x, 5, z + (z < 6 ? 1 : -1), pal.wallLight);
            }
        }
        // Припасы перед боем
        chest(10, 1, 2, Direction.SOUTH, loot("supplies"));
        set(11, 1, 2, candles(3, true));
        set(9, 1, 2, pal.junk2);
        // Черепа-предупреждение у двери босса
        set(11, 1, 4, skull(4));
        set(11, 1, 8, skull(4));
    }

    private void treasure() {
        // Задняя стена — напротив единственной двери
        int dir = 0;
        for (int d = 0; d < 4; d++) if (hasDoor(d)) dir = d;
        int back = (dir + 2) % 4;
        Direction face = switch (back) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
        for (int t : new int[]{4, 6, 8}) {
            int[] p = wallPos(back, t, M);
            int lx = p[0] + (back == 1 ? -1 : back == 3 ? 1 : 0);
            int lz = p[1] + (back == 0 ? 1 : back == 2 ? -1 : 0);
            if (t == 6) {
                chest(lx, 1, lz, face, loot("treasure"));
                set(lx, 2, lz, Blocks.AIR.defaultBlockState());
            } else if (h(t, 1, back, 8) < 0.6f) {
                chest(lx, 1, lz, face, loot("common"));
            } else {
                set(lx, 1, lz, pal.junk2);
            }
        }
        // Золотые блики и ковёр
        fill(4, 1, 4, 8, 1, 8, Blocks.AIR.defaultBlockState());
        for (int x = 4; x <= 8; x++) for (int z = 4; z <= 8; z++) set(x, 1, z, pal.carpet);
        // Ловушки перед сокровищем
        for (int k = 0; k < 3; k++) {
            int x = 3 + (int) (h(k, 2, 0, 9) * 7), z = 3 + (int) (h(k, 3, 0, 9) * 7);
            set(x, 1, z, trap());
        }
        if (h(0, 0, 0, 10) < 0.45f) spawner(6, 1, 6, mob(1));
    }

    private void shrine() {
        // Алтарь
        fill(5, 1, 5, 7, 1, 7, pal.accent);
        set(6, 2, 6, pal.wallLight);
        for (int[] c : new int[][]{{4, 4}, {4, 8}, {8, 4}, {8, 8}}) {
            set(c[0], 1, c[1], pal.pillar);
            set(c[0], 2, c[1], candles(1 + (int) (h(c[0], c[1], 0, 11) * 4), true));
        }
        int dir = 0;
        for (int d = 0; d < 4; d++) if (hasDoor(d)) dir = d;
        int back = (dir + 2) % 4;
        int[] p = wallPos(back, 6, M);
        int lx = p[0] + (back == 1 ? -1 : back == 3 ? 1 : 0);
        int lz = p[1] + (back == 0 ? 1 : back == 2 ? -1 : 0);
        Direction face = switch (back) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
        chest(lx, 1, lz, face, loot("shrine"));
        switch (kind) {
            case ARCHIVE -> {
                set(6, 2, 6, Blocks.ENCHANTING_TABLE);
                for (int x = 3; x <= 9; x++) for (int z : new int[]{3, 9}) if (x != 6) set(x, 1, z, Blocks.BOOKSHELF);
            }
            case CRYPT -> set(6, 2, 6, Blocks.SOUL_CAMPFIRE);
            case CITADEL -> set(6, 2, 6, Blocks.ANVIL);
        }
    }

    private void spawnerRoom() {
        fill(5, 1, 5, 7, 1, 7, pal.accent);
        spawner(6, 2, 6, mob(2));
        for (int[] c : new int[][]{{5, 5}, {5, 7}, {7, 5}, {7, 7}}) set(c[0], 2, c[1], pal.junk2);
        if (h(1, 1, 1, 12) < 0.5f) chest(2, 1, 10, Direction.EAST, loot("common"));
        if (depth >= 4 && h(2, 2, 2, 12) < 0.4f) {
            fill(9, 1, 2, 9, 1, 2, pal.accent);
            spawner(9, 2, 2, mob(3));
        }
    }

    private void trapRoom() {
        // Сетка рун-ловушек
        for (int x = 2; x <= M - 2; x++) {
            for (int z = 2; z <= M - 2; z++) {
                if (h(x, 1, z, 13) < 0.16f) set(x, 1, z, trap());
            }
        }
        // Стреляющие стены: нажимные плиты у стен
        for (int dir = 0; dir < 4; dir++) {
            if (hasDoor(dir)) continue;
            for (int t : new int[]{3, 9}) {
                int[] p = wallPos(dir, t, M);
                Direction out = switch (dir) {
                    case 0 -> Direction.SOUTH;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.NORTH;
                    default -> Direction.EAST;
                };
                arrowTrap(p[0], 1, p[1], out);
                set(p[0] + out.getStepX(), 1, p[1] + out.getStepZ(), pal.plate);
            }
        }
        // Приманка
        fill(5, 1, 5, 7, 1, 7, pal.accent);
        chest(6, 2, 6, Direction.SOUTH, loot("common"));
    }

    private void pillarsRoom() {
        for (int[] c : new int[][]{{3, 3}, {3, 8}, {8, 3}, {8, 8}}) {
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    for (int y = 1; y < C; y++) set(c[0] + dx, y, c[1] + dz, y == 1 || y == C - 1 ? pal.accent : pal.pillar);
            set(c[0] + (c[0] < 6 ? 2 : -1), 4, c[1], pal.wallLight);
        }
        if (h(3, 3, 3, 14) < 0.5f) spawner(6, 1, 6, mob(4));
        else set(6, 1, 6, pal.junk1);
    }

    private void hall() {
        // Рисунок круга на полу и центральный объект
        for (int x = 2; x <= M - 2; x++) {
            for (int z = 2; z <= M - 2; z++) {
                double r = Math.sqrt((x - 6) * (x - 6) + (z - 6) * (z - 6));
                if (r >= 3.5 && r < 4.5) set(x, 0, z, pal.floorBorder);
            }
        }
        switch (kind) {
            case ARCHIVE -> {
                set(6, 1, 6, Blocks.LECTERN.defaultBlockState());
                set(6, 0, 6, pal.wallLight);
                for (int[] c : new int[][]{{4, 4}, {4, 8}, {8, 4}, {8, 8}}) set(c[0], 1, c[1], Blocks.AMETHYST_CLUSTER);
            }
            case CRYPT -> {
                // Саркофаг
                fill(5, 1, 4, 7, 1, 8, pal.accent);
                fill(5, 2, 4, 7, 2, 8, slab(Blocks.STONE_BRICK_SLAB, false));
                set(6, 3, 5, skull(8));
                set(4, 1, 4, candles(3, true));
                set(8, 1, 8, candles(2, true));
            }
            case CITADEL -> {
                // Жаровня
                set(6, 1, 6, Blocks.NETHERRACK);
                set(6, 2, 6, Blocks.FIRE);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    set(6 + d.getStepX(), 1, 6 + d.getStepZ(), stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, d.getOpposite(), false));
                }
                for (int[] c : new int[][]{{5, 5}, {5, 7}, {7, 5}, {7, 7}}) set(c[0], 1, c[1], Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            }
        }
        // Висячие светильники
        for (int[] c : new int[][]{{3, 3}, {3, 9}, {9, 3}, {9, 9}}) {
            set(c[0], C - 1, c[1], Blocks.CHAIN.defaultBlockState());
            set(c[0], C - 2, c[1], hanging(pal.lantern));
        }
    }

    private void themed() {
        switch (kind) {
            case ARCHIVE -> {
                // Библиотека: стеллажи-проходы
                for (int x : new int[]{3, 9}) {
                    for (int z = 2; z <= 10; z++) {
                        if (z >= 5 && z <= 7) continue;
                        for (int y = 1; y <= 4; y++) {
                            float n = h(x, y, z, 15);
                            set(x, y, z, n < 0.2f ? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                                    : n < 0.55f ? ModBlocks.ARCHIVE_SHELF.get().defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
                        }
                        set(x, 5, z, slab(Blocks.DEEPSLATE_TILE_SLAB, false));
                    }
                }
                // Стол с книгами
                fill(5, 1, 2, 7, 1, 2, slab(Blocks.DARK_OAK_SLAB, true));
                set(6, 2, 2, candles(2, true));
                if (h(1, 2, 3, 16) < 0.6f) chest(5, 1, 10, Direction.NORTH, loot("common"));
                // Подвешенные фонари Эфира
                set(6, C - 1, 4, Blocks.CHAIN.defaultBlockState());
                set(6, C - 2, 4, hanging(Blocks.SOUL_LANTERN));
                set(6, C - 1, 8, Blocks.CHAIN.defaultBlockState());
                set(6, C - 2, 8, hanging(Blocks.SOUL_LANTERN));
            }
            case CRYPT -> {
                // Оссуарий: ниши с черепами в стенах и гробы
                for (int dir = 0; dir < 4; dir++) {
                    for (int t = 2; t <= 10; t += 2) {
                        if (hasDoor(dir) && t >= 4 && t <= 8) continue;
                        int[] p = wallPos(dir, t, M);
                        set(p[0], 2, p[1], Blocks.BONE_BLOCK);
                        set(p[0], 3, p[1], Blocks.BONE_BLOCK);
                        Direction in = switch (dir) {
                            case 0 -> Direction.SOUTH;
                            case 1 -> Direction.WEST;
                            case 2 -> Direction.NORTH;
                            default -> Direction.EAST;
                        };
                        int sx = p[0] + in.getStepX(), sz = p[1] + in.getStepZ();
                        if (h(t, dir, 0, 17) < 0.5f) set(sx, 1, sz, skull((dir * 4 + 8) & 15));
                    }
                }
                for (int z : new int[]{3, 9}) {
                    fill(4, 1, z, 8, 1, z, pal.accent);
                    fill(4, 2, z, 8, 2, z, slab(Blocks.STONE_BRICK_SLAB, false));
                }
                set(6, 1, 6, Blocks.SOUL_CAMPFIRE);
                if (h(2, 1, 3, 16) < 0.5f) chest(10, 1, 6, Direction.WEST, loot("common"));
            }
            case CITADEL -> {
                // Кузня: лавовый горн, наковальни, мехи
                fill(5, 0, 5, 7, 0, 7, Blocks.LAVA.defaultBlockState());
                for (int x = 4; x <= 8; x++) {
                    for (int z = 4; z <= 8; z++) {
                        if (x == 4 || x == 8 || z == 4 || z == 8) set(x, 1, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                    }
                }
                set(2, 1, 2, Blocks.ANVIL);
                set(10, 1, 2, Blocks.BLAST_FURNACE);
                set(2, 1, 10, Blocks.SMITHING_TABLE);
                set(10, 1, 10, Blocks.GRINDSTONE);
                for (int[] c : new int[][]{{4, 4}, {8, 8}}) {
                    for (int y = C - 1; y >= 4; y--) set(c[0], y, c[1], Blocks.CHAIN.defaultBlockState());
                }
                if (h(4, 1, 3, 16) < 0.6f) chest(10, 1, 6, Direction.WEST, loot("common"));
            }
        }
    }

    private void pit() {
        // Яма 7×7 в центре и крестовой мост
        for (int x = 3; x <= 9; x++) {
            for (int z = 3; z <= 9; z++) {
                for (int y = -5; y <= 0; y++) set(x, y, z, AIR);
                set(x, -6, z, pal.pitBottom);
                if (kind == DungeonKind.CITADEL) set(x, -5, z, Blocks.LAVA.defaultBlockState());
                if (kind == DungeonKind.ARCHIVE && h(x, -5, z, 18) < 0.35f)
                    set(x, -5, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                            .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.UP));
                if (kind == DungeonKind.CRYPT && h(x, -5, z, 18) < 0.25f) set(x, -5, z, Blocks.COBWEB);
            }
        }
        BlockState bridge = pal.bridge;
        for (int t = 3; t <= 9; t++) {
            if (hasDoor(1) || hasDoor(3) || !(hasDoor(0) || hasDoor(2))) set(t, 0, 6, bridge);
            if (hasDoor(0) || hasDoor(2)) set(6, 0, t, bridge);
        }
        if (h(0, 1, 0, 19) < 0.5f) {
            set(6, -5, 6, pal.accent);
            chest(6, -4, 6, Direction.NORTH, loot("common"));
        }
        // Мост — без перил, опасно. Светящийся центр
        set(6, C - 1, 6, Blocks.CHAIN.defaultBlockState());
        set(6, C - 2, 6, hanging(pal.lantern));
    }

    // =====================================================================
    // Зал босса 26×26, высота 15
    // =====================================================================
    private void buildBoss() {
        int size = 2 * S;          // 26
        int m = size - 1;          // 25
        int top = DungeonStructure.BOSS_H - 1;   // 14
        for (int x = 0; x <= m; x++) {
            for (int z = 0; z <= m; z++) {
                boolean edge = x == 0 || z == 0 || x == m || z == m;
                double r = Math.sqrt((x - 12.5) * (x - 12.5) + (z - 12.5) * (z - 12.5));
                for (int y = 0; y <= top; y++) {
                    BlockState s;
                    if (y == 0) {
                        if (edge) s = pal.wallBase;
                        else if (r < 2.6) s = pal.accent;
                        else if (r >= 5 && r < 6) s = pal.floorBorder;
                        else if (r >= 10 && r < 10.8) s = pal.floorBorder;
                        else s = ((x / 2 + z / 2) & 1) == 0 ? pal.floorA : pal.floorB;
                    } else if (y == top) {
                        s = r < 4 ? pal.accent : pal.ceiling(h(x, y, z, 2));
                    } else if (edge) {
                        s = y == 1 ? pal.wallBase : (y == 6 || y == top - 1) ? pal.wallTop : pal.wall(h(x, y, z, 3));
                    } else s = AIR;
                    set(x, y, z, s);
                }
            }
        }
        // Окна из призрачного стекла под потолком
        for (int dir = 0; dir < 4; dir++) {
            for (int t = 3; t < m; t += 4) {
                int[] p = wallPos(dir, t, m);
                set(p[0], 10, p[1], pal.glass);
                set(p[0], 11, p[1], pal.glass);
            }
        }
        // Двери
        for (int dir = 0; dir < 4; dir++) for (int half = 0; half < 2; half++) if ((doors & (1 << (dir * 2 + half))) != 0) door(dir, half, size);
        // Кольцо колонн 2×2
        int[][] cols = {{5, 5}, {5, 19}, {19, 5}, {19, 19}, {3, 12}, {21, 12}, {12, 3}, {12, 21}};
        for (int[] c : cols) {
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    for (int y = 1; y < top; y++) {
                        BlockState s = y == 1 || y == top - 1 ? pal.accent : y % 4 == 0 ? pal.wallTop : pal.pillar;
                        set(c[0] + dx, y, c[1] + dz, s);
                    }
            // Огни на колоннах
            set(c[0] - 1, 5, c[1], pal.wallLight);
            set(c[0] + 2, 5, c[1] + 1, pal.wallLight);
        }
        // Помост и Печать Стража
        for (int x = 10; x <= 15; x++) {
            for (int z = 10; z <= 15; z++) {
                boolean rim = x == 10 || x == 15 || z == 10 || z == 15;
                set(x, 1, z, rim ? slab(pal.slab, false) : pal.accent);
            }
        }
        set(12, 2, 12, ModBlocks.GUARDIAN_SEAL.get().defaultBlockState().setValue(GuardianSealBlock.KIND, kind.ordinal()));
        for (int[] c : new int[][]{{11, 11}, {11, 14}, {14, 11}, {14, 14}}) {
            set(c[0], 2, c[1], candles(4, true));
        }
        // Висячие огни на цепях
        for (int[] c : new int[][]{{8, 8}, {8, 17}, {17, 8}, {17, 17}}) {
            for (int y = top - 1; y >= top - 4; y--) set(c[0], y, c[1], Blocks.CHAIN.defaultBlockState());
            set(c[0], top - 5, c[1], hanging(pal.lantern));
        }
        // Награда у восточной стены
        chest(m - 1, 1, 12, Direction.WEST, loot("boss"));
        chest(m - 1, 1, 13, Direction.WEST, loot("boss"));
        set(m - 1, 1, 11, pal.pillar);
        set(m - 1, 1, 14, pal.pillar);
        set(m - 1, 2, 11, candles(3, true));
        set(m - 1, 2, 14, candles(3, true));

        switch (kind) {
            case ARCHIVE -> {
                for (int dir = 0; dir < 4; dir++) {
                    for (int t = 2; t < m - 1; t++) {
                        if (isBossDoorZone(dir, t) || (dir == 1 && t >= 10 && t <= 15)) continue;
                        int[] p = wallPos(dir, t, m);
                        Direction in = switch (dir) {
                            case 0 -> Direction.SOUTH;
                            case 1 -> Direction.WEST;
                            case 2 -> Direction.NORTH;
                            default -> Direction.EAST;
                        };
                        int x = p[0] + in.getStepX(), z = p[1] + in.getStepZ();
                        for (int y = 1; y <= 5; y++) {
                            float n = h(x, y, z, 20);
                            set(x, y, z, n < 0.25f ? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                                    : n < 0.6f ? ModBlocks.ARCHIVE_SHELF.get().defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
                        }
                    }
                }
                for (int[] c : new int[][]{{7, 12}, {18, 12}, {12, 7}, {12, 18}}) set(c[0], 1, c[1], Blocks.AMETHYST_CLUSTER);
            }
            case CRYPT -> {
                for (int[] c : new int[][]{{8, 3}, {16, 3}, {8, 21}, {16, 21}}) {
                    fill(c[0], 1, c[1], c[0] + 2, 1, c[1] + 1, pal.accent);
                    fill(c[0], 2, c[1], c[0] + 2, 2, c[1] + 1, slab(Blocks.STONE_BRICK_SLAB, false));
                    set(c[0] + 1, 3, c[1], skull(0));
                }
                for (int[] c : new int[][]{{3, 3}, {3, 22}, {22, 3}, {22, 22}}) {
                    set(c[0], 1, c[1], Blocks.SOUL_CAMPFIRE);
                }
            }
            case CITADEL -> {
                // Лавовый ров вокруг помоста с четырьмя мостами
                for (int x = 1; x < m; x++) {
                    for (int z = 1; z < m; z++) {
                        double r = Math.sqrt((x - 12.5) * (x - 12.5) + (z - 12.5) * (z - 12.5));
                        boolean bridge = (x >= 12 && x <= 13) || (z >= 12 && z <= 13);
                        if (r >= 6.2 && r < 8.2 && !bridge) {
                            set(x, 0, z, Blocks.LAVA.defaultBlockState());
                            set(x, -1, z, Blocks.MAGMA_BLOCK);
                        }
                    }
                }
                // Трон
                set(m - 3, 1, 12, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, false));
                set(m - 3, 1, 13, stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, false));
                fill(m - 2, 1, 11, m - 2, 4, 14, Blocks.GILDED_BLACKSTONE.defaultBlockState());
            }
        }
        if (kind == DungeonKind.CITADEL) citadelExterior(size);
        scatter(size, top);
    }

    // =====================================================================
    // Винтовая шахта 7×7 от входа до поверхности
    // =====================================================================
    private static final int[][] RING = buildRing();

    private static int[][] buildRing() {
        int[][] r = new int[16][];
        int i = 0;
        for (int x = 1; x <= 5; x++) r[i++] = new int[]{x, 1};
        for (int z = 2; z <= 5; z++) r[i++] = new int[]{5, z};
        for (int x = 4; x >= 1; x--) r[i++] = new int[]{x, 5};
        for (int z = 4; z >= 2; z--) r[i++] = new int[]{1, z};
        return r;
    }

    private void buildShaft() {
        int surface = extra;
        int bottom = 1;                       // пол входа + 1 (локально к полу входа)
        int ceil = C;                         // потолок входной комнаты
        int topFloor = surface - floorY;      // уровень поверхности относительно пола
        int roof = topFloor + 4;
        // Смещение: шахта стоит в клетке 3..9 входной комнаты, её локальные 0..6 → x0()+0..6
        for (int y = bottom; y <= roof; y++) {
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    boolean edge = x == 0 || z == 0 || x == 6 || z == 6;
                    boolean core = x >= 2 && x <= 4 && z >= 2 && z <= 4;
                    BlockState s;
                    if (y < ceil) {
                        // внутри входной комнаты — только лестница и столб
                        if (core) s = y % 4 == 0 ? pal.accent : pal.pillar;
                        else continue;
                    } else if (y == roof) {
                        if (edge) s = ((x + z) & 1) == 0 ? pal.pillar : AIR;
                        else s = pal.accent;
                    } else if (edge) {
                        boolean arch = y > topFloor && y <= topFloor + 3 && (x == 3 || z == 3);
                        boolean ruin = y > topFloor + 1 && h(x, y, z, 21) < 0.22f;
                        s = arch || ruin ? AIR : y % 6 == 0 && (x == 3 || z == 3) ? pal.wallLight : pal.wall(h(x, y, z, 22));
                    } else if (y == topFloor) {
                        s = core ? pal.accent : pal.floorA;
                    } else if (y > topFloor) {
                        s = AIR;
                    } else if (core) {
                        s = y % 4 == 0 ? pal.accent : pal.pillar;
                    } else {
                        s = AIR;
                    }
                    set(x, y, z, s);
                }
            }
            // Ступени
            if (y <= topFloor) {
                int k = Math.floorMod(y - bottom, 16);
                int[] c = RING[k];
                int[] n = RING[(k + 1) % 16];
                Direction d = Direction.fromDelta(n[0] - c[0], 0, n[1] - c[1]);
                if (d == null) d = Direction.NORTH;
                set(c[0], y, c[1], stairs(pal.stairs, d, false));
                if (y == topFloor) {
                    set(c[0], y, c[1], pal.floorA);
                    for (int b = 1; b <= 3; b++) {
                        int[] o = RING[Math.floorMod(k - b, 16)];
                        set(o[0], y, o[1], AIR);
                    }
                }
            }
        }
        // Маяк Эфира на крыше
        set(3, roof + 1, 3, pal.wallLight);
    }

    // =====================================================================
    // Палитры
    // =====================================================================
    static final class Palette {
        BlockState[] walls;
        float[] wallW;
        BlockState wallBase, wallTop, accent, pillar, floorA, floorB, floorBorder, floorWorn, ceilingA, ceilingB;
        BlockState wallLight, carpet, junk1, junk2, plate, bridge, pitBottom, glass;
        Block lantern, slab, stairs;
        float cobweb;
        Supplier<? extends EntityType<?>>[] mobs;

        BlockState wall(float n) {
            float acc = 0;
            for (int i = 0; i < walls.length; i++) {
                acc += wallW[i];
                if (n < acc) return walls[i];
            }
            return walls[0];
        }

        BlockState ceiling(float n) {
            return n < 0.7f ? ceilingA : ceilingB;
        }

        @SuppressWarnings("unchecked")
        static Palette of(DungeonKind kind) {
            Palette p = new Palette();
            switch (kind) {
                case ARCHIVE -> {
                    p.walls = new BlockState[]{Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
                            ModBlocks.RUNE_BRICKS.get().defaultBlockState(), ModBlocks.CRACKED_RUNE_BRICKS.get().defaultBlockState()};
                    p.wallW = new float[]{0.62f, 0.14f, 0.16f, 0.08f};
                    p.wallBase = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                    p.wallTop = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    p.accent = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                    p.pillar = pillar(ModBlocks.RUNE_PILLAR.get());
                    p.floorA = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                    p.floorB = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    p.floorBorder = ModBlocks.RUNE_BRICKS.get().defaultBlockState();
                    p.floorWorn = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
                    p.ceilingA = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    p.ceilingB = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                    p.wallLight = ModBlocks.PHANTOM_LANTERN.get().defaultBlockState();
                    p.carpet = Blocks.PURPLE_CARPET.defaultBlockState();
                    p.junk1 = Blocks.AMETHYST_CLUSTER.defaultBlockState();
                    p.junk2 = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
                    p.plate = Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState();
                    p.bridge = Blocks.DEEPSLATE_TILES.defaultBlockState();
                    p.pitBottom = Blocks.SCULK.defaultBlockState();
                    p.glass = ModBlocks.PHANTOM_GLASS.get().defaultBlockState();
                    p.lantern = Blocks.SOUL_LANTERN;
                    p.slab = Blocks.DEEPSLATE_TILE_SLAB;
                    p.stairs = Blocks.DEEPSLATE_TILE_STAIRS;
                    p.cobweb = 0.08f;
                    p.mobs = new Supplier[]{ModEntities.GLASSMAN, ModEntities.ETHER_WISP, ModEntities.SALT_WRAITH, () -> EntityType.VEX};
                }
                case CRYPT -> {
                    p.walls = new BlockState[]{Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                            Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.TUFF_BRICKS.defaultBlockState()};
                    p.wallW = new float[]{0.5f, 0.2f, 0.2f, 0.1f};
                    p.wallBase = Blocks.TUFF_BRICKS.defaultBlockState();
                    p.wallTop = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
                    p.accent = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                    p.pillar = Blocks.POLISHED_TUFF.defaultBlockState();
                    p.floorA = Blocks.STONE_BRICKS.defaultBlockState();
                    p.floorB = Blocks.POLISHED_ANDESITE.defaultBlockState();
                    p.floorBorder = Blocks.CHISELED_TUFF.defaultBlockState();
                    p.floorWorn = Blocks.SOUL_SOIL.defaultBlockState();
                    p.ceilingA = Blocks.STONE_BRICKS.defaultBlockState();
                    p.ceilingB = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                    p.wallLight = ModBlocks.PHANTOM_LANTERN.get().defaultBlockState();
                    p.carpet = Blocks.GRAY_CARPET.defaultBlockState();
                    p.junk1 = Blocks.BONE_BLOCK.defaultBlockState();
                    p.junk2 = candles(3, true);
                    p.plate = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
                    p.bridge = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                    p.pitBottom = Blocks.SOUL_SAND.defaultBlockState();
                    p.glass = ModBlocks.PHANTOM_GLASS.get().defaultBlockState();
                    p.lantern = Blocks.SOUL_LANTERN;
                    p.slab = Blocks.STONE_BRICK_SLAB;
                    p.stairs = Blocks.STONE_BRICK_STAIRS;
                    p.cobweb = 0.2f;
                    p.mobs = new Supplier[]{ModEntities.SALT_WRAITH, () -> EntityType.SKELETON, () -> EntityType.ZOMBIE, ModEntities.SCAR_CRAWLER};
                }
                case CITADEL -> {
                    p.walls = new BlockState[]{Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                            ModBlocks.ASH_BRICKS.get().defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState()};
                    p.wallW = new float[]{0.6f, 0.18f, 0.17f, 0.05f};
                    p.wallBase = Blocks.BLACKSTONE.defaultBlockState();
                    p.wallTop = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    p.accent = ModBlocks.EMBER_BRICKS.get().defaultBlockState();
                    p.pillar = pillar(Blocks.POLISHED_BASALT);
                    p.floorA = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    p.floorB = ModBlocks.ASH_BRICKS.get().defaultBlockState();
                    p.floorBorder = ModBlocks.EMBER_BRICKS.get().defaultBlockState();
                    p.floorWorn = Blocks.MAGMA_BLOCK.defaultBlockState();
                    p.ceilingA = Blocks.BLACKSTONE.defaultBlockState();
                    p.ceilingB = Blocks.BASALT.defaultBlockState();
                    p.wallLight = Blocks.SHROOMLIGHT.defaultBlockState();
                    p.carpet = Blocks.RED_CARPET.defaultBlockState();
                    p.junk1 = Blocks.MAGMA_BLOCK.defaultBlockState();
                    p.junk2 = Blocks.CHAIN.defaultBlockState();
                    p.plate = Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState();
                    p.bridge = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    p.pitBottom = Blocks.MAGMA_BLOCK.defaultBlockState();
                    p.glass = Blocks.TINTED_GLASS.defaultBlockState();
                    p.lantern = Blocks.LANTERN;
                    p.slab = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB;
                    p.stairs = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS;
                    p.cobweb = 0.03f;
                    p.mobs = new Supplier[]{ModEntities.ASH_HOUND, () -> EntityType.BLAZE, () -> EntityType.WITHER_SKELETON, () -> EntityType.MAGMA_CUBE};
                }
            }
            return p;
        }
    }
}
