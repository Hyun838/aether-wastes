package com.aetherwastes.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(SyncStatsPayload.TYPE, SyncStatsPayload.STREAM_CODEC, ModNetwork::handleStats);
        registrar.playToClient(SyncJournalPayload.TYPE, SyncJournalPayload.STREAM_CODEC, ModNetwork::handleJournal);
        registrar.playToClient(PhaseStatePayload.TYPE, PhaseStatePayload.STREAM_CODEC, ModNetwork::handlePhase);
        registrar.playToServer(AbilityRequestPayload.TYPE, AbilityRequestPayload.STREAM_CODEC, ModNetwork::handleAbility);
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

    private static void handlePhase(PhaseStatePayload p, IPayloadContext context) {
        ClientStatsCache.phaseTicks = p.ticks();
        ClientStatsCache.phaseActive = p.ticks() > 0;
        ClientStatsCache.phaseCooldown = p.cooldown();
    }

    private static void handleAbility(AbilityRequestPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp && p.ability() == 0) {
                com.aetherwastes.ability.Phase.request(sp);
            }
        });
    }
}
