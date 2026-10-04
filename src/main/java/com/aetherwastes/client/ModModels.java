package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Сгенерировано из описаний моделей (art/entityart.py). Не править вручную. */
public final class ModModels {
    public static final ModelLayerLocation ASH_HOUND = new ModelLayerLocation(AetherWastes.id("ash_hound"), "main");
    public static final ModelLayerLocation ETHER_WISP = new ModelLayerLocation(AetherWastes.id("ether_wisp"), "main");
    public static final ModelLayerLocation SCAR_CRAWLER = new ModelLayerLocation(AetherWastes.id("scar_crawler"), "main");
    public static final ModelLayerLocation SALT_WRAITH = new ModelLayerLocation(AetherWastes.id("salt_wraith"), "main");
    public static final ModelLayerLocation RESIN_WALKER = new ModelLayerLocation(AetherWastes.id("resin_walker"), "main");
    public static final ModelLayerLocation GLASSMAN = new ModelLayerLocation(AetherWastes.id("glassman"), "main");
    public static final ModelLayerLocation WANDERER_SPARK = new ModelLayerLocation(AetherWastes.id("wanderer_spark"), "main");
    public static final ModelLayerLocation WANDERER_RESONANCE = new ModelLayerLocation(AetherWastes.id("wanderer_resonance"), "main");
    public static final ModelLayerLocation WANDERER_CONVERGENCE = new ModelLayerLocation(AetherWastes.id("wanderer_convergence"), "main");
    public static final ModelLayerLocation WANDERER_HEART = new ModelLayerLocation(AetherWastes.id("wanderer_heart"), "main");

