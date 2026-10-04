package com.aetherwastes.world;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.base.Settlers;
import com.aetherwastes.base.Wards;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.ether.EtherField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;

/** Мировые часы: суточные события, обереги, Разломы, распад построек в Изнанке, Сердце Раскола. */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class WorldEvents {
    private WorldEvents() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long t = level.getGameTime();
        if (t % 40 == 0) Wards.tick(level);
        if (level.dimension() != Level.OVERWORLD) return;

        WorldState ws = WorldState.get(level.getServer());
        long day = level.getDayTime() / 24000L;
        if (t % 20 == 0 && day != ws.lastDay) {
            ws.lastDay = day;
            ws.setDirty();
            Migrations.daily(level, day);
            Settlers.daily(level);
        }
        if (t % 600 == 0) Rifts.expire(level.getServer());
        if (t % 100 == 0) decay(level, ws);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 40 == 0) HeartManager.tick(p);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AetherConfig.UNDERSIDE_DECAY.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !Dims.isUnderside(level)) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (EtherField.inAnchor(level, event.getPos())) return;
        CompoundTag e = new CompoundTag();
        e.putLong("pos", event.getPos().asLong());
        e.putLong("time", level.getGameTime());
        WorldState ws = WorldState.get(level.getServer());
        ws.undersideDecay.add(e);
        ws.setDirty();
    }

    /** Постройки в Изнанке без Якоря растворяются через ~10 минут. */
    private static void decay(ServerLevel overworld, WorldState ws) {
        if (ws.undersideDecay.isEmpty()) return;
        ServerLevel u = overworld.getServer().getLevel(Dims.UNDERSIDE);
        if (u == null) return;
        long now = u.getGameTime();
        int processed = 0;
        Iterator<CompoundTag> it = ws.undersideDecay.iterator();
        while (it.hasNext() && processed < 30) {
            CompoundTag e = it.next();
            if (now - e.getLong("time") < 12000L) break;
            BlockPos pos = BlockPos.of(e.getLong("pos"));
            processed++;
            it.remove();
            if (!u.isLoaded(pos) || EtherField.inAnchor(u, pos)) continue;
            if (!u.getBlockState(pos).isAir()) {
                u.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                u.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
            }
        }
        if (processed > 0) ws.setDirty();
    }
}
