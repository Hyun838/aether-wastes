package com.aetherwastes.item;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.Msg;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.progression.Insights;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

/**
 * Эфирный компас: в руке показывает давление, жилы, Якорь, Прилив и кольцо мира.
 * Shift + ПКМ по необычному блоку (руды, кристаллы, блоки Пустошей) — изучить его ради Озарения.
 */
public class EtherCompassItem extends Item {
    public EtherCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        if (player.tickCount % 10 != 0) return;
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;

        float p = EtherField.pressure(server, player.blockPosition());
        ChatFormatting color = p >= EtherField.HIGH ? ChatFormatting.RED
                : p <= EtherField.LOW ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE;
        String band = p >= EtherField.HIGH ? "high" : p <= EtherField.LOW ? "low" : "normal";

        MutableComponent msg = Component.translatable("message.aetherwastes.compass.pressure",
                Math.round(p), Component.translatable("message.aetherwastes.compass." + band)).withStyle(color);
        if (level.dimension() == Level.OVERWORLD) {
            msg.append(Component.literal(" · ")).append(Component.translatable("message.aetherwastes.compass.ring",
                    EtherField.ring(server, player.blockPosition())).withStyle(ChatFormatting.GRAY));
        }
        float ley = EtherField.leyStrength(server.getSeed(), player.getBlockX(), player.getBlockZ());
        if (ley > 0) {
            msg.append(Component.literal(" · ")).append(Component.translatable(
                    ley > 1f ? "message.aetherwastes.compass.ley_cross" : "message.aetherwastes.compass.ley")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (EtherField.inAnchor(server, player.blockPosition())) {
            msg.append(Component.literal(" · ")).append(Component.translatable("message.aetherwastes.compass.anchor")
                    .withStyle(ChatFormatting.GREEN));
        }
        if (EtherField.isTide(server)) {
            msg.append(Component.literal(" · ")).append(Component.translatable("message.aetherwastes.compass.tide")
                    .withStyle(ChatFormatting.GOLD));
        }
        player.displayClientMessage(msg, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        BlockState st = ctx.getLevel().getBlockState(ctx.getClickedPos());
        var key = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        boolean ore = st.is(Tags.Blocks.ORES);
        boolean special = key.getNamespace().equals(AetherWastes.MODID) || st.is(Blocks.AMETHYST_CLUSTER)
                || st.is(Blocks.BUDDING_AMETHYST) || st.is(Blocks.CRYING_OBSIDIAN) || st.is(Blocks.SCULK_SHRIEKER)
                || st.is(Blocks.SCULK_CATALYST) || st.is(Blocks.ANCIENT_DEBRIS) || st.is(Blocks.SPAWNER)
                || st.is(Blocks.REINFORCED_DEEPSLATE) || st.is(Blocks.END_PORTAL_FRAME);
        if (!ore && !special) {
            Msg.bar(player, ChatFormatting.GRAY, "message.aetherwastes.compass.nothing");
            return InteractionResult.SUCCESS;
        }
        if (!Insights.add((ServerPlayer) player, ore ? "metal" : "ether", key.toString())) {
            Msg.bar(player, ChatFormatting.GRAY, "message.aetherwastes.compass.already");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aetherwastes.ether_compass").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aetherwastes.ether_compass.study").withStyle(ChatFormatting.DARK_AQUA));
    }
}
