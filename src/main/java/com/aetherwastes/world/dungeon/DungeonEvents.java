package com.aetherwastes.world.dungeon;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.registry.ModSounds;
import com.aetherwastes.registry.ModStructures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Атмосфера подземелий: гул и шёпот, пока игрок внутри. */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class DungeonEvents {
    private DungeonEvents() {}

    public static boolean inDungeon(ServerPlayer p) {
        return p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(), ModStructures.DUNGEONS).isValid();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 220 != 37) return;
        if (!inDungeon(p)) return;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                ModSounds.DUNGEON_AMBIENT, SoundSource.AMBIENT,
                p.getX(), p.getY(), p.getZ(), 0.8f, 0.9f + p.getRandom().nextFloat() * 0.2f, p.getRandom().nextLong()));
    }
}
