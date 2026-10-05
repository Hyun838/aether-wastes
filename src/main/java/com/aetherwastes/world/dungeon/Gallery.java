package com.aetherwastes.world.dungeon;

import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * Витрина для операторов и скриншотов: /aether gallery gear | mobs.
 * Строит каменную площадку, ставит стойки с каждым комплектом брони и оружием или ряд существ.
 */
public final class Gallery {
    private Gallery() {}

    private static BlockPos platform(ServerLevel level, BlockPos near, int dx, int width, int depth) {
        int x0 = near.getX() + dx, z0 = near.getZ() - 40;
        int y = 0;
        for (int x = -6; x < width + 6; x += 2) {
            for (int z = -14; z < depth + 4; z += 2) {
                level.getChunk((x0 + x) >> 4, (z0 + z) >> 4);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + x, z0 + z));
            }
        }
        y += 3;
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState rim = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        for (int x = -2; x < width + 2; x++) {
            for (int z = -10; z < depth + 2; z++) {
                boolean edge = x == -2 || x == width + 1 || z == -10 || z == depth + 1;
                level.setBlock(new BlockPos(x0 + x, y - 1, z0 + z), edge ? rim : floor, 2);
                for (int h = 0; h < 6; h++) level.setBlock(new BlockPos(x0 + x, y + h, z0 + z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // Задник и светильники
        for (int x = -2; x < width + 2; x++) {
            for (int h = 0; h < 5; h++) {
                BlockState s = h == 0 || h == 4 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : (x % 4 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
                level.setBlock(new BlockPos(x0 + x, y + h, z0 + depth + 1), s, 2);
            }
            if (x % 4 == 2) level.setBlock(new BlockPos(x0 + x, y + 3, z0 + depth), Blocks.SOUL_LANTERN.defaultBlockState(), 2);
        }
        return new BlockPos(x0, y, z0);
    }

    private static void look(ServerPlayer p, double cx, double cy, double cz, double tx, double ty, double tz) {
        double dx = tx - cx, dy = ty - cy, dz = tz - cz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(p.serverLevel(), cx, cy, cz, yaw, pitch);
        p.getAbilities().flying = p.getAbilities().mayfly;
        p.onUpdateAbilities();
    }

    public static boolean gear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<DeferredItem<?>[]> sets = List.of(ModGear.ASHEN_SET, ModGear.ETHER_STEEL_SET, ModGear.PRISM_SET,
                ModGear.STAR_IRON_SET, ModGear.ARCHIVIST_SET, ModGear.PHANTOM_SET);
        List<DeferredItem<Item>> weapons = List.of(ModGear.SALT_SCYTHE, ModGear.ETHER_BLADE, ModGear.PRISM_DAGGER,
                ModGear.STAR_LONGSWORD, ModGear.SPARK_SCEPTER, ModGear.CONVERGENCE_GLAIVE);
        int gap = 3, width = sets.size() * gap;
        BlockPos o = platform(level, level.getSharedSpawnPos(), 60, width, 2);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < sets.size(); i++) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) continue;
            stand.moveTo(o.getX() + i * gap + 0.5, o.getY(), o.getZ() + 0.5, 180f, 0f);
            stand.setShowArms(true);
            stand.setNoBasePlate(true);
            stand.setRightArmPose(new Rotations(-60f, 10f, 0f));
            stand.setLeftArmPose(new Rotations(-10f, 0f, -10f));
            for (int k = 0; k < 4; k++) stand.setItemSlot(slots[k], new ItemStack((Item) sets.get(i)[k].get()));
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons.get(i).get()));
            level.addFreshEntity(stand);
        }
        double cx = o.getX() + width / 2.0 - 1.0;
        look(p, cx, o.getY() + 1.9, o.getZ() - 6.0, cx, o.getY() + 1.1, o.getZ() + 0.5);
        return true;
    }

    /** Ряд реликвийного оружия на невидимых стойках — крупный план 3D-моделей. */
    public static boolean weapons(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<DeferredItem<Item>> weapons = List.of(ModGear.ETHER_BLADE, ModGear.STAR_LONGSWORD, ModGear.HEART_BLADE,
                ModGear.CHRONICLE_BLADE, ModGear.SALT_SCYTHE, ModGear.ECHO_REAPER, ModGear.CONVERGENCE_GLAIVE,
                ModGear.EMBER_GREATAXE, ModGear.COLOSSUS_HAMMER, ModGear.RESONANCE_MAUL, ModGear.SPARK_SCEPTER, ModGear.PRISM_DAGGER);
        int gap = 1;
        int width = weapons.size() * gap;
        BlockPos o = platform(level, level.getSharedSpawnPos(), 180, width, 2);
        for (int i = 0; i < weapons.size(); i++) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) continue;
            stand.moveTo(o.getX() + i * gap + 0.5, o.getY(), o.getZ() + 0.5, 90f, 0f);
            stand.setInvisible(true);
            stand.setShowArms(true);
            stand.setNoBasePlate(true);
            stand.setNoGravity(true);
            stand.setRightArmPose(new Rotations(-90f, 0f, 0f));
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons.get(i).get()));
            level.addFreshEntity(stand);
        }
        double cx = o.getX() + width / 2.0;
        look(p, cx, o.getY() + 1.6, o.getZ() - 4.2, cx, o.getY() + 1.3, o.getZ() + 0.5);
        return true;
    }

    /** OBJ-объекты: обелиск с кристаллом, ритуальный фокус с кольцом и три Печати стражей. */
    public static boolean arcane(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos o = platform(level, level.getSharedSpawnPos(), 220, 12, 2);
        level.setBlock(o.offset(1, 0, 0), com.aetherwastes.registry.ModBlocks.PURIFYING_OBELISK.get().defaultBlockState(), 3);
        level.setBlock(o.offset(4, 0, 0), com.aetherwastes.registry.ModBlocks.RITUAL_FOCUS.get().defaultBlockState(), 3);
        if (level.getBlockEntity(o.offset(4, 0, 0)) instanceof com.aetherwastes.block.HolderBlockEntity h) {
            h.setItem(new ItemStack(ModGear.ECHO_REAPER.get()));
        }
        for (int k = 0; k < 3; k++) {
            BlockState seal = com.aetherwastes.registry.ModBlocks.GUARDIAN_SEAL.get().defaultBlockState()
                    .setValue(com.aetherwastes.block.GuardianSealBlock.KIND, k)
                    .setValue(com.aetherwastes.block.GuardianSealBlock.ACTIVE, true);
            level.setBlock(o.offset(7 + k * 2, 0, 0), seal, 3);
        }
        double cx = o.getX() + 6.5;
        look(p, cx, o.getY() + 2.6, o.getZ() - 7.5, cx, o.getY() + 1.6, o.getZ() + 0.5);
        return true;
    }

    /** Эффекты 1.3: ударные волны трёх цветов, объёмные осколки, искры с отскоком. */
    public static boolean fx(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos o = platform(level, level.getSharedSpawnPos(), 260, 12, 2);
        double cx = o.getX() + 6.5;
        look(p, cx, o.getY() + 3.0, o.getZ() - 6.5, cx, o.getY() + 0.6, o.getZ() + 0.5); // сначала камера: частицы шлются игрокам рядом
        for (int k = 0; k < 3; k++) {
            double x = o.getX() + 2 + k * 4, y = o.getY() + 0.02, z = o.getZ() + 0.5;
            com.aetherwastes.registry.ModParticles.shockwave(level, x, y, z, 2.6, k);
            var shard = k == 0 ? com.aetherwastes.registry.ModParticles.ETHER_SHARD.get()
                    : k == 1 ? com.aetherwastes.registry.ModParticles.EMBER_SHARD.get() : com.aetherwastes.registry.ModParticles.PHANTOM_SHARD.get();
            level.sendParticles(shard, x, y + 0.8, z, 30, 0.3, 0.3, 0.3, 0.3);
            level.sendParticles(com.aetherwastes.registry.ModParticles.ETHER_SPARK.get(), x, y + 1.0, z, 30, 0.2, 0.2, 0.2, 0.25);
        }
        return true;
    }

    public static boolean bosses(ServerPlayer p) {
        return row(p, List.<EntityType<? extends Mob>>of(ModEntities.ARCHIVE_KEEPER.get(), ModEntities.ECHO_LORD.get(), ModEntities.ASH_COLOSSUS.get(),
                ModEntities.WANDERER_SPARK.get(), ModEntities.WANDERER_HEART.get()), 6, 140, 13.0, 4.5);
    }

    public static boolean mobs(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<EntityType<? extends Mob>> types = List.<EntityType<? extends Mob>>of(ModEntities.ASH_HOUND.get(), ModEntities.SALT_WRAITH.get(),
                ModEntities.RESIN_WALKER.get(), ModEntities.GLASSMAN.get(), ModEntities.ETHER_WISP.get(),
                ModEntities.SCAR_CRAWLER.get());
        return row(p, types, 3, 100, 6.5, 2.0);
    }

    private static boolean row(ServerPlayer p, List<? extends EntityType<? extends Mob>> types, int gap, int dx, double back, double camY) {
        ServerLevel level = p.serverLevel();
        int width = types.size() * gap;
        BlockPos o = platform(level, level.getSharedSpawnPos(), dx, width, 2);
        for (int i = 0; i < types.size(); i++) {
            Mob m = types.get(i).create(level);
            if (m == null) continue;
            m.moveTo(o.getX() + i * gap + 0.5, o.getY(), o.getZ() + 0.5, 180f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(o), MobSpawnType.COMMAND, null);
            m.setNoAi(true);
            m.setYHeadRot(180f);
            m.setYBodyRot(180f);
            m.setPersistenceRequired();
            m.addTag("aw_spawned");
            level.addFreshEntity(m);
        }
        double cx = o.getX() + width / 2.0 - gap / 2.0 + 0.5;
        look(p, cx, o.getY() + camY, o.getZ() - back, cx, o.getY() + 1.2, o.getZ() + 0.5);
        return true;
    }
}
