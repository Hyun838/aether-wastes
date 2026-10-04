package com.aetherwastes.progression;

import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/** Текущая цель игрока для строки на экране: подсказывает, что делать дальше. */
public final class Objectives {
    private Objectives() {}

    public static void write(ServerPlayer p, PlayerData d, CompoundTag out) {
        String key;
        String[] args = {};
        if (!d.flags.contains("compass")) key = "compass";
        else if (!d.flags.contains("anchor")) key = "anchor";
        else if (d.schools.isEmpty() && !d.archivist) key = "school";
        else if (!d.flags.contains("inscribed")) key = "inscribe";
        else if (!d.flags.contains("cast")) key = "cast";
        else if (!d.flags.contains("forged")) key = "forge";
        else if (!d.flags.contains("armor_set")) key = "armor";
        else {
            switch (d.era) {
                case 1 -> key = "gate1";
                case 2 -> {
                    key = d.wandererKills.contains(EraManager.SPARK) ? "gate2_insights" : "gate2_spark";
                    args = new String[]{String.valueOf(Math.min(15, d.insights.size()))};
                }
                case 3 -> key = d.wandererKills.contains(EraManager.RESONANCE) ? "gate3_underside" : "gate3_resonance";
                case 4 -> {
                    key = d.wandererKills.contains(EraManager.CONVERGENCE) ? "gate4_circle" : "gate4_convergence";
                    args = new String[]{String.valueOf(EraManager.maxCircle(d))};
                }
                default -> key = WorldState.get(p.getServer()).ended() ? "free" : "heart";
            }
        }
        out.putString("objective", "objective.aetherwastes." + key);
        ListTag list = new ListTag();
        for (String a : args) list.add(StringTag.valueOf(a));
        out.put("objArgs", list);
    }
}
