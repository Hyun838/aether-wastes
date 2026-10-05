package com.aetherwastes.network;

import com.aetherwastes.AetherWastes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: игрок нажал клавишу способности (0 — Призрачный шаг). */
public record AbilityRequestPayload(int ability) implements CustomPacketPayload {
    public static final Type<AbilityRequestPayload> TYPE = new Type<>(AetherWastes.id("ability"));
    public static final StreamCodec<ByteBuf, AbilityRequestPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(AbilityRequestPayload::new, AbilityRequestPayload::ability);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
