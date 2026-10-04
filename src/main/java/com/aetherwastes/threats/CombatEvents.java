package com.aetherwastes.threats;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.base.Wards;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.craft.Engraving;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.item.StaffItem;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.progression.Mastery;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.survival.Mutations;
import com.aetherwastes.survival.Traumas;
import com.aetherwastes.world.Caravans;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Бой, убийства, добыча и появление мобов: всё, что связывает действия игрока с ответом мира. */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class CombatEvents {
    private CombatEvents() {}

    public static String category(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return "magic";
        if (s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "ranged";
        return "melee";
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource source = event.getSource();
        String cat = category(source);

        String resist = target.getPersistentData().getString(Hunters.RESIST);
        if (!resist.isEmpty() && resist.equals(cat)) event.setAmount(event.getAmount() * 0.25f);

        if (target instanceof ServerPlayer p) {
            PlayerData d = Data.get(p);
            if (d.mutations.contains(Mutations.GLASS_SKIN) && source.is(DamageTypeTags.IS_EXPLOSION)) {
                event.setAmount(event.getAmount() * 2f);
            }
            int forge = Engraving.armorCount(p, "forge");
            if (forge > 0 && source.is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * Math.max(0.2f, 1f - 0.2f * forge));
            int stone = Engraving.armorCount(p, "stone");
            if (stone > 0 && source.is(DamageTypeTags.IS_FALL)) event.setAmount(event.getAmount() * Math.max(0.2f, 1f - 0.2f * stone));
        }

        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer p && !(target instanceof Player)) {
            target.getPersistentData().putString(Nemesis.LAST_CAT, cat);
            if (source.getDirectEntity() == p && cat.equals("melee")) {
                if (Traits.has(p.getMainHandItem(), Traits.SHARP) > 0) event.setAmount(event.getAmount() * 1.15f);
                Engraving.onWeaponHit(p, target, event.getAmount());
            }
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Traumas.onDamaged(p, event.getSource(), event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        DamageSource source = event.getSource();

        if (victim instanceof ServerPlayer p) {
            if (source.getEntity() instanceof Mob killer && killer.isAlive()) Nemesis.onPlayerKilled(p, killer);
            return;
        }

        if (victim instanceof WanderingTrader) Caravans.onTraderDeath(level, victim.blockPosition());

        if (source.getEntity() instanceof ServerPlayer p) {
            PlayerData d = Data.get(p);
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
            Insights.add(p, "creature", type);
            if (victim instanceof Monster) {
                d.killTypes.merge(type, 1, Integer::sum);
                d.killCats.merge(category(source), 1, Integer::sum);
                d.killsSinceHunter++;
                Pulse.addNoise(p, 1f);
            }
            Mastery.add(p, School.ASH, 1);
            if (p.getMainHandItem().getItem() instanceof StaffItem && victim instanceof Monster && level.random.nextFloat() < 0.25f) {
                victim.spawnAtLocation(new ItemStack(ModItems.SOUL_ESSENCE.get()));
            }
            if (victim.getTags().contains(Hunters.TAG)) {
                victim.spawnAtLocation(new ItemStack(ModItems.HUNTER_TROPHY.get()));
                Msg.chat(p, ChatFormatting.GOLD, "pulse.aetherwastes.hunter_slain");
            }
            if (victim instanceof Mob mob) Nemesis.onKilledBy(p, mob);
        }

        // Много смертей в одном месте рождают Порчу.
        if (level.dimension() == Level.OVERWORLD && !(victim instanceof Player)) {
            WorldState ws = WorldState.get(level.getServer());
            long day = level.getDayTime() / 24000L;
            if (ws.killsDay != day) {
                ws.killsDay = day;
                ws.chunkKills.clear();
            }
            long chunk = ChunkPos.asLong(victim.blockPosition());
            int n = ws.chunkKills.merge(chunk, 1, Integer::sum);
            ws.setDirty();
            if (n >= 25) {
                ws.chunkKills.put(chunk, 0);
                Scar.seedNear(level, victim.blockPosition(), 4);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (Traits.has(event.getEntity().getMainHandItem(), Traits.LIGHT) > 0) {
            event.setNewSpeed(event.getNewSpeed() * 1.2f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p)) return;
        BlockState st = event.getState();
        BlockPos pos = event.getPos();
        if (pos.getY() < 0) {
            Pulse.addNoise(p, 0.3f);
            if (p.getRandom().nextInt(4) == 0) Mastery.add(p, School.STONE, 1);
        }
        if (st.getBlock() instanceof CropBlock crop && crop.isMaxAge(st)) Mastery.add(p, School.ROOT, 2);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!AetherConfig.DISABLE_ENCHANTING_TABLE.get()) return;
        if (event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide) {
                Msg.bar(event.getEntity(), ChatFormatting.GRAY, "message.aetherwastes.enchanting_disabled");
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof ServerPlayer p) Pulse.addNoise(p, 15f);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Monster mob)) return;
        for (String t : mob.getTags()) if (t.startsWith("aw_")) return;
        BlockPos pos = mob.blockPosition();

        if (Wards.isWarded(level, pos)) {
            event.setCanceled(true);
            return;
        }
        if (WorldState.ENDING_HEAL.equals(WorldState.get(level.getServer()).ending) && level.random.nextFloat() < 0.5f) {
            event.setCanceled(true);
            return;
        }
        Player near = level.getNearestPlayer(mob, 96);
        int era = near != null ? Data.get(near).era + Data.get(near).secondAge : 1;
        Pulse.scaleNewMob(level, mob, EtherField.ring(level, pos), era);
        boolean onScar = level.getBlockState(pos.below()).is(ModBlocks.AETHER_SCAR.get());
        if (onScar || (EtherField.pressure(level, pos) >= 85f && level.random.nextFloat() < 0.4f)) {
            Pulse.distort(mob);
        }
    }
}
