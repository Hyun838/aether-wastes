package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.magic.SpellData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AetherWastes.MODID);

    /** Заклинание на жезле. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellData>> SPELL =
            COMPONENTS.register("spell", () -> DataComponentType.<SpellData>builder()
                    .persistent(SpellData.CODEC).networkSynchronized(SpellData.STREAM_CODEC).build());

    /** Качество из Горна (0–3). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUALITY =
            COMPONENTS.register("quality", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Свойства материала. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> TRAITS =
            COMPONENTS.register("traits", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    /** Выгравированные глифы. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> ENGRAVINGS =
            COMPONENTS.register("engravings", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    /** Травы в эликсире. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> HERBS =
            COMPONENTS.register("herbs", () -> DataComponentType.<List<String>>builder()
                    .persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build());

    /** Дополнительный слот гравировки от трофея Охотника. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EXTRA_SLOT =
            COMPONENTS.register("extra_slot", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private ModComponents() {}
}
