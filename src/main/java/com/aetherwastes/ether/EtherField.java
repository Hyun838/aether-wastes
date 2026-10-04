package com.aetherwastes.ether;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Эфирное давление (0–100) в точке мира.
 * База — плавный шум по сиду; Эфирные жилы; кольца от точки спавна; глубина; измерение;
 * Прилив Эфира; Якорь тянет давление к комфортным 45.
 */
public final class EtherField {
    public static final float ANCHOR_TARGET = 45f;
    public static final float HIGH = 70f;
    public static final float LOW = 20f;
    /** Радиусы Якоря по уровням 1–5. */
    private static final int[] RADII = {16, 32, 48, 72, 96};

    private EtherField() {}

    public static int anchorRadius(int tier) {
        return RADII[Mth.clamp(tier, 1, 5) - 1];
    }

    public static float pressure(ServerLevel level, BlockPos pos) {
        float p = natural(level, pos);
        Anchor anchor = strongestAnchor(level, pos);
        if (anchor != null) {
            float pull = Math.min(1f, anchor.influence * 1.5f);
            p = p + (ANCHOR_TARGET - p) * pull;
        }
        return Mth.clamp(p, 0f, 100f);
    }

    /** Давление без учёта Якорей. */
    public static float natural(ServerLevel level, BlockPos pos) {
        long seed = level.getSeed();
        double x = pos.getX(), z = pos.getZ();
        double n = 0.65 * valueNoise(seed, x / 96.0, z / 96.0) + 0.35 * valueNoise(seed ^ 0x5DEECE66DL, x / 32.0, z / 32.0);
        float p = 15f + 55f * (float) n;

        p += 20f * leyStrength(seed, pos.getX(), pos.getZ());
        if (level.dimension() == Level.OVERWORLD) {
            p += ringOffset(ring(level, pos));
        }
        if (pos.getY() < 0) p += 10f;
        if (level.dimension() == Level.NETHER) p += 25f;
        else if (level.dimension() == Level.END) p += 35f;
        else if (Dims.isUnderside(level)) p += 30f;

        if (isTide(level)) p += 20f;

        WorldState ws = WorldState.get(level.getServer());
        if (WorldState.ENDING_HEAL.equals(ws.ending)) p *= 0.7f;
        p *= AetherConfig.PRESSURE_MULTIPLIER.get().floatValue();
        return Mth.clamp(p, 0f, 100f);
    }

    /** Кольцо мира 1–4 по расстоянию от точки спавна. */
    public static int ring(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return 3;
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = pos.getX() - spawn.getX(), dz = pos.getZ() - spawn.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double scale = AetherConfig.RING_SCALE.get();
        if (d < 1000 * scale) return 1;
        if (d < 3000 * scale) return 2;
        if (d < 6000 * scale) return 3;
        return 4;
    }

    private static float ringOffset(int ring) {
        return switch (ring) {
            case 1 -> -8f;
            case 2 -> 2f;
            case 3 -> 10f;
            default -> 18f;
        };
    }

    /** 0..2 — близость к Эфирным жилам (2 — пересечение двух жил). */
    public static float leyStrength(long seed, int x, int z) {
        double off = (seed & 0xFFFF);
        double u = x * 0.8 + z * 0.6 + off;
        double v = -x * 0.6 + z * 0.8 + off * 0.5;
        return line(u, 320.0) + line(v, 448.0);
    }

    private static float line(double coord, double period) {
        double m = ((coord % period) + period) % period;
        double dist = Math.min(m, period - m);
        return dist < 5.0 ? (float) (1.0 - dist / 5.0) : 0f;
    }

    public static boolean isTide(Level level) {
        if (!AetherConfig.ETHER_TIDES.get()) return false;
        if (level.getServer() != null && WorldState.ENDING_SEAL.equals(WorldState.get(level.getServer()).ending)) {
            return false;
        }
        long time = level.getDayTime();
        long day = time / 24000L;
        long tod = time % 24000L;
        boolean night = tod >= 13000L && tod <= 23000L;
        long cycle = day % 7L;
        return night && (cycle == 5L || cycle == 6L);
    }

    /** Дней до следующей ночи Прилива (0 — сегодня). */
    public static long daysToTide(Level level) {
        long day = level.getDayTime() / 24000L;
        long cycle = day % 7L;
        if (cycle == 5L || cycle == 6L) return 0;
        return 5L - cycle;
    }

    public static Anchor strongestAnchor(ServerLevel level, BlockPos pos) {
        Anchor best = null;
        for (Map.Entry<Long, Integer> e : AnchorData.get(level).all().entrySet()) {
            BlockPos a = BlockPos.of(e.getKey());
            int r = anchorRadius(e.getValue());
            double d = Math.sqrt(a.distSqr(pos));
            if (d > r) continue;
            float influence = (float) (1.0 - d / r);
            if (best == null || influence > best.influence) {
                best = new Anchor(a, e.getValue(), influence);
            }
        }
        return best;
    }

    public static boolean inAnchor(ServerLevel level, BlockPos pos) {
        return strongestAnchor(level, pos) != null;
    }

    public record Anchor(BlockPos pos, int tier, float influence) {}

    private static double valueNoise(long seed, double x, double z) {
        int x0 = Mth.floor(x), z0 = Mth.floor(z);
        double fx = smooth(x - x0), fz = smooth(z - z0);
        double a = hash(seed, x0, z0), b = hash(seed, x0 + 1, z0);
        double c = hash(seed, x0, z0 + 1), d = hash(seed, x0 + 1, z0 + 1);
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    public static double hash(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }
}
