package com.aetherwastes.threats;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.craft.Engraving;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.world.Rifts;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Пульс Мира — «режиссёр» угроз. Следит за Шумом и привычками игрока:
 * волны мобов на шум, Осады Якоря, Охотники против любимой тактики, Немезида,
 * Эфирные бури в Прилив, Скитальцы и Разломы.
 */
public final class Pulse {
    private Pulse() {}

    public static void addNoise(ServerPlayer p, float amount) {
        PlayerData d = Data.get(p);
        if (amount > 0) {
            amount *= Math.max(0.25f, 1f - 0.15f * Engraving.armorCount(p, "silence"));
        }
        d.noise = Math.max(0f, Math.min(100f, d.noise + amount));
    }

    public static void tickPlayer(ServerPlayer p, PlayerData d, float pressure, boolean tide) {
        float floor = 0f;
        String ending = WorldState.get(p.getServer()).ending;
        if (WorldState.ENDING_ABSORB.equals(ending)) floor = 40f;
        d.noise = Math.max(floor, d.noise - 0.4f);
        if (!AetherConfig.PULSE.get() || p.isCreative()) return;

        ServerLevel level = p.serverLevel();
        boolean overworld = level.dimension() == Level.OVERWORLD;

        if (overworld && p.tickCount % 600 == 0) noiseWave(p, d);
        Sieges.tick(p, d);
        if (p.tickCount % 400 == 0) Hunters.tick(p, d);
        if (p.tickCount % 2400 == 0) Nemesis.tickReturn(p, d);
        if (tide && overworld && p.tickCount % 400 == 0) storm(p, pressure);
        Wanderers.tick(p, d);
        Rifts.tick(p, d, tide);
    }

    /** Шум притягивает монстров из темноты. */
    private static void noiseWave(ServerPlayer p, PlayerData d) {
        if (d.noise < 50f) return;
        ServerLevel level = p.serverLevel();
        boolean dark = !level.isDay() || (!level.canSeeSky(p.blockPosition()) && p.getY() < 50);
        if (!dark || p.getRandom().nextFloat() > d.noise / 200f) return;
        int ring = EtherField.ring(level, p.blockPosition());
        int count = 1 + p.getRandom().nextInt(2 + ring / 2);
        EntityType<? extends Mob>[] pool = monsterPool(ring);
        for (int i = 0; i < count; i++) {
            spawnHostile(level, p, pool[p.getRandom().nextInt(pool.length)], 16 + p.getRandom().nextInt(8), "aw_spawned");
        }
        Msg.bar(p, ChatFormatting.DARK_RED, "pulse.aetherwastes.noise_wave");
    }

