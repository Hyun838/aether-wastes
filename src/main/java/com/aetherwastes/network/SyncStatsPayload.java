package com.aetherwastes.network;

import com.aetherwastes.AetherWastes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Сервер → клиент раз в секунду: Сосуд, Ясность, давление, температура, Шум.
 * meta = эпоха × 2 + (Прилив ? 1 : 0).
 */
public record SyncStatsPayload(float vessel, float clarity, float pressure, float temperature, float noise,
                               int meta) implements CustomPacketPayload {
    public static final Type<SyncStatsPayload> TYPE = new Type<>(AetherWastes.id("sync_stats"));

    public static final StreamCodec<ByteBuf, SyncStatsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncStatsPayload::vessel,
            ByteBufCodecs.FLOAT, SyncStatsPayload::clarity,
            ByteBufCodecs.FLOAT, SyncStatsPayload::pressure,
            ByteBufCodecs.FLOAT, SyncStatsPayload::temperature,
            ByteBufCodecs.FLOAT, SyncStatsPayload::noise,
            ByteBufCodecs.VAR_INT, SyncStatsPayload::meta,
            SyncStatsPayload::new);

    public static SyncStatsPayload of(float vessel, float clarity, float pressure, float temperature, float noise,
                                      int era, boolean tide) {
        return new SyncStatsPayload(vessel, clarity, pressure, temperature, noise, era * 2 + (tide ? 1 : 0));
    }

    public int era() {
        return meta / 2;
    }

    public boolean tide() {
        return (meta & 1) == 1;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
