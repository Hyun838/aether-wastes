package com.aetherwastes.ether;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Якоря и устройства базы в одном измерении. */
public class AnchorData extends SavedData {
    private static final String NAME = "aetherwastes_anchors";
    private final Map<Long, Integer> anchors = new HashMap<>();
    private final Map<Long, String> devices = new HashMap<>();
    private final Map<Long, Boolean> damagedPylons = new HashMap<>();

    public static AnchorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AnchorData::new, AnchorData::load, null), NAME);
    }

    public static AnchorData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnchorData data = new AnchorData();
        ListTag list = tag.getList("anchors", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.anchors.put(e.getLong("pos"), e.getInt("tier"));
        }
        ListTag dev = tag.getList("devices", Tag.TAG_COMPOUND);
        for (int i = 0; i < dev.size(); i++) {
            CompoundTag e = dev.getCompound(i);
            data.devices.put(e.getLong("pos"), e.getString("type"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        anchors.forEach((pos, tier) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", pos);
            e.putInt("tier", tier);
            list.add(e);
        });
        tag.put("anchors", list);
        ListTag dev = new ListTag();
        devices.forEach((pos, type) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", pos);
            e.putString("type", type);
            dev.add(e);
        });
        tag.put("devices", dev);
        return tag;
    }

    public void put(BlockPos pos, int tier) {
        Integer old = anchors.put(pos.asLong(), tier);
        if (old == null || old != tier) setDirty();
    }

    public void remove(BlockPos pos) {
        if (anchors.remove(pos.asLong()) != null) setDirty();
    }

    public Map<Long, Integer> all() {
        return Collections.unmodifiableMap(anchors);
    }

    public void putDevice(BlockPos pos, String type) {
        devices.put(pos.asLong(), type);
        setDirty();
    }

    public void removeDevice(BlockPos pos) {
        if (devices.remove(pos.asLong()) != null) setDirty();
        damagedPylons.remove(pos.asLong());
    }

    public Map<Long, String> devices() {
        return Collections.unmodifiableMap(devices);
    }
}
