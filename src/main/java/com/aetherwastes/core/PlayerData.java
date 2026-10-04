package com.aetherwastes.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Всё, что мод помнит об игроке: эпоха, школы, Озарения, травмы, мутации,
 * Шум, Немезида и т. д. Сохраняется в мир и переживает смерть.
 */
public class PlayerData implements INBTSerializable<CompoundTag> {
    // --- Прогрессия ---
    public int era = 1;
    public boolean wasInTide = false;
    public final Set<String> insights = new HashSet<>();       // "тема:ключ"
    public final Set<String> rewardsGiven = new HashSet<>();   // "тема:порог"
    public final Set<String> schools = new HashSet<>();
    public final Map<String, Integer> masteryXp = new HashMap<>();
    public final Set<String> wandererKills = new HashSet<>();
    public final Set<String> knownHerbs = new HashSet<>();
    public boolean reconciled = false;
    public boolean archivist = false;
    public boolean visitedUnderside = false;
    public int secondAge = 0;

    // --- Выживание ---
    public final Set<String> mutations = new HashSet<>();
    public float exposure = 0f;
    public final Map<String, Long> traumas = new HashMap<>();  // травма → когда получена
    public final List<String> foodHistory = new ArrayList<>();
    public float bodyTemp = 50f;
    public long warmUntil = 0L;
    public long silencedUntil = 0L;

    // --- Угрозы ---
    public float noise = 0f;
    public float siegeMeter = 0f;
    public long lastSiege = -100000L;
    public final Map<String, Integer> killTypes = new HashMap<>();
    public final Map<String, Integer> killCats = new HashMap<>();
    public int killsSinceHunter = 0;
    public long lastHunter = -100000L;
    public CompoundTag nemesis = new CompoundTag();
    public long lastNemesisReturn = 0L;
    public long lastWanderer = -100000L;
    public long lastRiftTp = 0L;

    // --- Перемещения ---
    public CompoundTag returnMark = new CompoundTag();
    public CompoundTag riftReturn = new CompoundTag();

    public boolean knows(String school) {
        return archivist || schools.contains(school);
    }

    public int insightCount(String theme) {
        int n = 0;
        String prefix = theme + ":";
        for (String s : insights) if (s.startsWith(prefix)) n++;
        return n;
    }

    public int circle(String school) {
        int xp = masteryXp.getOrDefault(school, 0);
        // Круги: 1 — сразу, 2 — 100, 3 — 400, 4 — 900, 5 — 1600 опыта.
        int c = 1;
        while (c < 5 && xp >= 100 * c * c) c++;
        return c;
    }

    public boolean hasTrauma(String t) {
        return traumas.containsKey(t);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putInt("era", era);
        t.putBoolean("wasInTide", wasInTide);
        t.put("insights", writeSet(insights));
        t.put("rewards", writeSet(rewardsGiven));
        t.put("schools", writeSet(schools));
        t.put("mastery", writeIntMap(masteryXp));
        t.put("wanderers", writeSet(wandererKills));
        t.put("herbs", writeSet(knownHerbs));
        t.putBoolean("reconciled", reconciled);
        t.putBoolean("archivist", archivist);
        t.putBoolean("visitedUnderside", visitedUnderside);
        t.putInt("secondAge", secondAge);

        t.put("mutations", writeSet(mutations));
        t.putFloat("exposure", exposure);
        CompoundTag tr = new CompoundTag();
        traumas.forEach(tr::putLong);
        t.put("traumas", tr);
        ListTag food = new ListTag();
        for (String f : foodHistory) food.add(StringTag.valueOf(f));
        t.put("food", food);
        t.putFloat("bodyTemp", bodyTemp);
        t.putLong("warmUntil", warmUntil);
        t.putLong("silencedUntil", silencedUntil);

        t.putFloat("noise", noise);
        t.putFloat("siegeMeter", siegeMeter);
        t.putLong("lastSiege", lastSiege);
        t.put("killTypes", writeIntMap(killTypes));
        t.put("killCats", writeIntMap(killCats));
        t.putInt("killsSinceHunter", killsSinceHunter);
        t.putLong("lastHunter", lastHunter);
        t.put("nemesis", nemesis.copy());
        t.putLong("lastNemesisReturn", lastNemesisReturn);
        t.putLong("lastWanderer", lastWanderer);
        t.putLong("lastRiftTp", lastRiftTp);
        t.put("returnMark", returnMark.copy());
        t.put("riftReturn", riftReturn.copy());
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        era = Math.max(1, t.getInt("era"));
        wasInTide = t.getBoolean("wasInTide");
        readSet(t, "insights", insights);
        readSet(t, "rewards", rewardsGiven);
        readSet(t, "schools", schools);
        readIntMap(t, "mastery", masteryXp);
        readSet(t, "wanderers", wandererKills);
        readSet(t, "herbs", knownHerbs);
        reconciled = t.getBoolean("reconciled");
        archivist = t.getBoolean("archivist");
        visitedUnderside = t.getBoolean("visitedUnderside");
        secondAge = t.getInt("secondAge");

        readSet(t, "mutations", mutations);
        exposure = t.getFloat("exposure");
        traumas.clear();
        CompoundTag tr = t.getCompound("traumas");
        for (String k : tr.getAllKeys()) traumas.put(k, tr.getLong(k));
        foodHistory.clear();
        ListTag food = t.getList("food", Tag.TAG_STRING);
        for (int i = 0; i < food.size(); i++) foodHistory.add(food.getString(i));
        bodyTemp = t.contains("bodyTemp") ? t.getFloat("bodyTemp") : 50f;
        warmUntil = t.getLong("warmUntil");
        silencedUntil = t.getLong("silencedUntil");

        noise = t.getFloat("noise");
        siegeMeter = t.getFloat("siegeMeter");
        lastSiege = t.contains("lastSiege") ? t.getLong("lastSiege") : -100000L;
        readIntMap(t, "killTypes", killTypes);
        readIntMap(t, "killCats", killCats);
        killsSinceHunter = t.getInt("killsSinceHunter");
        lastHunter = t.contains("lastHunter") ? t.getLong("lastHunter") : -100000L;
        nemesis = t.getCompound("nemesis");
        lastNemesisReturn = t.getLong("lastNemesisReturn");
        lastWanderer = t.contains("lastWanderer") ? t.getLong("lastWanderer") : -100000L;
        lastRiftTp = t.getLong("lastRiftTp");
        returnMark = t.getCompound("returnMark");
        riftReturn = t.getCompound("riftReturn");
    }

    private static ListTag writeSet(Set<String> set) {
        ListTag list = new ListTag();
        for (String s : set) list.add(StringTag.valueOf(s));
        return list;
    }

    private static void readSet(CompoundTag t, String key, Set<String> into) {
        into.clear();
        ListTag list = t.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) into.add(list.getString(i));
    }

    private static CompoundTag writeIntMap(Map<String, Integer> map) {
        CompoundTag c = new CompoundTag();
        map.forEach(c::putInt);
        return c;
    }

    private static void readIntMap(CompoundTag t, String key, Map<String, Integer> into) {
        into.clear();
        CompoundTag c = t.getCompound(key);
        for (String k : c.getAllKeys()) into.put(k, c.getInt(k));
    }
}
