package com.aetherwastes.client;

import com.aetherwastes.block.HolderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Предмет на ритуальном фокусе или постаменте парит и медленно вращается. */
public class HolderRenderer implements BlockEntityRenderer<HolderBlockEntity> {
    private final float height;

    public HolderRenderer(BlockEntityRendererProvider.Context ctx, float height) {
        this.height = height;
    }

    @Override
    public void render(HolderBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.item();
        if (stack.isEmpty() || be.getLevel() == null) return;
        float time = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, height + 0.08 * Math.sin(time / 12.0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2f));
        pose.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }
}
