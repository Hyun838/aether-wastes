package com.aetherwastes.threats;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.ether.EtherField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Осада Якоря: если у базы долго шумно (Шум от 60 около 3 минут), ночью приходит волна,
 * подобранная против обороны: летуны против высоких стен, пауки и криперы против закрытых баз.
 */
public final class Sieges {
    private Sieges() {}

    public static void tick(ServerPlayer p, PlayerData d) {
        if (!AetherConfig.SIEGES.get()) return;
        ServerLevel level = p.serverLevel();
        if (WorldState.ENDING_HEAL.equals(WorldState.get(p.getServer()).ending)) return;
        EtherField.Anchor anchor = EtherField.strongestAnchor(level, p.blockPosition());
        if (anchor != null && d.noise >= 60f) d.siegeMeter += 1f;
        else d.siegeMeter = Math.max(0f, d.siegeMeter - 0.2f);

        long now = level.getGameTime();
        boolean absorb = WorldState.ENDING_ABSORB.equals(WorldState.get(p.getServer()).ending);
        long cooldown = absorb ? 12000L : 24000L;
        if (anchor == null || d.siegeMeter < 180f || level.isDay() || now - d.lastSiege < cooldown) return;
        d.siegeMeter = 0f;
        d.lastSiege = now;
        start(p, d, anchor, absorb);
    }

    public static void start(ServerPlayer p, PlayerData d, EtherField.Anchor anchor, boolean absorb) {
        ServerLevel level = p.serverLevel();
        BlockPos center = anchor.pos();
        int count = 6 + d.era * 2 + (absorb ? 3 : 0);
        boolean high = p.getY() > center.getY() + 8;
        boolean enclosed = !level.canSeeSky(center.above(2));

        List<EntityType<? extends Mob>> pool = new ArrayList<>();
        if (high) pool.add(EntityType.PHANTOM);
        if (enclosed) {
            pool.add(EntityType.SPIDER);
            pool.add(EntityType.CREEPER);
        } else {
            pool.add(EntityType.ZOMBIE);
            pool.add(EntityType.SKELETON);
        }
        if (d.era >= 3) pool.add(EntityType.VINDICATOR);
        if (d.era >= 4) pool.add(EntityType.WITCH);

        double dist = EtherField.anchorRadius(anchor.tier()) * 0.85;
        int spawned = 0;
        for (int i = 0; i < count * 3 && spawned < count; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            int x = (int) (center.getX() + Math.cos(a) * dist);
            int z = (int) (center.getZ() + Math.sin(a) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            EntityType<? extends Mob> type = pool.get(level.random.nextInt(pool.size()));
            if (type == EntityType.PHANTOM) y += 20;
            Mob mob = type.create(level);
            if (mob == null) continue;
            mob.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null);
            mob.addTag("aw_spawned");
            mob.addTag("aw_siege");
            mob.setTarget(p);
            level.addFreshEntity(mob);
            spawned++;
        }
        Msg.title(p, Component.translatable("siege.aetherwastes.title").withStyle(ChatFormatting.DARK_RED),
                Component.translatable("siege.aetherwastes.subtitle", spawned));
        level.playSound(null, center, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5f, 0.6f);
    }
}
