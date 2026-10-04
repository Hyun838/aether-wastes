package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.craft.Quality;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.progression.Mastery;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.threats.Pulse;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Горн. Уголь — топливо; предмет снаряжения — заготовка; ПКМ пустой рукой — раздуть мехи;
 * Shift + ПКМ пустой — достать (качество по жару: 65–75 — Мастерское). Материал для починки
 * в руке при заготовке внутри и жаре от 40 — ремонт на 25%.
 */
public class ForgeHearthBlock extends BaseEntityBlock {
    public static final MapCodec<ForgeHearthBlock> CODEC = simpleCodec(ForgeHearthBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ForgeHearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeHearthBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FORGE_HEARTH.get(), ForgeHearthBlockEntity::serverTick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this)) Devices.placed(level, pos, Devices.FORGE);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ForgeHearthBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerPlayer p = (ServerPlayer) player;

        int burn = stack.is(Items.COAL_BLOCK) ? 16000 : (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) ? 1600 : 0;
        if (burn > 0) {
            be.fuel += burn;
            if (!p.getAbilities().instabuild) stack.shrink(1);
            be.setChanged();
            level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
            showHeat(p, be);
            return ItemInteractionResult.SUCCESS;
        }
        if (!be.work.isEmpty() && be.work.isDamageableItem() && be.work.getDamageValue() > 0
                && be.work.getItem().isValidRepairItem(be.work, stack)) {
            if (be.heat < 40f) {
                Msg.bar(p, ChatFormatting.RED, "forge.aetherwastes.too_cold");
                return ItemInteractionResult.SUCCESS;
            }
            ItemStack w = be.work;
            w.setDamageValue(Math.max(0, w.getDamageValue() - w.getMaxDamage() / 4));
            boolean tide = EtherField.isTide(level);
            if (Quality.get(w) != Quality.MASTER && !tide) {
                // Каждый ремонт обычным материалом снижает максимальную прочность.
                Traits.scaleDurability(w, 0.95f);
            }
            if (!p.getAbilities().instabuild) stack.shrink(1);
            be.setChanged();
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            Msg.bar(p, ChatFormatting.GREEN, "forge.aetherwastes.repaired", w.getMaxDamage() - w.getDamageValue(), w.getMaxDamage());
            return ItemInteractionResult.SUCCESS;
        }
        if (be.work.isEmpty() && stack.isDamageableItem()) {
            be.work = stack.copyWithCount(1);
            stack.shrink(1);
            be.setChanged();
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
            Msg.bar(p, ChatFormatting.GOLD, "forge.aetherwastes.inserted");
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ForgeHearthBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer p = (ServerPlayer) player;
        if (player.isShiftKeyDown()) {
            if (be.work.isEmpty()) {
                showHeat(p, be);
                return InteractionResult.SUCCESS;
            }
            ItemStack w = be.work;
            be.work = ItemStack.EMPTY;
            if (be.heat >= 30f && Quality.get(w) < 0) {
                boolean skilled = Data.get(p).knows(School.FORGE.id());
                int q = Quality.fromHeat(be.heat, skilled);
                Quality.set(w, q);
                if (EtherField.isTide(level)) {
                    String t = Traits.random(level.random);
                    Traits.add(w, t);
                    Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "forge.aetherwastes.tide_trait", Component.translatable("trait.aetherwastes." + t));
                }
                Mastery.add(p, School.FORGE, 5 + q * 5);
                Msg.chat(p, Quality.color(q), "forge.aetherwastes.forged", w.getHoverName(),
                        Component.translatable("quality.aetherwastes." + q));
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.9f);
            } else if (Quality.get(w) < 0) {
                Msg.bar(p, ChatFormatting.GRAY, "forge.aetherwastes.too_cold_forge");
            }
            if (w.has(DataComponents.MAX_DAMAGE) && w.getDamageValue() >= w.getMaxDamage()) w.setDamageValue(w.getMaxDamage() - 1);
            if (!p.addItem(w)) p.drop(w, false);
            be.setChanged();
            return InteractionResult.SUCCESS;
        }
        be.pump();
        Pulse.addNoise(p, 1f);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 0.6f);
        ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, pos.getX() + 0.5,
                pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.02);
        showHeat(p, be);
        return InteractionResult.SUCCESS;
    }

    private static void showHeat(ServerPlayer p, ForgeHearthBlockEntity be) {
        boolean skilled = Data.get(p).knows(School.FORGE.id());
        String band = be.heat < 30 ? "dark" : be.heat < 55 ? "cherry" : be.heat < 65 ? "orange" : be.heat <= 75 ? "yellow" : be.heat < 90 ? "white" : "sparks";
        Component heatText = skilled
                ? Component.translatable("forge.aetherwastes.heat_exact", Math.round(be.heat), Component.translatable("forge.aetherwastes.band." + band))
                : Component.translatable("forge.aetherwastes.heat_band", Component.translatable("forge.aetherwastes.band." + band));
        p.displayClientMessage(heatText.copy().withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("forge.aetherwastes.fuel", be.fuel / 20).withStyle(ChatFormatting.GRAY)), true);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ForgeHearthBlockEntity be && !be.work.isEmpty()) {
                Block.popResource(level, pos, be.work);
            }
            Devices.removed(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