    @SuppressWarnings("unchecked")
    public static EntityType<? extends Mob>[] monsterPool(int ring) {
        return switch (ring) {
            case 1 -> new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER};
            case 2 -> new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
                    com.aetherwastes.registry.ModEntities.SALT_WRAITH.get()};
            case 3 -> new EntityType[]{EntityType.SKELETON, EntityType.CREEPER, EntityType.WITCH,
                    com.aetherwastes.registry.ModEntities.RESIN_WALKER.get(), com.aetherwastes.registry.ModEntities.GLASSMAN.get()};
            default -> new EntityType[]{EntityType.VINDICATOR, EntityType.WITCH, EntityType.CREEPER,
                    com.aetherwastes.registry.ModEntities.GLASSMAN.get(), com.aetherwastes.registry.ModEntities.RESIN_WALKER.get()};
        };
    }

    public static Mob spawnHostile(ServerLevel level, ServerPlayer target, EntityType<? extends Mob> type, int distance, String tag) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            int x = (int) (target.getX() + Math.cos(a) * distance);
            int z = (int) (target.getZ() + Math.sin(a) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (!level.canSeeSky(target.blockPosition()) && target.getY() < 50) {
                y = target.getBlockY();
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            Mob mob = type.create(level);
            if (mob == null) return null;
            mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            mob.addTag(tag);
            mob.setTarget(target);
            level.addFreshEntity(mob);
            return mob;
        }
        return null;
    }

    /** Эфирная буря: молнии, мутационные зоны, Порча; громоотвод собирает бурю в Заряженные осколки. */
    private static void storm(ServerPlayer p, float pressure) {
        if (!AetherConfig.ETHER_STORMS.get() || pressure < 60f || p.getRandom().nextFloat() > 0.3f) return;
        ServerLevel level = p.serverLevel();
        if (!level.canSeeSky(p.blockPosition().above())) return;
        double a = level.random.nextDouble() * Math.PI * 2;
        int dist = 12 + level.random.nextInt(14);
        int x = (int) (p.getX() + Math.cos(a) * dist);
        int z = (int) (p.getZ() + Math.sin(a) * dist);
        BlockPos strike = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);

        BlockPos rod = null;
        for (BlockPos pos : BlockPos.betweenClosed(strike.offset(-16, -8, -16), strike.offset(16, 8, 16))) {
            if (level.getBlockState(pos).is(Blocks.LIGHTNING_ROD)) {
                rod = pos.immutable();
                break;
            }
        }
        if (rod != null) strike = rod.above();

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(Vec3.atBottomCenterOf(strike));
        level.addFreshEntity(bolt);
        com.aetherwastes.base.Wards.onLightning(level, strike);

        if (rod != null) {
            ItemEntity drop = new ItemEntity(level, rod.getX() + 0.5, rod.getY() + 1.2, rod.getZ() + 0.5,
                    new ItemStack(ModItems.CHARGED_SHARD.get(), 1 + level.random.nextInt(2)));
            level.addFreshEntity(drop);
            return;
        }
        if (level.random.nextFloat() < 0.3f) Scar.seedNear(level, strike, 2);
        // Вещи на земле рядом с ударом меняют свойства.
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(strike).inflate(4));
        for (ItemEntity ie : items) {
            ItemStack s = ie.getItem();
            if (s.isDamageableItem()) Traits.add(s, Traits.random(level.random));
        }
        // Зона мутаций: краткая волна давления.
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(strike).inflate(5))) {
            Data.get(near).exposure += 60f;
            near.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
    }

    /** Мобы в кольцах дальше от спавна и в поздних эпохах сильнее; в давлении от 85 — Искажённые. */
    public static void scaleNewMob(ServerLevel level, Mob mob, int ring, int era) {
        float bonus = 0.15f * (ring - 1) + 0.1f * (era - 1);
        if (bonus > 0f) {
            AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null && !hp.hasModifier(com.aetherwastes.AetherWastes.id("ring_scaling"))) {
                hp.addPermanentModifier(new AttributeModifier(com.aetherwastes.AetherWastes.id("ring_scaling"), bonus,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                mob.setHealth(mob.getMaxHealth());
            }
            AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null && !dmg.hasModifier(com.aetherwastes.AetherWastes.id("ring_damage"))) {
                dmg.addPermanentModifier(new AttributeModifier(com.aetherwastes.AetherWastes.id("ring_damage"), bonus * 0.5,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    public static void distort(Mob mob) {
        int inf = MobEffectInstance.INFINITE_DURATION;
        int n = 1 + mob.getRandom().nextInt(2);
        for (int i = 0; i < n; i++) {
            switch (mob.getRandom().nextInt(4)) {
                case 0 -> mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, inf, 1));
                case 1 -> mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, inf, 1));
                case 2 -> mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, inf, 0));
                default -> mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0));
            }
        }
        mob.setCustomName(Component.translatable("entity.aetherwastes.distorted", mob.getType().getDescription())
                .withStyle(ChatFormatting.DARK_PURPLE));
        mob.addTag("aw_distorted");
    }

    public static String dominant(Map<String, Integer> map, int total, float share) {
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (total > 0 && e.getValue() >= total * share) return e.getKey();
        }
        return null;
    }

    public static void sound(ServerPlayer p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.5f, 0.6f);
    }
}
