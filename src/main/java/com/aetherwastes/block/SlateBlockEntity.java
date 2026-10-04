package com.aetherwastes.block;

import com.aetherwastes.magic.Glyph;
import com.aetherwastes.magic.SpellData;
import com.aetherwastes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Глифы, выложенные на Скрижаль. */
public class SlateBlockEntity extends BlockEntity {
    private final List<Glyph> glyphs = new ArrayList<>();

    public SlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLATE.get(), pos, state);
    }

    public List<Glyph> glyphs() {
        return glyphs;
    }

    /** Ключ сообщения, почему глиф нельзя положить, или null. */
    public String rejectReason(Glyph glyph) {
        switch (glyph.type()) {
            case FORM -> {
                if (count(Glyph.Type.FORM) >= 1) return "message.aetherwastes.slate.has_form";
            }
            case ESSENCE -> {
                if (count(Glyph.Type.ESSENCE) >= SpellData.MAX_ESSENCES) return "message.aetherwastes.slate.has_essence";
                for (Glyph g : glyphs) if (g == glyph) return "message.aetherwastes.slate.same_essence";
            }
            case MODIFIER -> {
                if (count(Glyph.Type.MODIFIER) >= SpellData.MAX_MODIFIERS) return "message.aetherwastes.slate.too_many_mods";
            }
        }
        return null;
    }

    public void add(Glyph glyph) {
        glyphs.add(glyph);
        setChanged();
    }

    public Glyph removeLast() {
        if (glyphs.isEmpty()) return null;
        Glyph g = glyphs.remove(glyphs.size() - 1);
        setChanged();
        return g;
    }

    public void clear() {
        glyphs.clear();
        setChanged();
    }

    public SpellData compose() {
        Glyph form = null;
        List<Glyph> essences = new ArrayList<>();
        List<Glyph> mods = new ArrayList<>();
        for (Glyph g : glyphs) {
            switch (g.type()) {
                case FORM -> form = g;
                case ESSENCE -> essences.add(g);
                case MODIFIER -> mods.add(g);
            }
        }
        if (form == null || essences.isEmpty()) return null;
        return new SpellData(form, essences, mods);
    }

    private int count(Glyph.Type type) {
        int n = 0;
        for (Glyph g : glyphs) if (g.type() == type) n++;
        return n;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Glyph g : glyphs) list.add(StringTag.valueOf(g.id()));
        tag.put("glyphs", list);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        glyphs.clear();
        ListTag list = tag.getList("glyphs", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Glyph g = Glyph.byId(list.getString(i));
            if (g != null) glyphs.add(g);
        }
    }
}
