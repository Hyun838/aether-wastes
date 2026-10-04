package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.PlayerData;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Данные игрока. Сосуд и Ясность сбрасываются при смерти, прогрессия — нет. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AetherWastes.MODID);

    /** Запас Эфира в теле, 0–100 (до 120 при переполнении). */
    public static final Supplier<AttachmentType<Float>> VESSEL = ATTACHMENTS.register("vessel",
            () -> AttachmentType.builder(() -> 30f).serialize(Codec.FLOAT).build());

    /** Рассудок, 0–100. */
    public static final Supplier<AttachmentType<Float>> CLARITY = ATTACHMENTS.register("clarity",
            () -> AttachmentType.builder(() -> 100f).serialize(Codec.FLOAT).build());

    /** Всё остальное: эпоха, школы, Озарения, травмы, мутации, Шум, Немезида. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENTS.register("player_data",
            () -> AttachmentType.serializable(() -> new PlayerData()).copyOnDeath().build());

    private ModAttachments() {}
}
