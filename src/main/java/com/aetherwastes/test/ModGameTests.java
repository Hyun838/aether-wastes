package com.aetherwastes.test;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.magic.SpellCaster;
import com.aetherwastes.magic.SpellData;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.survival.Mutations;
import com.aetherwastes.survival.Nutrition;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.survival.Temperature;
import com.aetherwastes.survival.Traumas;
import com.aetherwastes.threats.Pulse;
import com.aetherwastes.threats.Scar;
import com.aetherwastes.threats.Wanderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Автотесты запуска. Прогоняют в живом сервере блоки, мобов, генерацию построек,
 * все заклинания и гибриды, тики выживания и угроз. Ошибки пишутся в журнал с меткой [aw-test].
 */
@GameTestHolder(AetherWastes.MODID)
@PrefixGameTestTemplate(false)
public class ModGameTests {

    private static void report(GameTestHelper helper, String name, List<String> errors) {
        if (errors.isEmpty()) {
            AetherWastes.LOGGER.info("[aw-test] {}: OK", name);
            helper.succeed();
        } else {
            for (String e : errors) AetherWastes.LOGGER.error("[aw-test] {}: {}", name, e);
            helper.fail(name + ": " + errors.size() + " ошибок, см. журнал");
        }
    }

    private static String err(String what, Throwable t) {
        StringBuilder sb = new StringBuilder(what).append(" -> ").append(t);
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < Math.min(6, st.length); i++) sb.append("\n      at ").append(st[i]);
        if (t.getCause() != null) sb.append("\n    caused by ").append(t.getCause());
        return sb.toString();
    }

    @GameTest(template = "empty")
    public static void blocksPlace(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        int i = 0;
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            try {
                Block b = entry.get();
                BlockPos pos = new BlockPos(i % 7, 1, i / 7);
                helper.setBlock(pos.below(), Blocks.STONE);
                helper.setBlock(pos, b);
            } catch (Throwable t) {
                errors.add(err(entry.getId().toString(), t));
            }
            i++;
        }
        report(helper, "blocks(" + i + ")", errors);
    }

    @GameTest(template = "empty")
    public static void mobsSpawn(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        int i = 0;
        for (var entry : ModEntities.ENTITIES.getEntries()) {
            try {
                EntityType<?> type = entry.get();
                for (int x = 0; x < 8; x++) helper.setBlock(new BlockPos(x, 0, i), Blocks.STONE);
                var e = helper.spawn(type, new BlockPos(1 + i % 5, 1, i));
                if (e instanceof Mob m) {
                    m.setNoAi(true);
                    m.tick();
                }
            } catch (Throwable t) {
                errors.add(err(entry.getId().toString(), t));
            }
            i++;
        }
        report(helper, "mobs(" + i + ")", errors);
    }

    @GameTest(template = "empty", batch = "features", timeoutTicks = 200)
    public static void featuresGenerate(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 0, 0)).offset(300, 0, 300);
        var reg = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        int n = 0;
        for (var e : reg.entrySet()) {
            if (!e.getKey().location().getNamespace().equals(AetherWastes.MODID)) continue;
            BlockPos at = base.offset(n * 40, 0, 0);
            try {
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-12, -12, -12), at.offset(12, 0, 12))) {
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
                }
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-12, 1, -12), at.offset(12, 12, 12))) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, 0, -6), at.offset(6, 0, 6))) {
                    level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
                boolean ok1 = e.getValue().place(level, level.getChunkSource().getGenerator(), level.random, at.offset(0, -5, 0));
                boolean ok2 = e.getValue().place(level, level.getChunkSource().getGenerator(), level.random, at.above());
                AetherWastes.LOGGER.info("[aw-test] feature {} placed underground={} surface={}", e.getKey().location(), ok1, ok2);
            } catch (Throwable t) {
                errors.add(err(e.getKey().location().toString(), t));
            }
            n++;
        }
        report(helper, "features(" + n + ")", errors);
    }

    @GameTest(template = "empty", batch = "player", timeoutTicks = 200)
    public static void spellsAndSurvival(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ServerPlayer p;
        try {
            p = helper.makeMockServerPlayerInLevel();
        } catch (Throwable t) {
            errors.add(err("mock player", t));
            report(helper, "player", errors);
            return;
        }
        for (int x = -2; x <= 8; x++)
            for (int z = -2; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        p.teleportTo(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)).getX() + 0.5,
                helper.absolutePos(new BlockPos(3, 1, 3)).getY(), helper.absolutePos(new BlockPos(3, 1, 3)).getZ() + 0.5, 0, 0);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 6));
        zombie.setNoAi(true);

        PlayerData d = Data.get(p);
        d.archivist = true;
        d.era = 5;

        Glyph[] forms = {Glyph.TOUCH, Glyph.PROJECTILE, Glyph.SELF, Glyph.ZONE, Glyph.WAVE, Glyph.RUNE, Glyph.SUMMON};
        List<Glyph> essences = new ArrayList<>();
        for (Glyph g : Glyph.values()) if (g.type() == Glyph.Type.ESSENCE) essences.add(g);
        int casts = 0;
        for (Glyph form : forms) {
            for (int a = 0; a < essences.size(); a++) {
                for (int b = a; b < essences.size(); b++) {
                    List<Glyph> ess = a == b ? List.of(essences.get(a)) : List.of(essences.get(a), essences.get(b));
                    List<Glyph> mods = (casts % 3 == 0) ? List.of(Glyph.AMPLIFY, Glyph.CHAIN, Glyph.QUIET) : List.of(Glyph.EXPAND);
                    SpellData spell = new SpellData(form, ess, mods);
                    try {
                        PlayerStats.setVessel(p, 120f);
                        PlayerStats.setClarity(p, 100f);
                        p.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.SOUL_ESSENCE.get(), 4));
                        p.setYRot(0);
                        p.setXRot(0);
                        SpellCaster.cast(p, spell);
                    } catch (Throwable t) {
                        errors.add(err("cast " + form.id() + "+" + ess + "+" + mods, t));
                    }
                    casts++;
                }
            }
        }
        AetherWastes.LOGGER.info("[aw-test] {} spell combinations cast", casts);

        float pressure = EtherField.pressure(helper.getLevel(), p.blockPosition());
        Runnable[] ticks = {
                () -> Traumas.give(p, Traumas.FRACTURE), () -> Traumas.give(p, Traumas.BLEEDING), () -> Traumas.give(p, Traumas.BURN),
                () -> Traumas.tick(p, d), () -> Nutrition.tick(p, d), () -> Temperature.tick(p, d),
                () -> Mutations.gainRandom(p, d), () -> Mutations.tick(p, d, pressure),
                () -> Pulse.addNoise(p, 80f), () -> Pulse.tickPlayer(p, d, pressure, true),
                () -> Insights.add(p, "ether", "test"), () -> EraManager.check(p),
                () -> Scar.seedNear(helper.getLevel(), p.blockPosition(), 4), () -> Scar.cleanse(helper.getLevel(), p.blockPosition(), 4),
                () -> Wanderers.spawnNear(p, EraManager.SPARK, 6), () -> com.aetherwastes.network.Sync.all(p),
                () -> com.aetherwastes.item.JournalItem.show(p),
        };
        // --- снаряжение: каждый комплект брони и каждое оружие ---
        var sets = new net.neoforged.neoforge.registries.DeferredItem[][]{com.aetherwastes.registry.ModGear.ASHEN_SET,
                com.aetherwastes.registry.ModGear.ETHER_STEEL_SET, com.aetherwastes.registry.ModGear.PRISM_SET,
                com.aetherwastes.registry.ModGear.STAR_IRON_SET, com.aetherwastes.registry.ModGear.ARCHIVIST_SET,
                com.aetherwastes.registry.ModGear.PHANTOM_SET};
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (var set : sets) {
            try {
                for (int k = 0; k < 4; k++) p.setItemSlot(slots[k], new net.minecraft.world.item.ItemStack((net.minecraft.world.item.Item) set[k].get()));
                String name = com.aetherwastes.craft.ArmorSets.fullSet(p);
                if (name == null) errors.add("set not detected: " + set[0].getId());
                com.aetherwastes.craft.ArmorSets.tick(p);
                AetherWastes.LOGGER.info("[aw-test] armor set {} -> {}", set[0].getId(), name);
            } catch (Throwable t) {
                errors.add(err("armor " + set[0].getId(), t));
            }
        }
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (!(entry.get() instanceof com.aetherwastes.item.WastesWeapon weapon)) continue;
            try {
                var stack = new net.minecraft.world.item.ItemStack(weapon);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                PlayerStats.setVessel(p, 120f);
                p.getCooldowns().removeCooldown(weapon);
                weapon.use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
                var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 4));
                target.setNoAi(true);
                weapon.hurtEnemy(stack, target, p);
                weapon.damageMultiplier(p, target);
                p.attack(target);
            } catch (Throwable t) {
                errors.add(err("weapon " + entry.getId(), t));
            }
        }
        for (int k = 0; k < ticks.length; k++) {
            try {
                ticks[k].run();
            } catch (Throwable t) {
                errors.add(err("survival step " + k, t));
            }
        }
        report(helper, "player(" + casts + " casts)", errors);
    }

    /** Призрачный шаг: полный комплект, активация, полёт сквозь стену, перезарядка, безопасный выход. */
    @GameTest(template = "empty", batch = "phase", timeoutTicks = 400)
    public static void phantomPhase(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ServerPlayer p;
        try {
            p = helper.makeMockServerPlayerInLevel();
        } catch (Throwable t) {
            errors.add(err("mock player", t));
            report(helper, "phase", errors);
            return;
        }
        try {
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for (int x = -2; x <= 8; x++)
                for (int z = -2; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            // Стена 3 блока толщиной
            for (int x = -2; x <= 8; x++)
                for (int y = 1; y <= 4; y++)
                    for (int z = 4; z <= 6; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            BlockPos start = helper.absolutePos(new BlockPos(3, 1, 2));
            p.teleportTo(helper.getLevel(), start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
            var set = com.aetherwastes.registry.ModGear.PHANTOM_SET;
            net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET};
            com.aetherwastes.ability.Phase.request(p);
            if (Data.get(p).phaseTicks > 0) errors.add("phase started without the set");
            for (int k = 0; k < 4; k++) p.setItemSlot(slots[k], new net.minecraft.world.item.ItemStack(set[k].get()));
            if (!com.aetherwastes.craft.ArmorSets.wearing(p, com.aetherwastes.craft.ArmorSets.PHANTOM)) errors.add("set not detected");
            com.aetherwastes.ability.Phase.request(p);
            PlayerData d = Data.get(p);
            if (d.phaseTicks != com.aetherwastes.ability.Phase.DURATION) errors.add("phase did not start: " + d.phaseTicks);
            if (!p.getAbilities().flying) errors.add("not flying");
            // Движение сквозь стену: move() не должен упираться
            double z0 = p.getZ();
            p.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, 0, 3.0));
            if (p.getZ() - z0 < 2.9) errors.add("blocked by wall: moved " + (p.getZ() - z0));
            if (!com.aetherwastes.ability.Phase.isPhasing(p)) errors.add("isPhasing false");
            // Конец фазы внутри стены — должен вытолкнуть в свободное место
            com.aetherwastes.ability.Phase.end(p, d);
            if (!helper.getLevel().noCollision(p, p.getBoundingBox())) errors.add("still stuck in wall after eject at " + p.blockPosition());
            if (d.phaseCooldown != com.aetherwastes.ability.Phase.COOLDOWN) errors.add("cooldown " + d.phaseCooldown);
            com.aetherwastes.ability.Phase.request(p);
            if (!p.isCreative() && d.phaseTicks > 0) errors.add("phase restarted during cooldown");
            AetherWastes.LOGGER.info("[aw-test] phase: ejected to {}", p.blockPosition());
        } catch (Throwable t) {
            errors.add(err("phase", t));
        }
        report(helper, "phase", errors);
    }

    /** Все три подземелья строятся целиком, в зале босса есть Печать, и она пробуждает хозяина. */
    @GameTest(template = "empty", batch = "dungeons", timeoutTicks = 1200)
    public static void dungeonsBuild(GameTestHelper helper) {
        List<String> errors = new ArrayList<>();
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 0, 0)).offset(1200, 0, 1200);
        int n = 0;
        int totalPieces = 0;
        for (var kind : com.aetherwastes.world.dungeon.DungeonKind.values()) {
            int ox = base.getX() + n * 200, oz = base.getZ();
            int y0 = kind.surface ? base.getY() + 4 : base.getY() - 30;
            if (y0 < level.getMinBuildHeight() + 8) y0 = level.getMinBuildHeight() + 8;
            int fy0 = y0;
            try {
                var pieces = com.aetherwastes.world.dungeon.DungeonStructure.layout(kind,
                        net.minecraft.util.RandomSource.create(42 + n), ox, y0, oz, (x, z) -> fy0 + 40);
                totalPieces += pieces.size();
                var all = net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingBoxes(
                        pieces.stream().map(net.minecraft.world.level.levelgen.structure.StructurePiece::getBoundingBox).toList()).orElseThrow();
                int seals = 0;
                BlockPos sealPos = null;
                for (int cx = all.minX() >> 4; cx <= all.maxX() >> 4; cx++) {
                    for (int cz = all.minZ() >> 4; cz <= all.maxZ() >> 4; cz++) {
                        var cp = new net.minecraft.world.level.ChunkPos(cx, cz);
                        var chunkBox = new net.minecraft.world.level.levelgen.structure.BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(),
                                cp.getMinBlockZ(), cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ());
                        for (var piece : pieces) {
                            if (!piece.getBoundingBox().intersects(chunkBox)) continue;
                            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                                    chunkBox, cp, BlockPos.ZERO);
                        }
                    }
                }
                for (var piece : pieces) {
                    if (piece.role() != com.aetherwastes.world.dungeon.DungeonPiece.Role.BOSS) continue;
                    var bb = piece.getBoundingBox();
                    for (BlockPos p : BlockPos.betweenClosed(bb.minX(), bb.minY(), bb.minZ(), bb.maxX(), bb.minY() + 3, bb.maxZ())) {
                        if (level.getBlockState(p).is(ModBlocks.GUARDIAN_SEAL.get())) {
                            seals++;
                            sealPos = p.immutable();
                        }
                    }
                }
                if (seals != 1) errors.add(kind + ": seals=" + seals);
                if (sealPos != null) {
                    var boss = com.aetherwastes.block.GuardianSealBlockEntity.awaken(level, sealPos, level.getBlockState(sealPos));
                    if (boss == null) errors.add(kind + ": boss not spawned");
                    else {
                        boss.setNoAi(false);
                        for (int t = 0; t < 40; t++) boss.tick();
                        AetherWastes.LOGGER.info("[aw-test] dungeon {}: {} pieces, boss {} at {}", kind, pieces.size(),
                                boss.getType().toShortString(), boss.blockPosition());
                        boss.discard();
                    }
                }
            } catch (Throwable t) {
                errors.add(err("dungeon " + kind, t));
            }
            n++;
        }
        report(helper, "dungeons(" + totalPieces + " pieces)", errors);
    }
}
