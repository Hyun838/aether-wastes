package com.aetherwastes.network;

import com.aetherwastes.AetherWastes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: сколько осталось Призрачного шага и перезарядки (в тиках). */
public record PhaseStatePayload(int ticks, int cooldown) implements CustomPacketPayload {
    public static final Type<PhaseStatePayload> TYPE = new Type<>(AetherWastes.id("phase_state"));
    public static final StreamCodec<ByteBuf, PhaseStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhaseStatePayload::ticks,
            ByteBufCodecs.VAR_INT, PhaseStatePayload::cooldown,
            PhaseStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
