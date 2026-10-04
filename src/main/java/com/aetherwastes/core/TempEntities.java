package com.aetherwastes.core;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Временные существа (духи, Эхо, мигранты) исчезают сами — даже после перезахода в мир. */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class TempEntities {
    public static final String TAG = "aw_temp";
    private static final String EXPIRE = "aw_expire";

    private TempEntities() {}

    public static void mark(Entity e, int lifeTicks) {
        e.addTag(TAG);
        e.getPersistentData().putLong(EXPIRE, e.level().getGameTime() + lifeTicks);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.tickCount % 20 != 0 || !(e.level() instanceof ServerLevel level)) return;
        if (!e.getTags().contains(TAG)) return;
        if (level.getGameTime() >= e.getPersistentData().getLong(EXPIRE)) {
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
            e.discard();
        }
    }
}