    public static LayerDefinition ashHound() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 12.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(36, 0).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 7.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 5.0F), PartPose.offset(0.0F, 11.0F, -7.0F));
        PartDefinition p3 = p2.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(18, 29).addBox(-1.5000F, 0.0F, -8.0F, 3.0F, 3.0F, 3.0F).texOffs(38, 29).addBox(-1.5000F, 3.0F, -7.5000F, 3.0F, 1.0F, 2.0F).texOffs(48, 29).addBox(-3.0F, -5.0F, -3.0F, 2.0F, 2.0F, 1.0F).texOffs(54, 29).addBox(1.0F, -5.0F, -3.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 11.0F, 6.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition p5 = p4.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(30, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition p6 = root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(22, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(-2.0F, 16.0F, -4.0F));
        PartDefinition p7 = root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(30, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(2.0F, 16.0F, -4.0F));
        PartDefinition p8 = root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(38, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(-2.0F, 16.0F, 4.0F));
        PartDefinition p9 = root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(46, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(2.0F, 16.0F, 4.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition etherWisp() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(48, 0).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.7854F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(0, 12).addBox(-6.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F).texOffs(10, 12).addBox(3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.7854F));
        PartDefinition p3 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F).texOffs(56, 0).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition p4 = p3.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(20, 12).addBox(-0.5000F, 0.0F, -0.5000F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition p5 = root.addOrReplaceChild("orbit", CubeListBuilder.create().texOffs(24, 12).addBox(6.0F, -1.0F, -0.5000F, 1.0F, 1.0F, 1.0F).texOffs(28, 12).addBox(-7.0F, 1.0F, -0.5000F, 1.0F, 1.0F, 1.0F).texOffs(32, 12).addBox(-0.5000F, -2.0F, 6.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition scarCrawler() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(76, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition p1 = root.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 12.0F), PartPose.offset(0.0F, 15.0F, 9.0F));
        PartDefinition p2 = p1.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(108, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, -0.3491F, 0.0F, 0.2618F));
        PartDefinition p3 = p1.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(116, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, -4.0F, 2.0F, 0.2618F, 0.0F, -0.4363F));
        PartDefinition p4 = p1.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(100, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(3.0F, -4.0F, 3.0F, 0.4363F, 0.0F, 0.3491F));
        PartDefinition p5 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(44, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 15.0F, -3.0F));
        PartDefinition p6 = p5.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, 0.0F, -2.0F, 2.0F, 3.0F, 2.0F).texOffs(8, 20).addBox(1.0F, 0.0F, -2.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 2.0F, -8.0F));
        PartDefinition p7 = root.addOrReplaceChild("leg0", CubeListBuilder.create().texOffs(16, 20).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 15.0F, 2.0F, 0.0F, 0.7854F, -0.7854F));
        PartDefinition p8 = root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(52, 20).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 15.0F, 2.0F, 0.0F, -0.7854F, 0.7854F));
        PartDefinition p9 = root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(88, 20).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 15.0F, 1.0F, 0.0F, 0.3927F, -0.5760F));
        PartDefinition p10 = root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(0, 25).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 15.0F, 1.0F, 0.0F, -0.3927F, 0.5760F));
        PartDefinition p11 = root.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(36, 25).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 15.0F, 0.0F, 0.0F, -0.3927F, -0.5760F));
        PartDefinition p12 = root.addOrReplaceChild("leg5", CubeListBuilder.create().texOffs(72, 25).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 15.0F, 0.0F, 0.0F, 0.3927F, 0.5760F));
        PartDefinition p13 = root.addOrReplaceChild("leg6", CubeListBuilder.create().texOffs(0, 29).addBox(-15.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 15.0F, -1.0F, 0.0F, -0.7854F, -0.7854F));
        PartDefinition p14 = root.addOrReplaceChild("leg7", CubeListBuilder.create().texOffs(36, 29).addBox(-1.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 15.0F, -1.0F, 0.0F, 0.7854F, 0.7854F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition saltWraith() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(0, 31).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 2.0F, 10.0F).texOffs(32, 43).addBox(-5.0F, -7.0F, 3.0F, 10.0F, 8.0F, 2.0F).texOffs(0, 16).addBox(-5.0F, -7.0F, -5.0F, 1.0F, 7.0F, 8.0F).texOffs(18, 16).addBox(4.0F, -7.0F, -5.0F, 1.0F, 7.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = p2.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(24, 54).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));
        PartDefinition p4 = p2.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(32, 54).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));
        PartDefinition p5 = p2.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(40, 54).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 2.0F, -0.3491F, 0.0F, 0.0F));
        PartDefinition p6 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 43).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 5.0F, 6.0F).texOffs(0, 54).addBox(-4.0F, 5.0F, -2.0F, 8.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p7 = p6.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(48, 54).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-3.0F, 8.0F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p8 = p6.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(24, 61).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 8.0F, 1.0F, -0.2094F, 0.0F, 0.0F));
        PartDefinition p9 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16).addBox(-2.0F, -2.0F, -1.5000F, 3.0F, 12.0F, 3.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p10 = p9.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(0, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 2.0F, 3.0F).texOffs(40, 61).addBox(-1.5000F, 2.0F, -1.5000F, 1.0F, 2.0F, 1.0F).texOffs(32, 61).addBox(0.5000F, 2.0F, -1.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(-0.5000F, 10.0F, 0.0F));
        PartDefinition p11 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 16).addBox(-1.0F, -2.0F, -1.5000F, 3.0F, 12.0F, 3.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p12 = p11.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(12, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 2.0F, 3.0F).texOffs(36, 61).addBox(-1.5000F, 2.0F, -1.5000F, 1.0F, 3.0F, 1.0F).texOffs(44, 61).addBox(0.5000F, 2.0F, -1.5000F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.5000F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    public static LayerDefinition resinWalker() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 37).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(56, 52).addBox(-0.5000F, -6.0F, -0.5000F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.4363F));
        PartDefinition p2 = p0.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(52, 52).addBox(-0.5000F, -7.0F, -0.5000F, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(2.0F, -7.0F, -1.0F, 0.1745F, 0.0F, 0.3491F));
        PartDefinition p3 = p0.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(0, 52).addBox(-5.0F, -3.0F, -4.0F, 10.0F, 4.0F, 8.0F).texOffs(0, 64).addBox(-3.0F, -5.0F, -2.0F, 6.0F, 2.0F, 5.0F), PartPose.offset(0.0F, -11.0F, 0.0F));
        PartDefinition p4 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 14.0F, 6.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition p5 = p4.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(36, 52).addBox(-6.0F, -1.0F, -2.0F, 3.0F, 4.0F, 5.0F).texOffs(22, 64).addBox(3.0F, 2.0F, -3.0F, 3.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p6 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(-6.0F, 0.0F, 0.0F));
        PartDefinition p7 = p6.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(44, 64).addBox(-2.0F, 0.0F, -2.0F, 1.0F, 4.0F, 1.0F).texOffs(36, 64).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 1.0F).texOffs(48, 64).addBox(-1.0F, 0.0F, 1.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(-1.0F, 14.0F, 0.0F));
        PartDefinition p8 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 16.0F, 4.0F), PartPose.offset(6.0F, 0.0F, 0.0F));
        PartDefinition p9 = p8.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(40, 64).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 1.0F).texOffs(52, 64).addBox(1.0F, 0.0F, -2.0F, 1.0F, 4.0F, 1.0F).texOffs(56, 64).addBox(0.0F, 0.0F, 1.0F, 1.0F, 4.0F, 1.0F), PartPose.offset(1.0F, 14.0F, 0.0F));
        PartDefinition p10 = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 20).addBox(-2.5000F, 0.0F, -2.5000F, 5.0F, 12.0F, 5.0F), PartPose.offset(-2.5000F, 12.0F, 0.0F));
        PartDefinition p11 = p10.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(0, 71).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 10.0F, -2.0F));
        PartDefinition p12 = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 20).addBox(-2.5000F, 0.0F, -2.5000F, 5.0F, 12.0F, 5.0F), PartPose.offset(2.5000F, 12.0F, 0.0F));
        PartDefinition p13 = p12.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(16, 71).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 10.0F, -2.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    public static LayerDefinition glassman() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 15).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(24, 15).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p3 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 11.0F, 4.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = p3.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(8, 28).addBox(-1.5000F, -1.5000F, -1.0F, 3.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 4.0F, -2.0F));
        PartDefinition p5 = p3.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(48, 15).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.5236F));
        PartDefinition p6 = p3.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(56, 15).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.5236F));
        PartDefinition p7 = p3.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(40, 15).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, -0.5236F, 0.0F, 0.0F));
        PartDefinition p8 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p9 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p10 = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        PartDefinition p11 = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition wandererSpark() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(64, 35).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F).texOffs(0, 51).addBox(-5.0F, -8.0F, 4.0F, 10.0F, 9.0F, 1.0F).texOffs(4, 0).addBox(-5.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F).texOffs(24, 0).addBox(4.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(106, 61).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 1.0F, 1.0F).texOffs(0, 68).addBox(-5.0F, 0.0F, 4.0F, 10.0F, 1.0F, 1.0F).texOffs(66, 51).addBox(-5.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F).texOffs(84, 51).addBox(4.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition p3 = p2.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(54, 61).addBox(-0.5000F, -4.0F, -0.5000F, 1.0F, 4.0F, 1.0F), PartPose.offset(-3.5000F, 0.0F, -4.5000F));
        PartDefinition p4 = p2.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(82, 61).addBox(-0.5000F, -3.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(3.5000F, 0.0F, -4.5000F));
        PartDefinition p5 = p2.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(58, 61).addBox(-0.5000F, -4.0F, -0.5000F, 1.0F, 4.0F, 1.0F), PartPose.offset(-3.5000F, 0.0F, 4.5000F));
        PartDefinition p6 = p2.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(86, 61).addBox(-0.5000F, -3.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(3.5000F, 0.0F, 4.5000F));
        PartDefinition p7 = p2.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(62, 61).addBox(-0.5000F, -4.0F, -0.5000F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -4.5000F));
        PartDefinition p8 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(78, 0).addBox(-4.0F, 0.0F, -2.5000F, 8.0F, 12.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p9 = p8.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(0, 61).addBox(-4.5000F, 8.0F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p10 = p8.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(22, 51).addBox(-3.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2094F));
        PartDefinition p11 = p8.addOrReplaceChild("d9", CubeListBuilder.create().texOffs(44, 51).addBox(-1.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));
        PartDefinition p12 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(44, 0).addBox(-5.0F, 0.0F, -3.5000F, 10.0F, 11.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p13 = p12.addOrReplaceChild("d10", CubeListBuilder.create().texOffs(104, 35).addBox(-1.0F, 0.0F, -0.5000F, 2.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -3.5000F));
        PartDefinition p14 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(-6.0F, 2.0F, 0.0F));
        PartDefinition p15 = p14.addOrReplaceChild("d13", CubeListBuilder.create().texOffs(30, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p16 = p14.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5000F, -26.0F, -0.5000F, 1.0F, 34.0F, 1.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p17 = p16.addOrReplaceChild("d11", CubeListBuilder.create().texOffs(102, 51).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -28.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p18 = p16.addOrReplaceChild("d12", CubeListBuilder.create().texOffs(90, 61).addBox(-2.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F).texOffs(94, 61).addBox(1.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
        PartDefinition p19 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(6.0F, 2.0F, 0.0F));
        PartDefinition p20 = p19.addOrReplaceChild("d14", CubeListBuilder.create().texOffs(42, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p21 = root.addOrReplaceChild("orbit", CubeListBuilder.create().texOffs(66, 61).addBox(9.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(74, 61).addBox(-11.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(98, 61).addBox(-1.0F, 1.0F, 9.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition wandererResonance() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(64, 35).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F).texOffs(0, 51).addBox(-5.0F, -8.0F, 4.0F, 10.0F, 9.0F, 1.0F).texOffs(4, 0).addBox(-5.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F).texOffs(24, 0).addBox(4.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(0, 68).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 1.0F, 1.0F).texOffs(22, 68).addBox(-5.0F, 0.0F, 4.0F, 10.0F, 1.0F, 1.0F).texOffs(70, 51).addBox(-5.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F).texOffs(88, 51).addBox(4.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition p3 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(78, 0).addBox(-4.0F, 0.0F, -2.5000F, 8.0F, 12.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = p3.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(0, 61).addBox(-4.5000F, 8.0F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p5 = p3.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(22, 51).addBox(-4.0F, -2.0F, -3.5000F, 5.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2094F));
        PartDefinition p6 = p3.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(46, 51).addBox(-1.0F, -2.0F, -3.5000F, 5.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));
        PartDefinition p7 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(44, 0).addBox(-5.0F, 0.0F, -3.5000F, 10.0F, 11.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p8 = p7.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(104, 35).addBox(-1.0F, 0.0F, -0.5000F, 2.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -3.5000F));
        PartDefinition p9 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(-6.0F, 2.0F, 0.0F));
        PartDefinition p10 = p9.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(30, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p11 = p9.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5000F, -26.0F, -0.5000F, 1.0F, 34.0F, 1.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p12 = p11.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(106, 51).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -28.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p13 = p11.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(104, 61).addBox(-2.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F).texOffs(108, 61).addBox(1.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
        PartDefinition p14 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(6.0F, 2.0F, 0.0F));
        PartDefinition p15 = p14.addOrReplaceChild("d9", CubeListBuilder.create().texOffs(42, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p16 = root.addOrReplaceChild("orbit", CubeListBuilder.create().texOffs(54, 61).addBox(9.0F, -2.0F, -2.0F, 4.0F, 3.0F, 3.0F).texOffs(68, 61).addBox(-12.0F, -1.0F, -1.0F, 3.0F, 3.0F, 3.0F).texOffs(92, 61).addBox(-2.0F, 0.0F, 9.0F, 3.0F, 2.0F, 3.0F).texOffs(80, 61).addBox(-2.0F, -3.0F, -12.0F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition wandererConvergence() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(64, 35).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F).texOffs(0, 51).addBox(-5.0F, -8.0F, 4.0F, 10.0F, 9.0F, 1.0F).texOffs(4, 0).addBox(-5.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F).texOffs(24, 0).addBox(4.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(102, 61).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 1.0F, 1.0F).texOffs(0, 68).addBox(-5.0F, 0.0F, 4.0F, 10.0F, 1.0F, 1.0F).texOffs(66, 51).addBox(-5.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F).texOffs(84, 51).addBox(4.0F, 0.0F, -4.0F, 1.0F, 1.0F, 8.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition p3 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(78, 0).addBox(-4.0F, 0.0F, -2.5000F, 8.0F, 12.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = p3.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(0, 61).addBox(-4.5000F, 8.0F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p5 = p3.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(22, 51).addBox(-3.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2094F));
        PartDefinition p6 = p3.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(44, 51).addBox(-1.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));
        PartDefinition p7 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(44, 0).addBox(-5.0F, 0.0F, -3.5000F, 10.0F, 11.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p8 = p7.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(104, 35).addBox(-1.0F, 0.0F, -0.5000F, 2.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -3.5000F));
        PartDefinition p9 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(-6.0F, 2.0F, 0.0F));
        PartDefinition p10 = p9.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(30, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p11 = p9.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5000F, -26.0F, -0.5000F, 1.0F, 34.0F, 1.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p12 = p11.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(102, 51).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -28.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p13 = p11.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(70, 61).addBox(-2.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F).texOffs(74, 61).addBox(1.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
        PartDefinition p14 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(6.0F, 2.0F, 0.0F));
        PartDefinition p15 = p14.addOrReplaceChild("d9", CubeListBuilder.create().texOffs(42, 61).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p16 = root.addOrReplaceChild("orbit", CubeListBuilder.create().texOffs(54, 61).addBox(9.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(62, 61).addBox(-11.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(78, 61).addBox(-1.0F, 1.0F, 9.0F, 2.0F, 2.0F, 2.0F).texOffs(86, 61).addBox(-1.0F, -4.0F, -11.0F, 2.0F, 2.0F, 2.0F).texOffs(94, 61).addBox(7.0F, 3.0F, 7.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition wandererHeart() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p0 = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("d5", CubeListBuilder.create().texOffs(0, 53).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F).texOffs(90, 53).addBox(-5.0F, -8.0F, 4.0F, 10.0F, 9.0F, 1.0F).texOffs(64, 0).addBox(-5.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F).texOffs(84, 0).addBox(4.0F, -8.0F, -5.0F, 1.0F, 9.0F, 9.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = p0.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(56, 74).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 1.0F, 1.0F).texOffs(82, 74).addBox(-6.0F, 0.0F, 5.0F, 12.0F, 1.0F, 1.0F).texOffs(40, 53).addBox(-6.0F, 0.0F, -5.0F, 1.0F, 1.0F, 10.0F).texOffs(62, 53).addBox(5.0F, 0.0F, -5.0F, 1.0F, 1.0F, 10.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition p3 = p2.addOrReplaceChild("d1", CubeListBuilder.create().texOffs(8, 74).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-5.5000F, -1.0F, -5.5000F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p4 = p2.addOrReplaceChild("d2", CubeListBuilder.create().texOffs(16, 74).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(5.5000F, -1.0F, -5.5000F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p5 = p2.addOrReplaceChild("d3", CubeListBuilder.create().texOffs(24, 74).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-5.5000F, -1.0F, 5.5000F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p6 = p2.addOrReplaceChild("d4", CubeListBuilder.create().texOffs(32, 74).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(5.5000F, -1.0F, 5.5000F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p7 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 35).addBox(-4.0F, 0.0F, -2.5000F, 8.0F, 12.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p8 = p7.addOrReplaceChild("d6", CubeListBuilder.create().texOffs(60, 64).addBox(-4.5000F, 8.0F, -3.0F, 9.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p9 = p7.addOrReplaceChild("d7", CubeListBuilder.create().texOffs(0, 64).addBox(-3.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2094F));
        PartDefinition p10 = p7.addOrReplaceChild("d8", CubeListBuilder.create().texOffs(22, 64).addBox(-1.0F, -2.0F, -3.5000F, 4.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2094F));
        PartDefinition p11 = p7.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(4, 0).addBox(-14.0F, -8.0F, 0.0F, 14.0F, 18.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 1.0F, 2.5000F, 0.0F, -0.6109F, 0.0F));
        PartDefinition p12 = p7.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(34, 0).addBox(0.0F, -8.0F, 0.0F, 14.0F, 18.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 1.0F, 2.5000F, 0.0F, 0.6109F, 0.0F));
        PartDefinition p13 = root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 35).addBox(-5.0F, 0.0F, -3.5000F, 10.0F, 11.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition p14 = p13.addOrReplaceChild("d9", CubeListBuilder.create().texOffs(84, 53).addBox(-1.0F, 0.0F, -0.5000F, 2.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -3.5000F));
        PartDefinition p15 = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(92, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(-6.0F, 2.0F, 0.0F));
        PartDefinition p16 = p15.addOrReplaceChild("d12", CubeListBuilder.create().texOffs(90, 64).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p17 = p15.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5000F, -26.0F, -0.5000F, 1.0F, 34.0F, 1.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition p18 = p17.addOrReplaceChild("d10", CubeListBuilder.create().texOffs(44, 64).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -28.0F, 0.0F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p19 = p17.addOrReplaceChild("d11", CubeListBuilder.create().texOffs(40, 74).addBox(-2.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F).texOffs(44, 74).addBox(1.0F, -1.0F, -0.5000F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -26.0F, 0.0F));
        PartDefinition p20 = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(108, 35).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(6.0F, 2.0F, 0.0F));
        PartDefinition p21 = p20.addOrReplaceChild("d13", CubeListBuilder.create().texOffs(102, 64).addBox(-1.5000F, 0.0F, -1.5000F, 3.0F, 4.0F, 3.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition p22 = root.addOrReplaceChild("orbit", CubeListBuilder.create().texOffs(114, 64).addBox(9.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(0, 74).addBox(-11.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(48, 74).addBox(-1.0F, 1.0F, 9.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private ModModels() {}
}
