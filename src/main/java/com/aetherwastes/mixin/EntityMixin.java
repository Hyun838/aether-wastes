package com.aetherwastes.mixin;

import com.aetherwastes.ability.Phase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Призрачный шаг: во время фазы игрок движется без столкновений с блоками. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void aetherwastes$phaseMove(MoverType type, Vec3 delta, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player p && Phase.isPhasing(p)) {
            self.setPos(self.getX() + delta.x, self.getY() + delta.y, self.getZ() + delta.z);
            self.horizontalCollision = false;
            self.verticalCollision = false;
            self.verticalCollisionBelow = false;
            self.minorHorizontalCollision = false;
            self.setOnGround(false);
            ci.cancel();
        }
    }

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void aetherwastes$phaseWall(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player p && Phase.isPhasing(p)) cir.setReturnValue(false);
    }
}
