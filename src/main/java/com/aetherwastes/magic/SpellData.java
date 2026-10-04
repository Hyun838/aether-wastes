package com.aetherwastes.magic;

import com.aetherwastes.progression.School;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Заклинание, собранное на Скрижали: Форма, 1–2 Сути, до 3 Модификаторов. */
public record SpellData(Glyph form, List<Glyph> essences, List<Glyph> modifiers) {
    public static final int MAX_MODIFIERS = 3;
    public static final int MAX_ESSENCES = 2;

    public static final Codec<Glyph> GLYPH_CODEC = Codec.STRING.comapFlatMap(
            s -> {
                Glyph g = Glyph.byId(s);
                return g == null ? DataResult.error(() -> "Unknown glyph: " + s) : DataResult.success(g);
            },
            Glyph::id);

    public static final Codec<SpellData> CODEC = RecordCodecBuilder.create(i -> i.group(
            GLYPH_CODEC.fieldOf("form").forGetter(SpellData::form),
            GLYPH_CODEC.listOf().fieldOf("essences").forGetter(SpellData::essences),
            GLYPH_CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(SpellData::modifiers)
    ).apply(i, SpellData::new));

    public static final StreamCodec<ByteBuf, SpellData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public SpellData {
        essences = List.copyOf(essences);
        modifiers = List.copyOf(modifiers);
    }

    public int count(Glyph modifier) {
        int n = 0;
        for (Glyph g : modifiers) if (g == modifier) n++;
        return n;
    }

    public List<School> schools() {
        List<School> list = new ArrayList<>();
        for (Glyph e : essences) if (e.school() != null && !list.contains(e.school())) list.add(e.school());
        return list;
    }

    public boolean has(School s) {
        return schools().contains(s);
    }

    public boolean isHybrid() {
        return schools().size() > 1;
    }

    /** Множитель силы от «Усиления». */
    public float amplify() {
        return 1f + 0.5f * count(Glyph.AMPLIFY);
    }

    /** Базовая стоимость без учёта давления. */
    public float baseCost() {
        float c = form.baseCost() * (1f + 0.5f * modifiers.size());
        if (isHybrid()) c *= 1.4f;
        return c;
    }

    public MutableComponent displayName() {
        MutableComponent essence;
        List<School> s = schools();
        if (s.size() > 1) {
            essence = Component.translatable("hybrid.aetherwastes." + School.hybrid(s.get(0), s.get(1)));
        } else if (!essences.isEmpty()) {
            essence = Component.translatable(essences.get(0).translationKey());
        } else {
            essence = Component.literal("?");
        }
        MutableComponent name = Component.translatable("spell.aetherwastes.name",
                Component.translatable(form.translationKey()), essence);
        if (!modifiers.isEmpty()) {
            MutableComponent list = Component.empty();
            for (int k = 0; k < modifiers.size(); k++) {
                if (k > 0) list.append(", ");
                list.append(Component.translatable(modifiers.get(k).translationKey()));
            }
            name.append(Component.literal(" [")).append(list).append("]");
        }
        return name;
    }
}
