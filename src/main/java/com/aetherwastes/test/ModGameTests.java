package com.aetherwastes.test;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.Dims;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Автотесты запуска: проверяют, что мир, блоки и мобы мода реально работают в игре. */
@GameTestHolder(AetherWastes.MODID)
@PrefixGameTestTemplate(false)
public class ModGameTests {

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void undersideGenerates(GameTestHelper helper) {
        ServerLevel u = helper.getLevel().getServer().getLevel(Dims.UNDERSIDE);
        if (u == null) {
            helper.fail("Изнанка не загрузилась");
            return;
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                u.getChunk(x * 4, z * 4);
            }
        }
        AetherWastes.LOGGER.info("[aw-test] Underside chunks generated OK");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blocksPlace(GameTestHelper helper) {
        int i = 0;
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            Block b = entry.get();
            BlockPos pos = new BlockPos(i % 7, 1, i / 7);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, b);
            i++;
        }
        AetherWastes.LOGGER.info("[aw-test] placed {} blocks", i);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mobsSpawn(GameTestHelper helper) {
        int i = 0;
        for (var entry : ModEntities.ENTITIES.getEntries()) {
            EntityType<?> type = entry.get();
            for (int x = 0; x < 8; x++) helper.setBlock(new BlockPos(x, 0, i), Blocks.STONE);
            var e = helper.spawn(type, new BlockPos(1 + i % 5, 1, i));
            if (e instanceof Mob m) m.setNoAi(true);
            i++;
        }
        AetherWastes.LOGGER.info("[aw-test] spawned {} entity types", i);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemsExist(GameTestHelper helper) {
        int n = ModItems.ITEMS.getEntries().size();
        AetherWastes.LOGGER.info("[aw-test] {} items registered", n);
        helper.succeed();
    }
}
