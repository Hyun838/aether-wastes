package com.aetherwastes.base;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.ether.AnchorData;
import com.aetherwastes.ether.EtherField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * Поселенцы приходят к Якорю 3-го уровня и выше: кузнец, травник, разведчик, фермер, страж.
 * У каждого имя и черта характера. Во время Осады они могут погибнуть навсегда.
 */
public final class Settlers {
    public static final String TAG = "aw_settler";
    private static final String[] NAMES = {"Ольга", "Тимофей", "Агата", "Радим", "Злата", "Мирон", "Веста", "Ярослав",
            "Ирма", "Богдан", "Лада", "Савва"};
    private static final String[] TRAITS = {"ворчливый", "смешливая", "молчаливый", "любопытная", "суеверный",
            "храбрая", "осторожный", "мечтательная"};

    private Settlers() {}

    /** Раз в игровые сутки. */
    public static void daily(ServerLevel level) {
        if (!AetherConfig.SETTLERS.get()) return;
        for (Map.Entry<Long, Integer> a : AnchorData.get(level).all().entrySet()) {
            int tier = a.getValue();
            if (tier < 3) continue;
            BlockPos c = BlockPos.of(a.getKey());
            if (!level.isLoaded(c) || level.random.nextFloat() > 0.5f) continue;
            int r = EtherField.anchorRadius(tier);
            int existing = level.getEntitiesOfClass(Villager.class, new AABB(c).inflate(r), v -> v.getTags().contains(TAG)).size();
            int cap = (tier - 2) * 2;
            if (existing >= cap) continue;
            spawn(level, c);
            if (tier >= 5 && level.random.nextFloat() < 0.3f) spawnGuard(level, c);
        }
    }

    private static BlockPos near(ServerLevel level, BlockPos c) {
        int x = c.getX() + level.random.nextInt(9) - 4;
        int z = c.getZ() + level.random.nextInt(9) - 4;
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static void spawn(ServerLevel level, BlockPos anchor) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) return;
        BlockPos pos = near(level, anchor);
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        v.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        VillagerProfession[] profs = {VillagerProfession.ARMORER, VillagerProfession.CLERIC, VillagerProfession.CARTOGRAPHER,
                VillagerProfession.FARMER, VillagerProfession.TOOLSMITH};
        String[] roles = {"smith", "herbalist", "scout", "farmer", "toolsmith"};
        int i = level.random.nextInt(profs.length);
        v.setVillagerData(v.getVillagerData().setProfession(profs[i]));
        v.setVillagerXp(1);
        String name = NAMES[level.random.nextInt(NAMES.length)];
        String trait = TRAITS[level.random.nextInt(TRAITS.length)];
        v.setCustomName(Component.translatable("settler.aetherwastes.name", name,
                Component.translatable("settler.aetherwastes.role." + roles[i]), trait).withStyle(ChatFormatting.GREEN));
        v.addTag(TAG);
        v.addTag("aw_spawned");
        v.setPersistenceRequired();
        level.addFreshEntity(v);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(anchor).inflate(96))) {
            p.displayClientMessage(Component.translatable("settler.aetherwastes.arrived", name).withStyle(ChatFormatting.GREEN), false);
        }
    }

    private static void spawnGuard(ServerLevel level, BlockPos anchor) {
        IronGolem g = EntityType.IRON_GOLEM.create(level);
        if (g == null) return;
        BlockPos pos = near(level, anchor);
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        g.setPlayerCreated(true);
        g.setCustomName(Component.translatable("settler.aetherwastes.guard").withStyle(ChatFormatting.GREEN));
        g.addTag("aw_spawned");
        level.addFreshEntity(g);
    }
}
