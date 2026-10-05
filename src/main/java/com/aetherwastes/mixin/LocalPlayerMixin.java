package com.aetherwastes.mixin;

import com.aetherwastes.network.ClientStatsCache;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Во время Призрачного шага клиент не выталкивает игрока из блоков. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void aetherwastes$noPush(double x, double z, CallbackInfo ci) {
        if (ClientStatsCache.phaseActive) ci.cancel();
    }
}
