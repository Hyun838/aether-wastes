package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Рендер мобов Пустошей: своя модель, текстура, масштаб и светящийся слой (глаза, руны, кристаллы). */
public class SpecRenderer<T extends Mob> extends MobRenderer<T, AnimatedModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public SpecRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, AnimatedModel.Style style,
                        String name, float scale, float shadow, boolean items) {
        super(ctx, new AnimatedModel<>(ctx.bakeLayer(layer), style), shadow * scale);
        this.texture = AetherWastes.id("textures/entity/" + name + ".png");
        this.scale = scale;
        addLayer(new GlowLayer<>(this, AetherWastes.id("textures/entity/" + name + "_glow.png")));
        if (items) addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    /** Светящийся слой: рисуется полной яркостью даже в темноте. */
    public static class GlowLayer<E extends Mob, M extends EntityModel<E>> extends EyesLayer<E, M> {
        private final RenderType type;

        public GlowLayer(RenderLayerParent<E, M> parent, ResourceLocation texture) {
            super(parent);
            this.type = RenderType.eyes(texture);
        }

        @Override
        public RenderType renderType() {
            return type;
        }
    }
}
