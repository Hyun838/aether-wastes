package com.aetherwastes.world;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.block.WorldBlocks;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Процедурные локации и «биомные» наросты Пустошей.
 * Каждая постройка собирается из случайных частей, поэтому руины не повторяются.
 */
public final class WastesFeatures {
    private WastesFeatures() {}

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    private static void chest(WorldGenLevel level, BlockPos pos, String table, RandomSource random) {
        set(level, pos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity cb) {
            cb.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, AetherWastes.id("chests/" + table)), random.nextLong());
        }
    }

    private static void spawner(WorldGenLevel level, BlockPos pos, EntityType<?> type, RandomSource random) {
        set(level, pos, Blocks.SPAWNER.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity sb) sb.setEntityId(type, random);
    }

    private static void room(WorldGenLevel level, BlockPos min, int sx, int sy, int sz, BlockState wall, BlockState floor, RandomSource r) {
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++) {
                    BlockPos p = min.offset(x, y, z);
                    boolean shell = x == 0 || z == 0 || x == sx - 1 || z == sz - 1 || y == sy - 1;
                    if (y == 0) set(level, p, floor);
                    else if (shell) set(level, p, r.nextFloat() < 0.2f ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : r.nextFloat() < 0.2f ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : wall);
                    else set(level, p, Blocks.CAVE_AIR.defaultBlockState());
                }
    }

    /** Руины Архива: главный зал-библиотека и 1–3 боковые комнаты, связанные коридорами. */
    public static class ArchiveRuins extends Feature<NoneFeatureConfiguration> {
        public ArchiveRuins() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!level.getBlockState(o).is(BlockTags.BASE_STONE_OVERWORLD)) return false;
            BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
            room(level, o.offset(-4, 0, -4), 9, 6, 9, wall, Blocks.POLISHED_ANDESITE.defaultBlockState(), r);
            for (int i = -3; i <= 3; i++) {
                if (r.nextFloat() < 0.7f) set(level, o.offset(i, 1, -3), Blocks.BOOKSHELF.defaultBlockState());
                if (r.nextFloat() < 0.7f) set(level, o.offset(i, 2, -3), Blocks.BOOKSHELF.defaultBlockState());
                if (r.nextFloat() < 0.5f) set(level, o.offset(-3, 1, i), Blocks.BOOKSHELF.defaultBlockState());
            }
            set(level, o.offset(0, 1, 0), Blocks.LECTERN.defaultBlockState());
            set(level, o.offset(0, 4, 0), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
            chest(level, o.offset(3, 1, 3), "archive_ruins", r);

            Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            int branches = 1 + r.nextInt(3);
            for (int b = 0; b < branches; b++) {
                Direction d = dirs[r.nextInt(4)];
                for (int step = 5; step <= 7; step++) {
                    BlockPos c = o.relative(d, step);
                    for (int h = 1; h <= 2; h++) set(level, c.above(h), Blocks.CAVE_AIR.defaultBlockState());
                    set(level, c, Blocks.STONE_BRICKS.defaultBlockState());
                }
                BlockPos rc = o.relative(d, 10);
                room(level, rc.offset(-2, 0, -2), 5, 4, 5, wall, Blocks.STONE_BRICKS.defaultBlockState(), r);
                for (int h = 1; h <= 2; h++) set(level, o.relative(d, 8).above(h), Blocks.CAVE_AIR.defaultBlockState());
                switch (r.nextInt(3)) {
                    case 0 -> chest(level, rc.above(), "archive_ruins", r);
                    case 1 -> spawner(level, rc.above(), r.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON, r);
                    default -> set(level, rc.above(), Blocks.BOOKSHELF.defaultBlockState());
                }
            }
            return true;
        }
    }

    /** Гробница Эха: склеп из чернокаменных кирпичей с душами. */
    public static class EchoTomb extends Feature<NoneFeatureConfiguration> {
        public EchoTomb() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!level.getBlockState(o).is(BlockTags.BASE_STONE_OVERWORLD)) return false;
            room(level, o.offset(-3, 0, -3), 7, 5, 7, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                    Blocks.SOUL_SOIL.defaultBlockState(), r);
            spawner(level, o.above(), r.nextBoolean() ? EntityType.SKELETON : EntityType.ZOMBIE, r);
            chest(level, o.offset(2, 1, 2), "echo_tomb", r);
            set(level, o.offset(-2, 1, -2), Blocks.SOUL_LANTERN.defaultBlockState());
            set(level, o.offset(2, 1, -2), Blocks.SOUL_LANTERN.defaultBlockState());
            return true;
        }
    }

    /** Затонувшая обсерватория на дне океана. */
    public static class SunkenObservatory extends Feature<NoneFeatureConfiguration> {
        public SunkenObservatory() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ());
            BlockPos base = new BlockPos(o.getX(), floor, o.getZ());
            if (!level.getBlockState(base).is(Blocks.WATER)) return false;
            int height = 10 + r.nextInt(5);
            for (int y = 0; y < height; y++) {
                for (int x = -2; x <= 2; x++)
                    for (int z = -2; z <= 2; z++) {
                        boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                        BlockPos p = base.offset(x, y, z);
                        if (y == 0) set(level, p, Blocks.DEEPSLATE_TILES.defaultBlockState());
                        else if (wall && !(z == -2 && x == 0 && y < 3)) {
                            set(level, p, (y % 4 == 3) ? Blocks.TINTED_GLASS.defaultBlockState()
                                    : r.nextFloat() < 0.25f ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                        }
                    }
            }
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++) set(level, base.offset(x, height, z), Blocks.GLASS.defaultBlockState());
            chest(level, base.offset(1, 1, 1), "sunken_observatory", r);
            set(level, base.offset(-1, 1, 1), Blocks.SEA_LANTERN.defaultBlockState());
            return true;
        }
    }

    /** Стеклянные дюны: шипы Эфирного стекла в пустынях. */
    public static class GlassSpike extends Feature<NoneFeatureConfiguration> {
        public GlassSpike() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int n = 3 + r.nextInt(5);
            for (int i = 0; i < n; i++) {
                int x = o.getX() + r.nextInt(9) - 4, z = o.getZ() + r.nextInt(9) - 4;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (!level.getBlockState(new BlockPos(x, y - 1, z)).is(BlockTags.SAND)) continue;
                int h = 2 + r.nextInt(5);
                for (int k = 0; k < h; k++) set(level, new BlockPos(x, y + k, z), ModBlocks.ETHER_GLASS.get().defaultBlockState());
            }
            return true;
        }
    }

    /** Кровоточащий лес: деревья с красной кроной, сочащиеся смолой. */
    public static class BleedingTree extends Feature<NoneFeatureConfiguration> {
        public BleedingTree() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
            BlockPos base = new BlockPos(o.getX(), y, o.getZ());
            if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
            int h = 5 + r.nextInt(3);
            BlockState log = ModBlocks.BLEEDING_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                    .setValue(WorldBlocks.BleedingLog.TAPPED, false);
            for (int k = 0; k < h; k++) set(level, base.above(k), log);
            BlockState leaves = Blocks.NETHER_WART_BLOCK.defaultBlockState();
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    for (int dy = -1; dy <= 1; dy++) {
                        if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) > 3) continue;
                        BlockPos p = base.offset(dx, h + dy, dz);
                        if (level.getBlockState(p).isAir()) set(level, p, leaves);
                    }
            return true;
        }
    }

    /** Солончаки Шёпота: пятна соляной корки. */
    public static class SaltFlat extends Feature<NoneFeatureConfiguration> {
        public SaltFlat() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int radius = 3 + r.nextInt(4);
            boolean any = false;
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    int x = o.getX() + dx, z = o.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (st.is(BlockTags.SAND) || st.is(BlockTags.DIRT) || st.is(BlockTags.TERRACOTTA) || st.is(Blocks.GRAVEL)) {
                        set(level, p, ModBlocks.SALT_CRUST.get().defaultBlockState());
                        any = true;
                    }
                }
            return any;
        }
    }

    /** Грибные катакомбы: гигантские светящиеся грибы в пещерах. */
    public static class Glowcap extends Feature<NoneFeatureConfiguration> {
        public Glowcap() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            BlockPos base = null;
            for (int dy = 0; dy < 12; dy++) {
                BlockPos p = o.below(dy);
                BlockState below = level.getBlockState(p.below());
                if (level.getBlockState(p).isAir() && (below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.DIRT))) {
                    base = p;
                    break;
                }
            }
            if (base == null) return false;
            int h = 3 + r.nextInt(3);
            for (int k = 0; k < h; k++) {
                if (!level.getBlockState(base.above(k)).isAir()) return k > 0;
                set(level, base.above(k), Blocks.MUSHROOM_STEM.defaultBlockState());
            }
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    BlockPos p = base.offset(dx, h, dz);
                    if (level.getBlockState(p).isAir()) set(level, p, ModBlocks.GLOWCAP_BLOCK.get().defaultBlockState());
                }
            set(level, base.below(), Blocks.MYCELIUM.defaultBlockState());
            return true;
        }
    }

    /** Застывшая Буря: остановленные во времени молнии на вершинах. */
    public static class FrozenStorm extends Feature<NoneFeatureConfiguration> {
        public FrozenStorm() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
            BlockPos p = new BlockPos(o.getX(), y, o.getZ());
            int len = 6 + r.nextInt(8);
            for (int i = 0; i < len; i++) {
                if (level.getBlockState(p).isAir()) set(level, p, ModBlocks.CHARGED_CRYSTAL.get().defaultBlockState());
                p = p.offset(r.nextInt(3) - 1, 1, r.nextInt(3) - 1);
            }
            return true;
        }
    }

    /** Парящие обломки: островки земли со звёздным железом высоко в небе. */
    public static class FloatingDebris extends Feature<NoneFeatureConfiguration> {
        public FloatingDebris() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int cy = Math.max(o.getY(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ()) + 40);
            if (cy > level.getMaxBuildHeight() - 12) return false;
            BlockPos c = new BlockPos(o.getX(), cy, o.getZ());
            int radius = 3 + r.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = -radius; dy <= 0; dy++) {
                        double d = Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 1.4;
                        if (d > radius) continue;
                        BlockPos p = c.offset(dx, dy, dz);
                        BlockState st = dy == 0 ? Blocks.GRASS_BLOCK.defaultBlockState()
                                : dy == -1 ? Blocks.DIRT.defaultBlockState()
                                : r.nextFloat() < 0.12f ? ModBlocks.STAR_IRON_ORE.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
                        set(level, p, st);
                    }
            if (r.nextBoolean()) {
                for (int k = 1; k <= 3; k++) set(level, c.above(k), Blocks.BIRCH_LOG.defaultBlockState());
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        set(level, c.offset(dx, 4, dz), Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
            return true;
        }
    }

    /** Пепельные равнины: пепел, мёртвые кусты и обломки старых стен. */
    public static class AshPatch extends Feature<NoneFeatureConfiguration> {
        public AshPatch() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int radius = 3 + r.nextInt(3);
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius || r.nextFloat() < 0.25f) continue;
                    int x = o.getX() + dx, z = o.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(BlockTags.DIRT)) {
                        set(level, p, ModBlocks.ASH_BLOCK.get().defaultBlockState());
                        if (r.nextFloat() < 0.15f && level.getBlockState(p.above()).isAir()) set(level, p.above(), Blocks.DEAD_BUSH.defaultBlockState());
                    }
                }
            if (r.nextFloat() < 0.5f) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
                BlockPos w = new BlockPos(o.getX(), y, o.getZ());
                int len = 3 + r.nextInt(4);
                for (int i = 0; i < len; i++) {
                    int h = 1 + r.nextInt(3);
                    for (int k = 0; k < h; k++) {
                        set(level, w.east(i).above(k), r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
                if (r.nextFloat() < 0.3f) chest(level, w.south(), "ash_ruin", r);
            }
            return true;
        }
    }
}
