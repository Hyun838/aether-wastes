package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Автоматическая съёмка для проверки графики (только при -Daetherwastes.autoshots=true):
 * создаёт мир, строит подземелья, ставит камеру и делает скриншоты в папку screenshots.
 */
@EventBusSubscriber(modid = AetherWastes.MODID, value = Dist.CLIENT)
public final class AutoShots {
    private static final boolean ENABLED = Boolean.getBoolean("aetherwastes.autoshots");
    private static boolean worldRequested;
    private static boolean createPressed;
    private static int tick;
    private static int step;
    private static int wait;

    private record Step(int waitAfter, Runnable action) {}

    private static final List<Step> SCRIPT = new ArrayList<>();

    private AutoShots() {}

    private static void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(c);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "aw_" + name + ".png", mc.getMainRenderTarget(),
                msg -> AetherWastes.LOGGER.info("[aw-shot] {} -> {}", name, msg.getString()));
    }

    private static void camera(CameraType type) {
        Minecraft.getInstance().options.setCameraType(type);
    }

    static {
        if (ENABLED) {
            SCRIPT.add(new Step(40, () -> {
                cmd("gamemode creative");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("gamerule doWeatherCycle false");
                cmd("time set 1000");
                cmd("weather clear");
            }));
            SCRIPT.add(new Step(40, () -> shot("hud")));
            // Пепельная цитадель снаружи и внутри
            SCRIPT.add(new Step(420, () -> cmd("aether dungeon citadel outside")));
            SCRIPT.add(new Step(20, () -> shot("citadel_outside")));
            SCRIPT.add(new Step(260, () -> cmd("aether dungeon citadel boss")));
            SCRIPT.add(new Step(20, () -> shot("citadel_boss")));
            // Склеп Эха
            SCRIPT.add(new Step(420, () -> cmd("aether dungeon crypt boss")));
            SCRIPT.add(new Step(20, () -> shot("crypt_boss")));
            SCRIPT.add(new Step(200, () -> cmd("aether dungeon crypt room")));
            SCRIPT.add(new Step(20, () -> shot("crypt_room")));
            // Затерянный Архив
            SCRIPT.add(new Step(420, () -> cmd("aether dungeon archive boss")));
            SCRIPT.add(new Step(20, () -> shot("archive_boss")));
            SCRIPT.add(new Step(200, () -> cmd("aether dungeon archive room")));
            SCRIPT.add(new Step(20, () -> shot("archive_room")));
            // Призрачная броня: вид со стороны и сквозь стену
            SCRIPT.add(new Step(200, () -> {
                cmd("aether dungeon crypt boss");
                camera(CameraType.THIRD_PERSON_FRONT);
            }));
            SCRIPT.add(new Step(30, () -> cmd("aether phase")));
            SCRIPT.add(new Step(20, () -> shot("phantom_third")));
            SCRIPT.add(new Step(10, () -> {
                camera(CameraType.FIRST_PERSON);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) cmd("tp @s ~-4 ~-2.5 ~8 -90 8");
            }));
            SCRIPT.add(new Step(60, () -> shot("phantom_wall")));
            SCRIPT.add(new Step(60, () -> {
                AetherWastes.LOGGER.info("[aw-shot] done");
                Minecraft.getInstance().stop();
            }));
        }
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.execute(() -> mc.setScreen(new TitleScreen(false)));
            return;
        }
        if (event.getScreen() instanceof TitleScreen && !worldRequested) {
            worldRequested = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(8);
            mc.execute(() -> CreateWorldScreen.openFresh(mc, event.getScreen()));
        }
        if (event.getScreen() instanceof CreateWorldScreen cws && !createPressed) {
            WorldCreationUiState ui = cws.getUiState();
            ui.setName("AutoShots");
            ui.setSeed("20261005");
            ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            ui.setAllowCommands(true);
            for (var child : cws.children()) {
                if (child instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc
                        && tc.getKey().equals("selectWorld.create")) {
                    createPressed = true;
                    mc.execute(b::onPress);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        tick++;
        if (tick < 200) return;   // дать миру прогрузиться
        if (wait > 0) {
            wait--;
            return;
        }
        if (step < SCRIPT.size()) {
            Step s = SCRIPT.get(step++);
            AetherWastes.LOGGER.info("[aw-shot] step {}", step);
            try {
                s.action.run();
            } catch (Throwable t) {
                AetherWastes.LOGGER.error("[aw-shot] step failed", t);
            }
            wait = s.waitAfter;
        }
    }
}
