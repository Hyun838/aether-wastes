package com.aetherwastes.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(SyncStatsPayload.TYPE, SyncStatsPayload.STREAM_CODEC, ModNetwork::handleStats);
        registrar.playToClient(SyncJournalPayload.TYPE, SyncJournalPayload.STREAM_CODEC, ModNetwork::handleJournal);
    }

    private static void handleStats(SyncStatsPayload p, IPayloadContext context) {
        ClientStatsCache.vessel = p.vessel();
        ClientStatsCache.clarity = p.clarity();
        ClientStatsCache.pressure = p.pressure();
        ClientStatsCache.temperature = p.temperature();
        ClientStatsCache.noise = p.noise();
        ClientStatsCache.era = p.era();
        ClientStatsCache.tide = p.tide();
        ClientStatsCache.received = true;
    }

    private static void handleJournal(SyncJournalPayload p, IPayloadContext context) {
        ClientStatsCache.journal = p.journal();
    }
}
