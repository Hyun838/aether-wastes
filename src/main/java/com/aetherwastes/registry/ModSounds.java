package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Собственные звуки мода (assets/aetherwastes/sounds). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AetherWastes.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CAST = reg("spell_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = reg("whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ROAR = reg("boss_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_OPEN = reg("rift_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL = reg("ritual");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE = reg("tide");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_HUM = reg("anchor_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHANTOM_PHASE = reg("phantom_phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> PHANTOM_RETURN = reg("phantom_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUNGEON_AMBIENT = reg("dungeon_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_SLAM = reg("boss_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_BREAK = reg("seal_break");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(AetherWastes.id(name)));
    }

    private ModSounds() {}
}
