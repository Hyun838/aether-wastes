package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.world.Migrations;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** Простые устройства базы: Рунный столб (оберег) и Обсерватория. */
public class DeviceBlock extends Block {
    private final String type;

    public DeviceBlock(String type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this)) Devices.placed(level, pos, type);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this)) Devices.removed(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        ServerPlayer p = (ServerPlayer) player;
        if (!Devices.powered(server, pos)) {
            Msg.bar(p, ChatFormatting.RED, EtherField.inAnchor(server, pos) ? "device.aetherwastes.overloaded" : "device.aetherwastes.no_anchor");
            return InteractionResult.SUCCESS;
        }
        if (Devices.OBSERVATORY.equals(type)) forecast(server, pos, p);
        else Msg.bar(p, ChatFormatting.AQUA, "device.aetherwastes.ward_ok");
        return InteractionResult.SUCCESS;
    }

    private static void forecast(ServerLevel level, BlockPos pos, ServerPlayer p) {
        if (!level.canSeeSky(pos.above())) {
            Msg.bar(p, ChatFormatting.GRAY, "device.aetherwastes.observatory_roof");
            return;
        }
        PlayerData d = Data.get(p);
        long day = level.getDayTime() / 24000L;
        Msg.chat(p, ChatFormatting.AQUA, "observatory.aetherwastes.header");
        long tide = EtherField.daysToTide(level);
        Msg.chat(p, ChatFormatting.GRAY, tide == 0 ? "observatory.aetherwastes.tide_now" : "observatory.aetherwastes.tide_in", tide);
        long mig = com.aetherwastes.core.WorldState.get(level.getServer()).nextMigrationDay - day;
        Msg.chat(p, ChatFormatting.GRAY, "observatory.aetherwastes.migration", Math.max(0, mig),
                Component.translatable("season.aetherwastes." + Migrations.season(day)));
        Msg.chat(p, ChatFormatting.GRAY, "observatory.aetherwastes.moon", level.getMoonPhase() == 0
                ? Component.translatable("observatory.aetherwastes.full_moon") : Component.literal(String.valueOf(level.getMoonPhase())));
        long sinceWanderer = level.getGameTime() - d.lastWanderer;
        Msg.chat(p, ChatFormatting.GRAY, sinceWanderer >= 48000L ? "observatory.aetherwastes.wanderer_near" : "observatory.aetherwastes.wanderer_far");
        Msg.chat(p, ChatFormatting.GRAY, "observatory.aetherwastes.noise", Math.round(d.noise), Math.round(d.siegeMeter));
        if (d.nemesis.contains("name")) {
            Msg.chat(p, ChatFormatting.DARK_RED, "observatory.aetherwastes.nemesis", d.nemesis.getString("name"));
        }
        level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1f, 1f);
        com.aetherwastes.progression.Mastery.add(p, com.aetherwastes.progression.School.STAR, 2);
    }

    /** Чинит повреждённый бурей столб Осколком Эфира. */
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.hasProperty(WardPylonBlock.DAMAGED) && state.getValue(WardPylonBlock.DAMAGED) && stack.is(ModItems.ETHER_SHARD.get())) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(WardPylonBlock.DAMAGED, false));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1f);
                player.displayClientMessage(Component.translatable("device.aetherwastes.repaired").withStyle(ChatFormatting.GREEN), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }
}
