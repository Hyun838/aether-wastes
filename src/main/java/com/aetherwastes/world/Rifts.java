package com.aetherwastes.world;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.block.RiftPortalBlock;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.threats.Pulse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;

/**
 * Разломы: во время Прилива рядом с игроком открывается портал на игровые сутки.
 * Внутри — маленькая арена в глубине Изнанки со случайным биомом, врагами и наградой.
 * Не успел выйти до закрытия — выбросит в случайную точку Изнанки.
 */
public final class Rifts {
    private static final int ARENA_X = 200000;

    private Rifts() {}

    public static void tick(ServerPlayer p, PlayerData d, boolean tide) {
        if (!AetherConfig.RIFTS.get() || !tide || p.level().dimension() != Level.OVERWORLD) return;
        if (p.tickCount % 2400 != 0 || p.getRandom().nextFloat() > 0.3f) return;
        WorldState ws = WorldState.get(p.getServer());
        for (CompoundTag r : ws.rifts) {
            if (BlockPos.of(r.getLong("pos")).distSqr(p.blockPosition()) < 128 * 128) return;
        }
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        int dist = 24 + p.getRandom().nextInt(16);
        int x = (int) (p.getX() + Math.cos(a) * dist);
        int z = (int) (p.getZ() + Math.sin(a) * dist);
        int y = p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        open(p.serverLevel(), new BlockPos(x, y, z), 24000L);
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "rift.aetherwastes.opened");
    }

    /** Ключ Изнанки открывает испытание прямо перед игроком. */
    public static boolean openTrialNear(ServerPlayer p) {
        BlockPos pos = p.blockPosition().relative(p.getDirection(), 3);
        if (!p.level().getBlockState(pos).canBeReplaced()) {
            Msg.bar(p, ChatFormatting.GRAY, "rift.aetherwastes.no_space");
            return false;
        }
        open(p.serverLevel(), pos, 6000L);
        Msg.bar(p, ChatFormatting.LIGHT_PURPLE, "rift.aetherwastes.key_opened");
        return true;
    }

    private static void open(ServerLevel level, BlockPos pos, long life) {
        WorldState ws = WorldState.get(level.getServer());
        int id = ++ws.riftCounter;
        CompoundTag r = new CompoundTag();
        r.putInt("id", id);
        r.putString("dim", level.dimension().location().toString());
        r.putLong("pos", pos.asLong());
        r.putLong("expire", level.getGameTime() + life);
        r.putBoolean("built", false);
        ws.rifts.add(r);
        ws.setDirty();
        level.setBlockAndUpdate(pos, ModBlocks.RIFT_PORTAL.get().defaultBlockState());
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7f, 1.5f);
    }

    private static BlockPos arena(int id) {
        return new BlockPos(ARENA_X + id * 400, 150, 0);
    }

    public static void enter(ServerPlayer p, BlockPos portal, boolean exit) {
        PlayerData d = Data.get(p);
        long now = p.level().getGameTime();
        if (now - d.lastRiftTp < 60) return;
        d.lastRiftTp = now;
        MinecraftServer server = p.getServer();
        if (exit) {
            leave(p, d);
            return;
        }
        WorldState ws = WorldState.get(server);
        CompoundTag rift = null;
        for (CompoundTag r : ws.rifts) {
            if (r.getLong("pos") == portal.asLong() && r.getString("dim").equals(p.level().dimension().location().toString())) rift = r;
        }
        if (rift == null) return;
        ServerLevel u = server.getLevel(Dims.UNDERSIDE);
        if (u == null) return;
        BlockPos center = arena(rift.getInt("id"));
        if (!rift.getBoolean("built")) {
            build(u, center, rift.getInt("id"));
            rift.putBoolean("built", true);
            ws.setDirty();
        }
        CompoundTag back = new CompoundTag();
        back.putString("dim", p.level().dimension().location().toString());
        back.putDouble("x", p.getX());
        back.putDouble("y", p.getY());
        back.putDouble("z", p.getZ());
        d.riftReturn = back;
        p.teleportTo(u, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, p.getYRot(), p.getXRot());
        spawnGuardians(u, center, p);
        Msg.title(p, net.minecraft.network.chat.Component.translatable("rift.aetherwastes.title").withStyle(ChatFormatting.LIGHT_PURPLE),
                net.minecraft.network.chat.Component.translatable("rift.aetherwastes.subtitle"));
    }

    public static void leave(ServerPlayer p, PlayerData d) {
        MinecraftServer server = p.getServer();
        ServerLevel target = server.overworld();
        double x = target.getSharedSpawnPos().getX(), y = target.getSharedSpawnPos().getY(), z = target.getSharedSpawnPos().getZ();
        if (d.riftReturn.contains("dim")) {
            ServerLevel l = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.riftReturn.getString("dim"))));
            if (l != null) target = l;
            x = d.riftReturn.getDouble("x");
            y = d.riftReturn.getDouble("y");
            z = d.riftReturn.getDouble("z");
        }
        d.riftReturn = new CompoundTag();
        p.teleportTo(target, x, y, z, p.getYRot(), p.getXRot());
    }

    private static void build(ServerLevel u, BlockPos c, int id) {
        u.getChunk(c.getX() >> 4, c.getZ() >> 4);
        BlockState[] themes = {
                ModBlocks.ASH_BLOCK.get().defaultBlockState(), ModBlocks.SALT_CRUST.get().defaultBlockState(),
                ModBlocks.ETHER_GLASS.get().defaultBlockState(), Blocks.MYCELIUM.defaultBlockState(),
                Blocks.CALCITE.defaultBlockState(), Blocks.SOUL_SOIL.defaultBlockState()};
        BlockState floor = themes[Math.floorMod(id * 7, themes.length)];
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                boolean edge = Math.abs(dx) == 8 || Math.abs(dz) == 8;
                u.setBlockAndUpdate(c.offset(dx, 0, dz), edge ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : floor);
                for (int h = 1; h <= 4; h++) u.setBlockAndUpdate(c.offset(dx, h, dz), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos chest = c.offset(0, 1, 6);
        u.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        if (u.getBlockEntity(chest) instanceof ChestBlockEntity cb) {
            cb.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, AetherWastes.id("chests/rift_reward")), u.random.nextLong());
        }
        u.setBlockAndUpdate(c.offset(0, 1, -6), ModBlocks.RIFT_PORTAL.get().defaultBlockState().setValue(RiftPortalBlock.EXIT, true));
    }

    private static void spawnGuardians(ServerLevel u, BlockPos c, ServerPlayer p) {
        var pool = Pulse.monsterPool(3 + u.random.nextInt(2));
        int count = 3 + u.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            Mob m = pool[u.random.nextInt(pool.length)].create(u);
            if (m == null) continue;
            m.moveTo(c.getX() + u.random.nextInt(11) - 5 + 0.5, c.getY() + 1, c.getZ() + u.random.nextInt(7) - 1 + 0.5, 0, 0);
            m.finalizeSpawn(u, u.getCurrentDifficultyAt(c), net.minecraft.world.entity.MobSpawnType.EVENT, null);
            m.addTag("aw_spawned");
            m.setTarget(p);
            u.addFreshEntity(m);
        }
    }

    /** Раз в 30 секунд: закрыть истёкшие Разломы. */
    public static void expire(MinecraftServer server) {
        WorldState ws = WorldState.get(server);
        long now = server.overworld().getGameTime();
        Iterator<CompoundTag> it = ws.rifts.iterator();
        while (it.hasNext()) {
            CompoundTag r = it.next();
            if (r.getLong("expire") > now) continue;
            ServerLevel l = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(r.getString("dim"))));
            BlockPos pos = BlockPos.of(r.getLong("pos"));
            if (l != null && l.isLoaded(pos) && l.getBlockState(pos).is(ModBlocks.RIFT_PORTAL.get())) {
                l.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            ServerLevel u = server.getLevel(Dims.UNDERSIDE);
            BlockPos c = arena(r.getInt("id"));
            if (u != null) {
                for (ServerPlayer p : u.players()) {
                    if (Math.abs(p.getX() - c.getX()) < 40 && Math.abs(p.getZ() - c.getZ()) < 40) {
                        int x = u.random.nextInt(6000) - 3000, z = u.random.nextInt(6000) - 3000;
                        int y = Underside.findGround(u, x, z);
                        p.teleportTo(u, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
                        Data.get(p).riftReturn = new CompoundTag();
                        Msg.chat(p, ChatFormatting.DARK_PURPLE, "rift.aetherwastes.thrown");
                    }
                }
            }
            it.remove();
            ws.setDirty();
        }
    }
}
