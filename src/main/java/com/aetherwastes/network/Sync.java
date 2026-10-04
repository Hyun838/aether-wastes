package com.aetherwastes.network;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.craft.Alchemy;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.progression.School;
import com.aetherwastes.survival.PlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;

/** Отправка состояния игроку. */
public final class Sync {
    private Sync() {}

    public static void all(ServerPlayer p) {
        stats(p);
        journal(p);
    }

    public static void stats(ServerPlayer p) {
        PlayerData d = Data.get(p);
        float pressure = EtherField.pressure(p.serverLevel(), p.blockPosition());
        send(p, SyncStatsPayload.of(PlayerStats.vessel(p), PlayerStats.clarity(p),
                pressure, d.bodyTemp, d.noise, d.era, EtherField.isTide(p.level())));
    }

    public static void journal(ServerPlayer p) {
        send(p, new SyncJournalPayload(build(p)));
    }

    /** Отправить, только если клиент знает этот канал (защита от клиентов без мода и тестовых игроков). */
    private static void send(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        try {
            if (p.connection == null || !p.connection.hasChannel(payload.type())) return;
            PacketDistributor.sendToPlayer(p, payload);
        } catch (RuntimeException e) {
            // Клиент не готов принять пакет — пропускаем, следующий придёт через секунду.
        }
    }

    public static CompoundTag build(ServerPlayer p) {
        PlayerData d = Data.get(p);
        CompoundTag t = new CompoundTag();
        t.putInt("era", d.era);
        t.put("schools", list(d.schools));
        CompoundTag circles = new CompoundTag();
        for (School s : School.values()) {
            if (d.knows(s.id())) circles.putInt(s.id(), d.circle(s.id()));
        }
        t.put("circles", circles);
        CompoundTag ins = new CompoundTag();
        for (String theme : Insights.THEMES) ins.putInt(theme, d.insightCount(theme));
        t.put("insights", ins);
        t.putInt("insightTotal", d.insights.size());
        CompoundTag herbs = new CompoundTag();
        long seed = p.serverLevel().getServer().overworld().getSeed();
        for (String herb : d.knownHerbs) herbs.putString(herb, Alchemy.effectOf(seed, herb));
        t.put("herbs", herbs);
        CompoundTag metals = new CompoundTag();
        for (String metal : Traits.METALS) metals.put(metal, list(Traits.traitsOf(seed, metal)));
        t.put("metals", metals);
        t.put("mutations", list(d.mutations));
        t.put("traumas", list(d.traumas.keySet()));
        t.put("wanderers", list(d.wandererKills));
        t.putString("ending", WorldState.get(p.getServer()).ending);
        t.putBoolean("archivist", d.archivist);
        t.putBoolean("reconciled", d.reconciled);
        t.putInt("secondAge", d.secondAge);
        t.putString("nemesis", d.nemesis.getString("name"));
        return t;
    }

    private static ListTag list(Collection<String> values) {
        ListTag l = new ListTag();
        for (String v : values) l.add(StringTag.valueOf(v));
        return l;
    }
}
