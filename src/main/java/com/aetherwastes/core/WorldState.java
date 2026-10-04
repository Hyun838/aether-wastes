package com.aetherwastes.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Общее состояние мира (хранится в Верхнем мире): концовка, Разломы, Порча, миграции, распад в Изнанке. */
public class WorldState extends SavedData {
    private static final String NAME = "aetherwastes_world";

    public static final String ENDING_NONE = "";
    public static final String ENDING_HEAL = "heal";
    public static final String ENDING_ABSORB = "absorb";
    public static final String ENDING_SEAL = "seal";

    public String ending = ENDING_NONE;
    public long nextMigrationDay = 3;
    public long killsDay = -1;
    public final Map<Long, Integer> chunkKills = new HashMap<>();
    public final List<CompoundTag> rifts = new ArrayList<>();
    public int riftCounter = 0;
    public boolean heartBuilt = false;
    public final List<CompoundTag> undersideDecay = new ArrayList<>();
    public long lastDay = -1;

    public static WorldState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WorldState::new, WorldState::load, null), NAME);
    }

    public static WorldState load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldState s = new WorldState();
        s.ending = tag.getString("ending");
        s.nextMigrationDay = tag.contains("nextMigrationDay") ? tag.getLong("nextMigrationDay") : 3;
        s.killsDay = tag.getLong("killsDay");
        CompoundTag kills = tag.getCompound("chunkKills");
        for (String k : kills.getAllKeys()) {
            try {
                s.chunkKills.put(Long.parseLong(k), kills.getInt(k));
            } catch (NumberFormatException ignored) {
            }
        }
        ListTag rifts = tag.getList("rifts", Tag.TAG_COMPOUND);
        for (int i = 0; i < rifts.size(); i++) s.rifts.add(rifts.getCompound(i));
        s.riftCounter = tag.getInt("riftCounter");
        s.heartBuilt = tag.getBoolean("heartBuilt");
        ListTag decay = tag.getList("decay", Tag.TAG_COMPOUND);
        for (int i = 0; i < decay.size(); i++) s.undersideDecay.add(decay.getCompound(i));
        s.lastDay = tag.contains("lastDay") ? tag.getLong("lastDay") : -1;
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("ending", ending);
        tag.putLong("nextMigrationDay", nextMigrationDay);
        tag.putLong("killsDay", killsDay);
        CompoundTag kills = new CompoundTag();
        chunkKills.forEach((k, v) -> kills.putInt(Long.toString(k), v));
        tag.put("chunkKills", kills);
        ListTag r = new ListTag();
        r.addAll(rifts);
        tag.put("rifts", r);
        tag.putInt("riftCounter", riftCounter);
        tag.putBoolean("heartBuilt", heartBuilt);
        ListTag d = new ListTag();
        d.addAll(undersideDecay);
        tag.put("decay", d);
        tag.putLong("lastDay", lastDay);
        return tag;
    }

    public boolean ended() {
        return !ending.isEmpty();
    }
}
