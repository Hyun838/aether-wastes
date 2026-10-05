package com.aetherwastes.mixin;

import com.aetherwastes.AetherWastes;
import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Изнанка — обычное измерение мода, а не «экспериментальная» настройка.
 * Без этого игра при создании и загрузке мира показывает пугающее предупреждение.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void aetherwastes$stableUnderside(ResourceKey<LevelStem> key, LevelStem stem, CallbackInfoReturnable<Lifecycle> cir) {
        if (AetherWastes.MODID.equals(key.location().getNamespace())) cir.setReturnValue(Lifecycle.stable());
    }

    /** Лишнее измерение само по себе не делает мир «экспериментальным» — каждое проверяется отдельно выше. */
    @ModifyVariable(method = "bake", at = @At("STORE"), ordinal = 0)
    private Lifecycle aetherwastes$stableRegistry(Lifecycle lifecycle) {
        return Lifecycle.stable();
    }
}
