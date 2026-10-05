package com.aetherwastes.item;

import com.aetherwastes.core.Msg;
import com.aetherwastes.registry.ModStructures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

/** Карта Картографа Пустошей: превращается в карту с отметкой ближайшего подземелья. */
public class DungeonMapItem extends Item {
    public DungeonMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        BlockPos best = null;
        ResourceKey<Structure> bestKey = null;
        var registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (ResourceKey<Structure> key : List.of(ModStructures.LOST_ARCHIVE, ModStructures.ECHO_CRYPT, ModStructures.ASH_CITADEL)) {
            var holder = registry.getHolder(key);
            if (holder.isEmpty()) continue;
            var found = server.getChunkSource().getGenerator().findNearestMapStructure(server,
                    HolderSet.direct((Holder<Structure>) holder.get()), player.blockPosition(), 50, false);
            if (found == null) continue;
            BlockPos pos = found.getFirst();
            if (best == null || pos.distSqr(player.blockPosition()) < best.distSqr(player.blockPosition())) {
                best = pos;
                bestKey = key;
            }
        }
        if (best == null) {
            Msg.bar(player, ChatFormatting.GRAY, "item.aetherwastes.dungeon_map.none");
            return InteractionResultHolder.fail(stack);
        }
        ItemStack map = MapItem.create(server, best.getX(), best.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(server, map);
        MapItemSavedData.addTargetDecoration(map, best, "+", MapDecorationTypes.RED_X);
        String name = bestKey.location().getPath();
        map.set(net.minecraft.core.component.DataComponents.ITEM_NAME,
                Component.translatable("item.aetherwastes.dungeon_map.of", Component.translatable("structure.aetherwastes." + name)));
        level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1f, 1f);
        int dist = (int) Math.sqrt(best.distSqr(player.blockPosition()));
        Msg.bar(player, ChatFormatting.GOLD, "item.aetherwastes.dungeon_map.found",
                Component.translatable("structure.aetherwastes." + name), dist);
        stack.consume(1, player);
        if (!player.getInventory().add(map)) player.drop(map, false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aetherwastes.dungeon_map.desc").withStyle(ChatFormatting.GRAY));
    }
}
