package com.aetherwastes.world;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.entity.Wanderer;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.threats.Wanderers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Сердце Раскола в центре Изнанки, последний Скиталец и выбор одной из трёх концовок. */
public final class HeartManager {
    public static final BlockPos HEART = new BlockPos(0, 131, 0);

    private HeartManager() {}

    public static void announce(ServerPlayer p) {
        ensureBuilt(p.getServer());
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "heart.aetherwastes.announce", HEART.getX(), HEART.getY(), HEART.getZ());
    }

    public static void ensureBuilt(MinecraftServer server) {
        WorldState ws = WorldState.get(server);
        if (ws.heartBuilt) return;
        ServerLevel u = server.getLevel(Dims.UNDERSIDE);
        if (u == null) return;
        u.getChunk(HEART.getX() >> 4, HEART.getZ() >> 4);
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (dx * dx + dz * dz > 81) continue;
                BlockPos floor = HEART.offset(dx, -1, dz);
                u.setBlockAndUpdate(floor, (dx + dz) % 3 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
                for (int h = 0; h < 6; h++) {
                    BlockPos air = HEART.offset(dx, h, dz);
                    if (!(dx == 0 && dz == 0 && h == 0)) u.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
                }
            }
        }
        u.setBlockAndUpdate(HEART, ModBlocks.HEART_OF_RIFT.get().defaultBlockState());
        u.setBlockAndUpdate(HEART.offset(6, 0, 0), ModBlocks.UNDERSIDE_PORTAL.get().defaultBlockState());
        ws.heartBuilt = true;
        ws.setDirty();
    }

    /** Каждые 2 секунды для игроков в Изнанке. */
    public static void tick(ServerPlayer p) {
        if (!Dims.isUnderside(p.level())) return;
        PlayerData d = Data.get(p);
        if (d.era < 5 || d.wandererKills.contains(EraManager.HEART)) return;
        if (p.blockPosition().distSqr(HEART) > 24 * 24) return;
        ServerLevel level = p.serverLevel();
        boolean alive = !level.getEntitiesOfClass(Wanderer.class, new AABB(HEART).inflate(48),
                w -> w.variant().equals(EraManager.HEART)).isEmpty();
        if (!alive) Wanderers.spawnAt(level, HEART.offset(-5, 0, 0), EraManager.HEART, p);
    }

    public static void offerEndings(ServerPlayer p) {
        if (WorldState.get(p.getServer()).ended()) return;
        Msg.chat(p, ChatFormatting.GOLD, "heart.aetherwastes.choose");
        MutableComponent line = Component.empty();
        for (String e : new String[]{WorldState.ENDING_HEAL, WorldState.ENDING_ABSORB, WorldState.ENDING_SEAL}) {
            line.append(Component.literal("  [")
                    .append(Component.translatable("ending.aetherwastes." + e))
                    .append("]")
                    .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/aether ending " + e))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("ending.aetherwastes." + e + ".desc")))));
        }
        p.displayClientMessage(line, false);
    }

    /** Выбор концовки. Возвращает ключ ошибки или null. */
    public static String choose(ServerPlayer p, String ending) {
        WorldState ws = WorldState.get(p.getServer());
        PlayerData d = Data.get(p);
        if (ws.ended()) return "heart.aetherwastes.already";
        if (!d.wandererKills.contains(EraManager.HEART)) return "heart.aetherwastes.not_yet";
        if (!Dims.isUnderside(p.level()) || p.blockPosition().distSqr(HEART) > 16 * 16) return "heart.aetherwastes.too_far";
        ws.ending = ending;
        ws.setDirty();
        if (WorldState.ENDING_ABSORB.equals(ending)) d.archivist = true;
        if (WorldState.ENDING_SEAL.equals(ending)) give(p, new ItemStack(ModItems.UNDERSIDE_KEY.get()));
        give(p, new ItemStack(ModItems.SECOND_AGE_SEAL.get()));
        for (ServerPlayer other : p.getServer().getPlayerList().getPlayers()) {
            Msg.title(other, Component.translatable("ending.aetherwastes." + ending).withStyle(ChatFormatting.GOLD),
                    Component.translatable("ending.aetherwastes." + ending + ".desc"));
            Sync.all(other);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 0.6f);
        return null;
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.addItem(s)) p.drop(s, false);
    }
}
