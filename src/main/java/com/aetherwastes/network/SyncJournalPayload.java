package com.aetherwastes.network;

import com.aetherwastes.AetherWastes;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: Журнал (школы, Озарения, известные травы, свойства металлов мира и т. д.). */
public record SyncJournalPayload(CompoundTag journal) implements CustomPacketPayload {
    public static final Type<SyncJournalPayload> TYPE = new Type<>(AetherWastes.id("sync_journal"));

    public static final StreamCodec<ByteBuf, SyncJournalPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncJournalPayload::journal,
            SyncJournalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
