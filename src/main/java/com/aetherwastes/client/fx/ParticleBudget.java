package com.aetherwastes.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.ParticleStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Бюджет частиц мода, чтобы эффекты не роняли FPS.
 *
 * <ul>
 *   <li>Корзина жетонов на тик: не больше N новых частиц за игровой тик (зависит от настройки
 *       «Частицы» в видеонастройках игрока). Корзина не может «утечь»: она просто наполняется
 *       заново каждый тик, даже если движок частиц очистили при смене мира.</li>
 *   <li>Отсечение по дальности: дальше предела частица не создаётся вовсе.</li>
 *   <li>Прореживание: за половиной предела создаётся только часть частиц.</li>
 * </ul>
 * Всё считается на клиенте, без аллокаций.
 */
public final class ParticleBudget {
    private static long tick = Long.MIN_VALUE;
    private static int tokens;
    private static int thinCounter;

    private ParticleBudget() {}

    /** Можно ли создать частицу в этой точке прямо сейчас. */
    public static boolean allow(ClientLevel level, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        ParticleStatus status = mc.options.particles().get();
        int perTick;
        double maxDist;
        switch (status) {
            case MINIMAL -> { perTick = 12; maxDist = 24; }
            case DECREASED -> { perTick = 48; maxDist = 40; }
            default -> { perTick = 160; maxDist = 64; }
        }
        long now = level.getGameTime();
        if (now != tick) {
            tick = now;
            tokens = perTick;
        }
        if (tokens <= 0) return false;

        Camera cam = mc.gameRenderer.getMainCamera();
        if (cam.isInitialized()) {
            Vec3 c = cam.getPosition();
            double dx = x - c.x, dy = y - c.y, dz = z - c.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > maxDist * maxDist) return false;
            // За половиной дальности оставляем каждую вторую частицу.
            if (d2 > maxDist * maxDist * 0.25 && (thinCounter++ & 1) == 1) return false;
        }
        tokens--;
        return true;
    }

    /** Квадрат расстояния до камеры (для LOD внутри частиц). */
    public static double cameraDistSqr(double x, double y, double z) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (!cam.isInitialized()) return 0;
        Vec3 c = cam.getPosition();
        double dx = x - c.x, dy = y - c.y, dz = z - c.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
