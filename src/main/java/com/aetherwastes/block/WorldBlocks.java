package com.aetherwastes.block;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.world.HeartManager;
import com.aetherwastes.world.Rifts;
import com.aetherwastes.world.Underside;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Блоки мира Пустошей. */
public final class WorldBlocks {
    private WorldBlocks() {}

    /** Живая стена Корневища: сама растёт вверх до 3 блоков, если рядом есть вода. */
    public static class LivingWall extends Block {
        public LivingWall(Properties p) {
            super(p);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean water = false;
            for (BlockPos q : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 1, 4))) {
                if (level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)) {
                    water = true;
                    break;
                }
            }
            if (!water) {
                if (random.nextFloat() < 0.05f) level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DEAD_BUSH.defaultBlockState());
                return;
            }
            int height = 1;
            while (height < 3 && level.getBlockState(pos.below(height)).is(this)) height++;
            if (height < 3 && level.getBlockState(pos.above()).isAir()) level.setBlockAndUpdate(pos.above(), defaultBlockState());
        }
    }

    /** Кровоточащее дерево: ПКМ (пустой рукой или пузырьком) — собрать смолу; ночью «шевелится». */
    public static class BleedingLog extends RotatedPillarBlock {
        public static final BooleanProperty TAPPED = BooleanProperty.create("tapped");

        public BleedingLog(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(TAPPED, false));
        }

        @Override
        public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(TAPPED);
        }

        @Override
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return tap(state, level, pos);
        }

        @Override
        public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit) {
            if (stack.is(Items.GLASS_BOTTLE)) {
                tap(state, level, pos);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        private InteractionResult tap(BlockState state, Level level, BlockPos pos) {
            if (state.getValue(TAPPED)) return InteractionResult.PASS;
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(TAPPED, true));
                Block.popResource(level, pos, new ItemStack(ModItems.RESIN.get(), 1 + level.random.nextInt(2)));
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1f, 0.7f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public boolean isRandomlyTicking(BlockState state) {
            return state.getValue(TAPPED);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextFloat() < 0.15f) level.setBlockAndUpdate(pos, state.setValue(TAPPED, false));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (!state.getValue(TAPPED) && random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.DRIPPING_HONEY, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0, 0, 0);
            }
        }
    }

    /** Застывшая молния: касание высвобождает разряд. */
    public static class ChargedCrystal extends Block {
        public ChargedCrystal(Properties p) {
            super(p);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (level instanceof ServerLevel server && entity instanceof LivingEntity living && level.random.nextFloat() < 0.02f) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                living.hurt(level.damageSources().lightningBolt(), 3f);
            }
            super.stepOn(level, pos, state, entity);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                        pos.getZ() + random.nextDouble(), 0, 0.05, 0);
            }
        }
    }

    /** Разлом (вход и выход). */
    public static class RiftPortal extends Block {
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

        public RiftPortal(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(RiftPortalBlock.EXIT, false));
        }

        @Override
        public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RiftPortalBlock.EXIT);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof ServerPlayer p) Rifts.enter(p, pos, state.getValue(RiftPortalBlock.EXIT));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 2,
                        pos.getZ() + random.nextDouble(), 0, 0.05, 0);
            }
        }
    }

    /** Портальный камень Изнанки: ПКМ — перейти (нужна Эпоха III). */
    public static class UndersidePortal extends Block {
        public UndersidePortal(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer p) Underside.travel(p);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.1,
                    pos.getZ() + random.nextDouble(), 0, 0.3, 0);
        }
    }

    /** Сердце Раскола. */
    public static class HeartOfRift extends Block {
        public HeartOfRift(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            if (WorldState.get(p.getServer()).ended()) {
                Msg.chat(p, ChatFormatting.GOLD, "heart.aetherwastes.already");
            } else if (Data.get(p).wandererKills.contains(EraManager.HEART)) {
                HeartManager.offerEndings(p);
            } else {
                Msg.chat(p, ChatFormatting.DARK_PURPLE, "heart.aetherwastes.guarded");
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    public static boolean faceUp(Direction d) {
        return d == Direction.UP;
    }
}
