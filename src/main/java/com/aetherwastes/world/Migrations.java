package com.aetherwastes.world;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.TempEntities;
import com.aetherwastes.core.WorldState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Сезонные миграции: раз в 3 дня через окрестности игроков проходят стада, а в поздних эпохах —
 * и стаи хищников или мертвецов. Их можно обойти, а можно устроить засаду ради ресурсов.
 */
public final class Migrations {
    private Migrations() {}

    public static String season(long day) {
        return switch ((int) ((day / 8) % 4)) {
            case 0 -> "spring";
            case 1 -> "summer";
            case 2 -> "autumn";
            default -> "winter";
        };
    }

    public static void daily(ServerLevel level, long day) {
        if (!AetherConfig.MIGRATIONS.get()) return;
        WorldState ws = WorldState.get(level.getServer());
        if (day < ws.nextMigrationDay) return;
        ws.nextMigrationDay = day + 3;
        ws.setDirty();
        String season = season(day);
        for (ServerPlayer p : level.players()) {
            if (level.random.nextFloat() > 0.6f) continue;
            int era = Data.get(p).era;
            boolean predators = era >= 3 && level.random.nextFloat() < 0.4f;
            EntityType<? extends Mob> type = predators
                    ? (season.equals("winter") ? EntityType.STRAY : EntityType.ZOMBIE)
                    : switch (season) {
                        case "spring" -> EntityType.SHEEP;
                        case "summer" -> EntityType.HORSE;
                        case "autumn" -> EntityType.GOAT;
                        default -> EntityType.WOLF;
                    };
            herd(level, p, type, 5 + level.random.nextInt(4), predators);
            Msg.chat(p, ChatFormatting.YELLOW, predators ? "migration.aetherwastes.predators" : "migration.aetherwastes.herd",
                    Component.translatable("season.aetherwastes." + season));
        }
    }

    private static void herd(ServerLevel level, ServerPlayer p, EntityType<? extends Mob> type, int count, boolean hostile) {
        double a = level.random.nextDouble() * Math.PI * 2;
        int sx = (int) (p.getX() + Math.cos(a) * 40), sz = (int) (p.getZ() + Math.sin(a) * 40);
        int tx = (int) (p.getX() - Math.cos(a) * 40), tz = (int) (p.getZ() - Math.sin(a) * 40);
        for (int i = 0; i < count; i++) {
            int x = sx + level.random.nextInt(7) - 3, z = sz + level.random.nextInt(7) - 3;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Mob m = type.create(level);
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null);
            m.addTag("aw_spawned");
            m.addTag("aw_migrant");
            TempEntities.mark(m, 20 * 60 * 6);
            level.addFreshEntity(m);
            if (hostile) m.setTarget(p);
            else m.getNavigation().moveTo(tx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz), tz, 1.1);
        }
    }
}
