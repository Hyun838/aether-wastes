package com.aetherwastes.block;

import com.aetherwastes.registry.ModParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Руна-ловушка подземелья. Срабатывает на игрока и перезаряжается через 3 секунды.
 * Архив — взрыв Эфира и левитация; Склеп — цепи душ (замедление, иссушение); Цитадель — огненный гейзер.
 */
public class DungeonTrapBlock extends Block {
    public static final MapCodec<DungeonTrapBlock> CODEC = simpleCodec(DungeonTrapBlock::new);
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    public DungeonTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, 0).setValue(ARMED, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(KIND, ARMED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server) || !state.getValue(ARMED)) return;
        if (!(entity instanceof Player p) || p.isCreative() || p.isSpectator()) return;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.2, cz = pos.getZ() + 0.5;
        switch (state.getValue(KIND)) {
            case 0 -> {
                p.hurt(level.damageSources().magic(), 6f);
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
                server.sendParticles(ModParticles.RUNE.get(), cx, cy + 0.5, cz, 20, 0.6, 0.6, 0.6, 0.05);
                server.sendParticles(ModParticles.ETHER_SPARK.get(), cx, cy, cz, 40, 0.4, 0.2, 0.4, 0.2);
                server.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1f, 1.3f);
            }
            case 1 -> {
                p.hurt(level.damageSources().magic(), 4f);
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                server.sendParticles(ParticleTypes.SOUL, cx, cy + 0.5, cz, 30, 0.5, 0.8, 0.5, 0.03);
                server.sendParticles(ModParticles.PHANTOM_WISP.get(), cx, cy, cz, 20, 0.4, 0.4, 0.4, 0.03);
                server.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.2f, 0.6f);
                server.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.2f, 0.8f);
            }
            default -> {
                p.hurt(level.damageSources().inFire(), 6f);
                p.igniteForSeconds(5f);
                p.push(0, 0.7, 0);
                p.hurtMarked = true;
                server.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 50, 0.3, 1.2, 0.3, 0.05);
                server.sendParticles(ModParticles.ASH_EMBER.get(), cx, cy, cz, 30, 0.4, 1.0, 0.4, 0.08);
                server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 0.7f);
            }
        }
        level.setBlock(pos, state.setValue(ARMED, false), 3);
        level.scheduleTick(pos, this, 60);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ARMED)) level.setBlock(pos, state.setValue(ARMED, true), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ARMED) && random.nextInt(10) == 0) {
            var type = switch (state.getValue(KIND)) {
                case 0 -> ModParticles.ETHER_SPARK.get();
                case 1 -> ModParticles.PHANTOM_WISP.get();
                default -> ModParticles.ASH_EMBER.get();
            };
            level.addParticle(type, pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
