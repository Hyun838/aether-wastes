package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.network.AbilityRequestPayload;
import com.aetherwastes.network.ClientStatsCache;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Клиентская часть Призрачного шага: клавиша, отсчёт, отключение выталкивания и затемнения в стенах. */
@EventBusSubscriber(modid = AetherWastes.MODID, value = Dist.CLIENT)
public final class PhaseClient {
    public static final KeyMapping PHASE_KEY = new KeyMapping("key.aetherwastes.phase", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.aetherwastes");
    private static boolean changedCull = false;
    private static boolean wasActive = false;
    /** Плавное появление/исчезновение призрачного виньетирования (0..1). */
    public static float fade = 0f;

    private PhaseClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            ClientStatsCache.phaseActive = false;
            return;
        }
        while (PHASE_KEY.consumeClick()) {
            if (mc.getConnection() != null && mc.getConnection().hasChannel(AbilityRequestPayload.TYPE)) {
                PacketDistributor.sendToServer(new AbilityRequestPayload(0));
            }
        }
        if (mc.isPaused()) return;
        if (ClientStatsCache.phaseTicks > 0) ClientStatsCache.phaseTicks--;
        if (ClientStatsCache.phaseCooldown > 0 && !ClientStatsCache.phaseActive) ClientStatsCache.phaseCooldown--;
        boolean active = ClientStatsCache.phaseActive;
        fade += active ? 0.12f : -0.08f;
        fade = Math.max(0f, Math.min(1f, fade));
        if (active) {
            mc.player.setForcedPose(Pose.STANDING);
            if (mc.smartCull) {
                mc.smartCull = false;
                changedCull = true;
            }
        } else if (wasActive) {
            mc.player.setForcedPose(null);
            if (changedCull) {
                mc.smartCull = true;
                changedCull = false;
            }
        }
        wasActive = active;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide && event.getEntity().isLocalPlayer() && ClientStatsCache.phaseActive) {
            event.getEntity().noPhysics = true;
        }
    }

    @SubscribeEvent
    public static void onBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (ClientStatsCache.phaseActive) event.setCanceled(true);
    }
}
