package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.entity.Glassman;
import com.aetherwastes.entity.ResinWalker;
import com.aetherwastes.entity.SaltWraith;
import com.aetherwastes.entity.Wanderer;
import com.aetherwastes.progression.EraManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = AetherWastes.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AetherWastes.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SaltWraith>> SALT_WRAITH = ENTITIES.register("salt_wraith",
            () -> EntityType.Builder.<SaltWraith>of(SaltWraith::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("salt_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<ResinWalker>> RESIN_WALKER = ENTITIES.register("resin_walker",
            () -> EntityType.Builder.<ResinWalker>of(ResinWalker::new, MobCategory.MONSTER).sized(0.7f, 2.1f).clientTrackingRange(8).build("resin_walker"));
    public static final DeferredHolder<EntityType<?>, EntityType<Glassman>> GLASSMAN = ENTITIES.register("glassman",
            () -> EntityType.Builder.<Glassman>of(Glassman::new, MobCategory.MONSTER).sized(0.6f, 1.99f).clientTrackingRange(8).build("glassman"));

    public static final DeferredHolder<EntityType<?>, EntityType<Wanderer>> WANDERER_SPARK = wanderer(EraManager.SPARK);
    public static final DeferredHolder<EntityType<?>, EntityType<Wanderer>> WANDERER_RESONANCE = wanderer(EraManager.RESONANCE);
    public static final DeferredHolder<EntityType<?>, EntityType<Wanderer>> WANDERER_CONVERGENCE = wanderer(EraManager.CONVERGENCE);
    public static final DeferredHolder<EntityType<?>, EntityType<Wanderer>> WANDERER_HEART = wanderer(EraManager.HEART);

    private static DeferredHolder<EntityType<?>, EntityType<Wanderer>> wanderer(String variant) {
        String name = "wanderer_" + variant;
        return ENTITIES.register(name, () -> EntityType.Builder.<Wanderer>of((type, level) -> new Wanderer(type, level, variant), MobCategory.MONSTER)
                .sized(0.8f, 2.5f).fireImmune().clientTrackingRange(10).build(name));
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(SALT_WRAITH.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0).build());
        event.put(RESIN_WALKER.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0).build());
        event.put(GLASSMAN.get(), AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.ARMOR, 6).build());
        double[] hp = {200, 300, 450, 700};
        var types = new DeferredHolder[]{WANDERER_SPARK, WANDERER_RESONANCE, WANDERER_CONVERGENCE, WANDERER_HEART};
        for (int i = 0; i < 4; i++) {
            @SuppressWarnings("unchecked")
            EntityType<Wanderer> t = (EntityType<Wanderer>) types[i].get();
            event.put(t, Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, hp[i])
                    .add(Attributes.ATTACK_DAMAGE, 7 + 2 * i)
                    .add(Attributes.ARMOR, 6 + 2 * i)
                    .add(Attributes.MOVEMENT_SPEED, 0.27)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                    .add(Attributes.FOLLOW_RANGE, 48)
                    .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0)
                    .build());
        }
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SALT_WRAITH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(RESIN_WALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GLASSMAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {}
}
